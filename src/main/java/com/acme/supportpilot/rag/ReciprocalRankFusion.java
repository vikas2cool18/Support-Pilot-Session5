package com.acme.supportpilot.rag;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;

/**
 * Checkpoint D: Reciprocal Rank Fusion (Cormack et al., 2009).
 *
 * <pre>
 *   score(d) = sum over every ranking that contains d of  1 / (k + rank(d))
 * </pre>
 *
 * Only ranks are used, never the raw scores - which is why RRF can combine a cosine similarity
 * (0..1) with a full-text rank (any positive number) without any normalisation.
 * A document that appears high in several lists beats one that is first in only one.
 */
public final class ReciprocalRankFusion {

    private ReciprocalRankFusion() {
    }

    /**
     * Fuses several rankings into one, best first. Documents are matched by id; the first copy
     * seen is kept, so put the ranking with the richest metadata first.
     */
    public static List<Document> fuse(List<List<Document>> rankings, int k) {
        Map<String, Double> scores = new HashMap<>();
        Map<String, Document> firstSeen = new LinkedHashMap<>();
        for (List<Document> ranking : rankings) {
            for (int i = 0; i < ranking.size(); i++) {
                Document d = ranking.get(i);
                scores.merge(d.getId(), 1.0 / (k + i + 1), Double::sum);   // ranks start at 1
                firstSeen.putIfAbsent(d.getId(), d);
            }
        }
        return firstSeen.values().stream()
                .map(d -> withScore(d, scores.get(d.getId())))
                .sorted(Comparator.comparingDouble((Document d) -> d.getScore()).reversed())
                .toList();
    }

    static Document withScore(Document d, double score) {
        Map<String, Object> metadata = new HashMap<>(d.getMetadata());
        metadata.put("rrf_score", score);
        return d.mutate().metadata(metadata).score(score).build();
    }

}
