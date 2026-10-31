package com.acme.supportpilot.rag;

import java.util.List;

import org.springframework.ai.document.Document;

/**
 * Session 5: one run of the RAG pipeline as an evaluator sees it - the answer returned to the
 * customer, and the full text of every chunk that was in the prompt ([1] is the first).
 */
public record RagRun(GroundedAnswer answer, List<Document> context) {
}
