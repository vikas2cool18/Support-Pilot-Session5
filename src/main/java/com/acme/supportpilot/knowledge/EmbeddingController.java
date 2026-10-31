package com.acme.supportpilot.knowledge;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.acme.supportpilot.web.InvalidRequestException;

/**
 * Checkpoint B: an embeddings playground.
 * GET /embeddings?text=...                          -> what an embedding looks like
 * GET /embeddings/similarity?text=a&amp;text=b&amp;text=c   -> how close their meanings are
 */
@RestController
@RequestMapping("/embeddings")
public class EmbeddingController {

    private final EmbeddingModel embeddingModel;

    public EmbeddingController(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public record EmbeddingInfo(String text, int dimensions, double norm, float[] firstValues) {
    }

    public record SimilarityMatrix(List<String> texts, int dimensions, double[][] cosine) {
    }

    @GetMapping
    public EmbeddingInfo embed(@RequestParam String text) {
        float[] vector = this.embeddingModel.embed(text);
        return new EmbeddingInfo(text, vector.length, round(VectorMath.norm(vector)),
                Arrays.copyOf(vector, Math.min(8, vector.length)));
    }

    @GetMapping("/similarity")
    public SimilarityMatrix similarity(@RequestParam List<String> text) {
        if (text.size() < 2 || text.size() > 10) {
            throw new InvalidRequestException("Pass between 2 and 10 'text' parameters.");
        }
        List<float[]> vectors = this.embeddingModel.embed(text);
        double[][] matrix = new double[text.size()][text.size()];
        for (int i = 0; i < text.size(); i++) {
            for (int j = 0; j < text.size(); j++) {
                matrix[i][j] = round(VectorMath.cosine(vectors.get(i), vectors.get(j)));
            }
        }
        return new SimilarityMatrix(text, vectors.get(0).length, matrix);
    }

    private static double round(double value) {
        return Math.round(value * 1000) / 1000.0;
    }

}
