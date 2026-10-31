package com.acme.supportpilot.rag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Builds the modular RAG pipeline for one request:
 * <pre>
 *   pre-retrieval     rewrite the question (B)  ->  expand it into variants (B)
 *   retrieval         vector search, or hybrid vector + keyword search with RRF (D), filtered (C)
 *   join              merge the results of the variants (B: concatenate, D: RRF)
 *   post-retrieval    re-rank (E)  ->  keep the top N
 *   generation        numbered sources + the question in one prompt
 * </pre>
 * Every stage is optional and switched by {@link RagOptions}, so you can measure what each adds.
 */
@Component
public class RagPipelineFactory {

    /** Run retrieval before any other advisor (e.g. logging) sees the prompt. */
    static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

    private final VectorStore vectorStore;

    private final RagSettings settings;

    private final QueryAugmenter augmenter;

    private final RagHelperModel helper;

    private final QueryTransformer rewriter;

    private final DocumentRetriever keywordRetriever;

    /**
     * The advisor is built per request, so it must not create its own thread pool each time:
     * all pipelines share one executor (virtual threads, Java 21).
     */
    private final SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("rag-");

    public RagPipelineFactory(VectorStore vectorStore, RagSettings settings, RagPrompts prompts,
                              RagHelperModel helper,
                              @Qualifier("keywordRetriever") DocumentRetriever keywordRetriever) {
        this.vectorStore = vectorStore;
        this.settings = settings;
        this.augmenter = ContextualQueryAugmenter.builder()
                .promptTemplate(prompts.answer())
                .emptyContextPromptTemplate(prompts.emptyContext())
                .allowEmptyContext(false)                 // no sources -> say "I don't know"
                .documentFormatter(RagPrompts::numbered)  // [1] source, page ... so answers can cite
                .build();
        this.helper = helper;
        this.rewriter = RewriteQueryTransformer.builder()
                .chatClientBuilder(helper.builder())
                .targetSearchSystem("knowledge base of Acme Cloud product documentation")
                .build();
        this.keywordRetriever = keywordRetriever;
        this.executor.setVirtualThreads(true);
    }

    /** The RAG advisor for one request, assembled from the switches in {@code options}. */
    public RetrievalAugmentationAdvisor advisor(RagOptions options) {
        // Fetch more candidates when a later stage (fusion, re-ranking) will narrow them down.
        int depth = (options.hybrid() || options.rerank()) ? this.settings.candidates() : options.topN();
        RetrievalAugmentationAdvisor.Builder builder = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever(options, depth))
                .queryAugmenter(this.augmenter)
                .taskExecutor(this.executor)
                .order(ORDER);

        // Pre-retrieval (Checkpoint B)
        if (options.rewrite()) {
            builder.queryTransformers(Traced.transformer("rewrite", this.rewriter));
        }
        if (options.expansions() > 0) {
            builder.queryExpander(Traced.expander(MultiQueryExpander.builder()
                    .chatClientBuilder(this.helper.builder())
                    .numberOfQueries(options.expansions())
                    .includeOriginal(true)
                    .build()));
        }
        // Join the variants' results by rank, not by raw score (Checkpoint D)
        builder.documentJoiner(new RrfDocumentJoiner(this.settings.rrfK()));

        // Post-retrieval
        List<DocumentPostProcessor> post = new ArrayList<>();
        post.add(Traced.postProcessor("top-n", (query, docs) -> docs.stream().limit(options.topN()).toList()));
        return builder.documentPostProcessors(post).build();
    }

    private DocumentRetriever retriever(RagOptions options, int depth) {
        DocumentRetriever vector = VectorStoreDocumentRetriever.builder()
                .vectorStore(this.vectorStore)
                .topK(depth)
                .similarityThreshold(this.settings.similarityThreshold())
                .build();
        if (options.hybrid()) {
            return Traced.retriever("hybrid", new HybridDocumentRetriever(Traced.retriever("vector", vector),
                    Traced.retriever("keyword", this.keywordRetriever), this.settings.rrfK(), depth));
        }
        return Traced.retriever("vector", vector);
    }

    /**
     * Runs only the retrieval half of the pipeline - no answer is generated. Used by /retrieve
     * and the comparison runner: it shows exactly which chunks would reach the prompt.
     */
    public List<Document> retrieve(String question, RagOptions options, Map<String, Object> context) {
        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage(question)))
                .context(new HashMap<>(context))
                .build();
        ChatClientRequest augmented = advisor(options).before(request, null);
        return documents(augmented.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT));
    }

    @SuppressWarnings("unchecked")
    static List<Document> documents(Object value) {
        return (value instanceof List<?> list) ? (List<Document>) list : List.of();
    }

}
