package com.acme.supportpilot.rag;

import java.util.List;

import com.acme.supportpilot.knowledge.SearchHit;

/** The output of /retrieve: what the pipeline would put in the prompt, and how it got there. */
public record RetrievalResult(RagOptions options, List<SearchHit> chunks, List<RagTrace.Stage> trace, long latencyMs) {
}
