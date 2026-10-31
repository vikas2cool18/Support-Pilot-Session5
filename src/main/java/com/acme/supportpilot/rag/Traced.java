package com.acme.supportpilot.rag;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

/**
 * Wraps the pipeline components so each one records its timing and output in the {@link RagTrace}.
 * The components are functional interfaces, so a wrapper is just a lambda around the original.
 */
final class Traced {

    private Traced() {
    }

    static DocumentRetriever retriever(String stage, DocumentRetriever delegate) {
        return query -> {
            long start = System.nanoTime();
            List<Document> result = delegate.retrieve(query);
            RagTrace.from(query.context()).record(stage, start, result.size() + " chunks for: " + query.text());
            return result;
        };
    }

    static DocumentPostProcessor postProcessor(String stage, DocumentPostProcessor delegate) {
        return (query, documents) -> {
            long start = System.nanoTime();
            List<Document> result = delegate.process(query, documents);
            RagTrace.from(query.context()).record(stage, start, documents.size() + " -> " + result.size() + " chunks");
            return result;
        };
    }

}
