package com.acme.supportpilot.rag;

import java.util.List;

import com.acme.supportpilot.knowledge.SearchHit;

/**
 * Session 4: an answer that can be checked.
 *
 * @param answer           the answer, with citation markers such as [1]
 * @param answerable       false when the knowledge base does not contain the answer
 * @param citations        the sources the answer cites, resolved and verified
 * @param sources          every chunk that was in the prompt, in prompt order ([1] is the first)
 * @param trace            each pipeline stage with its duration and output
 * @param promptTokens     input tokens of the answer call
 * @param completionTokens output tokens of the answer call
 * @param latencyMs        end-to-end time, including all retrieval stages
 */
public record GroundedAnswer(String answer, boolean answerable, List<CitedSource> citations, List<SearchHit> sources,
                             List<RagTrace.Stage> trace, Integer promptTokens, Integer completionTokens,
                             long latencyMs) {
}
