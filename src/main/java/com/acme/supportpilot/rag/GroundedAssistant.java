package com.acme.supportpilot.rag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.acme.supportpilot.knowledge.SearchHit;

/**
 * Session 4: answers grounded in the knowledge base, with citations that can be checked.
 * The Session 3 {@code /ask} endpoint stays as it was, as the baseline to compare against.
 */
@Service
public class GroundedAssistant {

    private static final Logger log = LoggerFactory.getLogger(GroundedAssistant.class);

    private final ChatClient chat;

    private final RagPipelineFactory pipelines;

    private final boolean nativeStructuredOutput;

    public GroundedAssistant(ChatClient.Builder builder, RagPipelineFactory pipelines,
                             @Value("classpath:prompts/system-v1.st") Resource systemPrompt,
                             @Value("${supportpilot.rag.native-structured-output:true}") boolean nativeStructuredOutput) {
        this.chat = builder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.pipelines = pipelines;
        this.nativeStructuredOutput = nativeStructuredOutput;
    }

    public GroundedAnswer answer(String question, RagOptions options, KnowledgeFilter filter) {
        long start = System.nanoTime();
        RagTrace trace = new RagTrace();
        Map<String, Object> context = new HashMap<>();
        context.put(RagTrace.KEY, trace);   // every pipeline stage records itself here
        filter.putInto(context);

        ChatClient.CallResponseSpec call = this.chat.prompt()
                .user(question)
                .advisors(this.pipelines.advisor(options))
                .advisors(a -> a.params(context))
                .call();

        // Checkpoint E: ask for a structured answer with quotes, then verify every quote.
        ResponseEntity<ChatResponse, AnswerWithCitations> result = call.responseEntity(AnswerWithCitations.class, spec -> {
            spec.validateSchema();
            if (this.nativeStructuredOutput) {
                spec.useProviderStructuredOutput();
            }
        });
        ChatResponse response = result.getResponse();
        AnswerWithCitations structured = result.getEntity();
        List<Document> used = documentsIn(response);
        String answer = (structured != null && structured.answer() != null) ? structured.answer() : "";
        boolean answerable = structured != null && structured.answerable() && !used.isEmpty();
        List<CitedSource> citations = answerable ? CitationVerifier.verify(structured.citations(), used) : List.of();
        if (citations.stream().anyMatch(c -> !c.verified())) {
            log.warn("Unverified citation(s) for question '{}': {}", question, citations);
        }

        Usage usage = (response != null) ? response.getMetadata().getUsage() : null;
        trace.record("generate", trace.lastEndNanos(), (usage != null)
                ? usage.getPromptTokens() + " prompt + " + usage.getCompletionTokens() + " completion tokens" : "");
        return new GroundedAnswer(answer, answerable, citations, used.stream().map(SearchHit::from).toList(),
                trace.stages(),
                (usage != null) ? usage.getPromptTokens() : null,
                (usage != null) ? usage.getCompletionTokens() : null,
                (System.nanoTime() - start) / 1_000_000);
    }

    /** Retrieval only - shows which chunks would reach the prompt, without generating an answer. */
    public RetrievalResult retrieve(String question, RagOptions options, KnowledgeFilter filter) {
        long start = System.nanoTime();
        RagTrace trace = new RagTrace();
        Map<String, Object> context = new HashMap<>();
        context.put(RagTrace.KEY, trace);
        filter.putInto(context);
        List<Document> documents = this.pipelines.retrieve(question, options, context);
        return new RetrievalResult(options, documents.stream().map(SearchHit::from).toList(), trace.stages(),
                (System.nanoTime() - start) / 1_000_000);
    }

    /** RetrievalAugmentationAdvisor puts the chunks it used into the response metadata. */
    private static List<Document> documentsIn(ChatResponse response) {
        return (response == null) ? List.of()
                : RagPipelineFactory.documents(response.getMetadata().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT));
    }

}
