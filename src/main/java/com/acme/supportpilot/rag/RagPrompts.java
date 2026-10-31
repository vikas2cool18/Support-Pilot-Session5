package com.acme.supportpilot.rag;

import java.util.List;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * The prompt templates of the RAG pipeline, and how retrieved chunks are shown to the model.
 * Every chunk gets a number, so the model can cite it as [1], [2] ... and we can map each
 * citation back to a document.
 */
@Component
public class RagPrompts {

    private final PromptTemplate answer;

    private final PromptTemplate emptyContext;

    private final String version;

    public RagPrompts(@Value("${supportpilot.rag.prompt-version:v1}") String version,
                      @Value("classpath:prompts/rag-answer-${supportpilot.rag.prompt-version:v1}.st") Resource answer,
                      @Value("classpath:prompts/rag-empty-context.st") Resource emptyContext) {
        this.version = version;
        this.answer = new PromptTemplate(answer);
        this.emptyContext = new PromptTemplate(emptyContext);
    }

    /** Answer template: must contain {query} and {context}. */
    public PromptTemplate answer() {
        return this.answer;
    }

    /** Used instead of the answer template when retrieval found nothing. */
    public PromptTemplate emptyContext() {
        return this.emptyContext;
    }

    public String version() {
        return this.version;
    }

    /**
     * Formats the chunks as numbered sources:
     * <pre>
     * [1] refund-policy.pdf, page 1
     * Refund and Billing Policy ...
     * </pre>
     */
    public static String numbered(List<Document> documents) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document d = documents.get(i);
            sb.append('[').append(i + 1).append("] ").append(label(d)).append('\n')
                    .append((d.getText() == null) ? "" : d.getText().strip()).append("\n\n");
        }
        return sb.toString().strip();
    }

    static String label(Document d) {
        Object source = d.getMetadata().getOrDefault("source", "unknown");
        Object page = d.getMetadata().get("page_number");
        return (page != null) ? source + ", page " + page : source.toString();
    }

}
