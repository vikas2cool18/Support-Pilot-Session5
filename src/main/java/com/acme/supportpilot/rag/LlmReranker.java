package com.acme.supportpilot.rag;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.stereotype.Component;

/**
 * Checkpoint E: re-ranking with a language model.
 *
 * <p>Retrieval is tuned for recall: fetch 20 candidates so the right chunk is probably among them.
 * The re-ranker is tuned for precision: it reads the question and every candidate together,
 * scores each 0-10, keeps the best {@code topN} and drops anything below the minimum score.
 * Dropping weak chunks is also what lets an unanswerable question end with an honest
 * "I don't know" instead of an answer built from loosely related text.
 *
 * <p>Production systems usually use a dedicated cross-encoder re-ranker instead - faster and
 * cheaper per call (e.g. Cohere Rerank, Voyage, Jina, or bge-reranker on your own GPU).
 * The pipeline does not care: anything that implements DocumentPostProcessor fits here.
 */
@Component
public class LlmReranker {

    private static final Logger log = LoggerFactory.getLogger(LlmReranker.class);

    private static final int MAX_CHARS = 700;

    private static final String PROMPT = """
            Rate how useful each passage is for answering the question.
            Score from 0 (irrelevant) to 10 (answers the question directly).
            Rate every passage, using its id. Judge only what the passage says.

            Question: {query}

            Passages:
            {passages}
            """;

    /** What we ask the model to return. */
    record PassageScore(int id, int score) {
    }

    record RelevanceScores(List<PassageScore> scores) {
    }

    private final ChatClient chat;

    private final int minRelevance;

    private final int maxCandidates;

    public LlmReranker(RagHelperModel helper, RagSettings settings) {
        this.chat = helper.builder().build();
        this.minRelevance = settings.minRelevance();
        this.maxCandidates = settings.candidates();
    }

    public List<Document> rerank(Query query, List<Document> candidates, int topN) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        List<Document> pool = candidates.subList(0, Math.min(candidates.size(), this.maxCandidates));
        try {
            RelevanceScores result = this.chat.prompt()
                    .user(u -> u.text(PROMPT).param("query", query.text()).param("passages", passages(pool)))
                    .call()
                    .entity(RelevanceScores.class);
            Map<Integer, Integer> scoreById = new HashMap<>();
            if (result != null && result.scores() != null) {
                result.scores().forEach(s -> scoreById.put(s.id(), s.score()));
            }
            return IntStream.range(0, pool.size())
                    .mapToObj(i -> withRelevance(pool.get(i), scoreById.getOrDefault(i + 1, 0)))
                    .filter(d -> relevance(d) >= this.minRelevance)
                    .sorted(Comparator.comparingInt(LlmReranker::relevance).reversed())   // stable: ties keep fused order
                    .limit(topN)
                    .toList();
        }
        catch (RuntimeException ex) {
            // A failed re-rank must not fail the answer: fall back to the retrieval order.
            log.warn("Re-ranking failed, keeping the retrieval order: {}", ex.getMessage());
            return pool.stream().limit(topN).toList();
        }
    }

    static String passages(List<Document> pool) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pool.size(); i++) {
            String text = (pool.get(i).getText() == null) ? "" : pool.get(i).getText().replaceAll("\\s+", " ").strip();
            sb.append("id ").append(i + 1).append(": ")
                    .append(text.length() <= MAX_CHARS ? text : text.substring(0, MAX_CHARS) + " ...")
                    .append("\n\n");
        }
        return sb.toString().strip();
    }

    private static Document withRelevance(Document d, int score) {
        Map<String, Object> metadata = new HashMap<>(d.getMetadata());
        metadata.put("rerank_score", score);
        return d.mutate().metadata(metadata).score(score / 10.0).build();
    }

    private static int relevance(Document d) {
        return (d.getMetadata().get("rerank_score") instanceof Integer score) ? score : 0;
    }

}
