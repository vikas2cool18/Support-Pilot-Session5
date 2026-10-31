package com.acme.supportpilot.eval;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Checkpoint C: what the LLM judge returns. Reasoning comes first on purpose: models that explain
 * before they score tend to score more consistently.
 */
public record JudgeScore(
        @JsonPropertyDescription("Two or three sentences explaining the scores, written before scoring")
        String reasoning,
        @JsonPropertyDescription("1-5: is every claim in the answer supported by the context?")
        int faithfulness,
        @JsonPropertyDescription("1-5: does the answer address the question that was asked?")
        int relevance,
        @JsonPropertyDescription("1-5: does the answer agree with the expected answer?")
        int correctness) {

    public JudgeScore {
        faithfulness = clamp(faithfulness);
        relevance = clamp(relevance);
        correctness = clamp(correctness);
    }

    private static int clamp(int score) {
        return Math.max(1, Math.min(5, score));
    }

}
