package com.acme.supportpilot.knowledge;

import java.util.List;

/** Checkpoint E: the answer, the chunks it was based on, and what it cost. */
public record KnowledgeAnswer(String answer, List<SearchHit> sources, Integer promptTokens,
                              Integer completionTokens, long latencyMs) {
}
