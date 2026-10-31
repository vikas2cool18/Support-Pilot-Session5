package com.acme.supportpilot.rag;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Checkpoint E: the structured answer we ask the model for. Spring AI turns this record into a
 * JSON schema; the descriptions become part of the schema the model sees.
 */
public record AnswerWithCitations(
        @JsonPropertyDescription("Short answer for the customer, with source numbers in square brackets, e.g. [2]")
        String answer,
        @JsonPropertyDescription("One entry per source used to write the answer; empty if not answerable")
        List<Citation> citations,
        @JsonPropertyDescription("True only if the sources contain the answer")
        boolean answerable) {

    /** A claim's evidence: which numbered source, and the exact supporting sentence from it. */
    public record Citation(
            @JsonPropertyDescription("Number of the source, e.g. 2 for [2]")
            int source,
            @JsonPropertyDescription("A sentence copied word for word from that source")
            String quote) {
    }

}
