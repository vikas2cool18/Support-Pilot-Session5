package com.acme.supportpilot.knowledge;

/**
 * Checkpoint B: the maths behind semantic search, written out so you can see it.
 * Spring AI and pgvector do this for you - in production you never call these methods yourself.
 */
public final class VectorMath {

    private VectorMath() {
    }

    /** Cosine similarity: 1.0 = same direction (same meaning), 0 = unrelated, -1 = opposite. */
    public static double cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors have different dimensions: " + a.length + " vs " + b.length);
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** Length of the vector. Many embedding models return vectors of length 1 (normalised). */
    public static double norm(float[] v) {
        double sum = 0;
        for (float x : v) {
            sum += x * x;
        }
        return Math.sqrt(sum);
    }

}
