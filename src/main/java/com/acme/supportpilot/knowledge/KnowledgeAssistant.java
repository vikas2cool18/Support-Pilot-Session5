package com.acme.supportpilot.knowledge;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Checkpoint E: "naive" RAG - retrieve the top-k chunks for the question, put them in the
 * prompt, and let the model answer from them. QuestionAnswerAdvisor does all three steps.
 * Session 4 replaces this with query rewriting, hybrid search, re-ranking and citations.
 */
@Service
public class KnowledgeAssistant {

    private final ChatClient chat;

    public KnowledgeAssistant(ChatClient.Builder builder, VectorStore vectorStore,
                              @Value("classpath:prompts/system-v1.st") Resource systemPrompt,
                              @Value("${supportpilot.knowledge.top-k:5}") int topK,
                              @Value("${supportpilot.knowledge.similarity-threshold:0.3}") double threshold) {
        this.chat = builder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder().topK(topK).similarityThreshold(threshold).build())
                                .build(),
                        new SimpleLoggerAdvisor())
                .build();
    }

    public KnowledgeAnswer ask(String question) {
        long start = System.nanoTime();
        ChatResponse response = this.chat.prompt()
                .user(question)
                .call()
                .chatResponse();
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        if (response == null) {
            throw new IllegalStateException("The model returned no response");
        }
        // The advisor puts the retrieved chunks into the response metadata.
        List<Document> retrieved = response.getMetadata().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
        List<SearchHit> sources = (retrieved == null) ? List.of() : retrieved.stream().map(SearchHit::from).toList();
        Usage usage = response.getMetadata().getUsage();
        String answer = (response.getResult() != null) ? response.getResult().getOutput().getText() : "";
        return new KnowledgeAnswer(answer, sources, usage.getPromptTokens(), usage.getCompletionTokens(), latencyMs);
    }

}
