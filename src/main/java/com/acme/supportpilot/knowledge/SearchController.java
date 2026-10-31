package com.acme.supportpilot.knowledge;

import java.util.List;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.acme.supportpilot.web.InvalidRequestException;

/**
 * Checkpoint D: semantic search, without any LLM.
 * GET /search?q=refund for annual plan&amp;k=5&amp;threshold=0.0&amp;docType=policy
 */
@RestController
public class SearchController {

    private final VectorStore vectorStore;

    public SearchController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @GetMapping("/search")
    public List<SearchHit> search(@RequestParam String q,
                                  @RequestParam(defaultValue = "5") int k,
                                  @RequestParam(defaultValue = "0.0") double threshold,
                                  @RequestParam(required = false) String docType) {
        if (q.isBlank()) {
            throw new InvalidRequestException("The query is empty.");
        }
        SearchRequest.Builder request = SearchRequest.builder()
                .query(q)
                .topK(Math.clamp(k, 1, 20))
                .similarityThreshold(threshold);
        if (docType != null && docType.matches("[a-z-]{1,30}")) {
            request.filterExpression("doc_type == '" + docType + "'");   // metadata filter
        }
        return this.vectorStore.similaritySearch(request.build()).stream()
                .map(SearchHit::from)
                .toList();
    }

}
