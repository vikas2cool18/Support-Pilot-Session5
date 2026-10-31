package com.acme.supportpilot.rag;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;

/**
 * Checkpoint D: combines the results of several query variants (Checkpoint B) with Reciprocal
 * Rank Fusion instead of simple concatenation. A chunk found by several variants rises to the top.
 */
public class RrfDocumentJoiner implements DocumentJoiner {

    private final int k;

    public RrfDocumentJoiner(int k) {
        this.k = k;
    }

    @Override
    public List<Document> join(Map<Query, List<List<Document>>> documentsForQuery) {
        List<List<Document>> rankings = documentsForQuery.values().stream()
                .flatMap(List::stream)
                .toList();
        return ReciprocalRankFusion.fuse(rankings, this.k);
    }

}
