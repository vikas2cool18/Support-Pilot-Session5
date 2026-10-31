package com.acme.supportpilot.rag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

/**
 * Checkpoint D: hybrid search. Runs a vector search (meaning) and a keyword search (exact words)
 * in parallel, then fuses the two rankings with Reciprocal Rank Fusion.
 *
 * <p>Each result is tagged with {@code matched_by}: vector, keyword or both - useful when you
 * want to see which leg found what.
 */
public class HybridDocumentRetriever implements DocumentRetriever {

    private final DocumentRetriever vector;

    private final DocumentRetriever keyword;

    private final int rrfK;

    private final int limit;

    public HybridDocumentRetriever(DocumentRetriever vector, DocumentRetriever keyword, int rrfK, int limit) {
        this.vector = vector;
        this.keyword = keyword;
        this.rrfK = rrfK;
        this.limit = limit;
    }

    @Override
    public List<Document> retrieve(Query query) {
        // The two searches are independent, so run them at the same time.
        CompletableFuture<List<Document>> vectorHits = CompletableFuture.supplyAsync(() -> this.vector.retrieve(query));
        CompletableFuture<List<Document>> keywordHits = CompletableFuture.supplyAsync(() -> this.keyword.retrieve(query));
        List<Document> byMeaning = vectorHits.join();
        List<Document> byWords = keywordHits.join();

        Set<String> vectorIds = byMeaning.stream().map(Document::getId).collect(Collectors.toSet());
        Set<String> keywordIds = byWords.stream().map(Document::getId).collect(Collectors.toSet());
        // Vector results first: they carry the full metadata from the vector store.
        return ReciprocalRankFusion.fuse(List.of(byMeaning, byWords), this.rrfK).stream()
                .limit(this.limit)
                .map(d -> tag(d, vectorIds.contains(d.getId()), keywordIds.contains(d.getId())))
                .toList();
    }

    private static Document tag(Document d, boolean byVector, boolean byKeyword) {
        Map<String, Object> metadata = new HashMap<>(d.getMetadata());
        metadata.put("matched_by", (byVector && byKeyword) ? "both" : byVector ? "vector" : "keyword");
        return d.mutate().metadata(metadata).build();
    }

}
