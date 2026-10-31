package com.acme.supportpilot.rag;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

/**
 * Wraps the pipeline components so each one records its timing and output in the {@link RagTrace}.
 * The components are functional interfaces, so a wrapper is just a lambda around the original.
 */
final class Traced {

    private Traced() {
    }

    static QueryTransformer transformer(String stage, QueryTransformer delegate) {
        return query -> {
            long start = System.nanoTime();
            Query result = delegate.transform(query);
            RagTrace.from(query.context()).record(stage, start, result.text());
            return result;
        };
    }

    static QueryExpander expander(QueryExpander delegate) {
        return query -> {
            long start = System.nanoTime();
            // distinct(): the advisor collects results in a map keyed by query, so two identical
            // variants (it happens with small models) would otherwise make it fail.
            List<Query> result = delegate.expand(query).stream().distinct().toList();
            RagTrace.from(query.context()).record("expand", start,
                    result.stream().map(Query::text).collect(Collectors.joining(" | ")));
            return result;
        };
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
