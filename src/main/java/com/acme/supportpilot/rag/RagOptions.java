package com.acme.supportpilot.rag;

/**
 * The switches of the retrieval pipeline for one request.
 *
 * <p>Defaults come from application.yml ({@code supportpilot.rag.*}). {@code /answer} and
 * {@code /retrieve} can override each one, so the same question can be compared with and
 * without a technique. Each checkpoint wires up one more switch:
 * <ul>
 *   <li>{@code rewrite}, {@code expansions} - Checkpoint B (query transformation)</li>
 *   <li>{@code hybrid} - Checkpoint D (keyword + vector search, fused with RRF)</li>
 *   <li>{@code rerank} - Checkpoint E (re-ranking the candidates)</li>
 * </ul>
 *
 * @param rewrite    rewrite the user's question into a clean search query
 * @param expansions number of extra query variants to search with (0 = off)
 * @param hybrid     combine full-text (keyword) search with vector search
 * @param rerank     let a model re-score the candidates and keep the best
 * @param topN       how many chunks finally go into the prompt
 */
public record RagOptions(boolean rewrite, int expansions, boolean hybrid, boolean rerank, int topN) {

    public RagOptions {
        if (expansions < 0 || expansions > 5) {
            throw new IllegalArgumentException("expansions must be 0-5");
        }
        if (topN < 1 || topN > 20) {
            throw new IllegalArgumentException("topN must be 1-20");
        }
    }

    /** The Session 3 baseline: one vector search with the raw question. */
    public static RagOptions naive(int topN) {
        return new RagOptions(false, 0, false, false, topN);
    }

    /** Returns a copy with any non-null override applied. */
    public RagOptions with(Boolean rewrite, Integer expansions, Boolean hybrid, Boolean rerank) {
        return new RagOptions(
                (rewrite != null) ? rewrite : this.rewrite,
                (expansions != null) ? expansions : this.expansions,
                (hybrid != null) ? hybrid : this.hybrid,
                (rerank != null) ? rerank : this.rerank,
                this.topN);
    }

}
