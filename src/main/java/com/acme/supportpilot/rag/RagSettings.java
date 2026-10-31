package com.acme.supportpilot.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Pipeline settings from application.yml ({@code supportpilot.rag.*}). */
@Component
public class RagSettings {

    private final RagOptions defaults;

    private final int candidates;

    private final double similarityThreshold;

    private final int rrfK;

    private final int minRelevance;

    public RagSettings(@Value("${supportpilot.rag.rewrite:false}") boolean rewrite,
                       @Value("${supportpilot.rag.expansions:0}") int expansions,
                       @Value("${supportpilot.rag.hybrid:false}") boolean hybrid,
                       @Value("${supportpilot.rag.rerank:false}") boolean rerank,
                       @Value("${supportpilot.rag.top-n:5}") int topN,
                       @Value("${supportpilot.rag.candidates:20}") int candidates,
                       @Value("${supportpilot.rag.rrf-k:60}") int rrfK,
                       @Value("${supportpilot.rag.min-relevance:3}") int minRelevance,
                       @Value("${supportpilot.knowledge.similarity-threshold:0.3}") double similarityThreshold) {
        this.defaults = new RagOptions(rewrite, expansions, hybrid, rerank, topN);
        this.candidates = candidates;
        this.similarityThreshold = similarityThreshold;
        this.rrfK = rrfK;
        this.minRelevance = minRelevance;
    }

    /** The switches used when a request does not override them. */
    public RagOptions defaults() {
        return this.defaults;
    }

    /** Chunks fetched per search before fusion and re-ranking narrow them down. */
    public int candidates() {
        return this.candidates;
    }

    /** Minimum cosine similarity for the vector search (model-specific, see Session 3). */
    public double similarityThreshold() {
        return this.similarityThreshold;
    }

    /** The k constant of Reciprocal Rank Fusion; 60 is the value from the original paper. */
    public int rrfK() {
        return this.rrfK;
    }

    /** Candidates the re-ranker scores below this (0-10) are dropped. */
    public int minRelevance() {
        return this.minRelevance;
    }

}
