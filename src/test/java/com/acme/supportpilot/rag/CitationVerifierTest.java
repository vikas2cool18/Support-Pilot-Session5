package com.acme.supportpilot.rag;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.ai.document.Document;

import com.acme.supportpilot.rag.AnswerWithCitations.Citation;

import static org.assertj.core.api.Assertions.assertThat;

class CitationVerifierTest {

    private final List<Document> docs = List.of(new Document(
            "Plans and Pricing\n\nPRO costs **INR 999** per user per month and supports up to 25 users.",
            Map.of("source", "plans-and-pricing.pdf", "page_number", 1)));

    @Test
    void exactQuoteIsVerifiedDespiteCaseSpacingAndMarkdown() {
        List<CitedSource> result = CitationVerifier.verify(
                List.of(new Citation(1, "PRO costs INR 999 per user per month and   supports up to 25 users.")), this.docs);

        assertThat(result).singleElement().satisfies(c -> {
            assertThat(c.verified()).isTrue();
            assertThat(c.source()).isEqualTo("plans-and-pricing.pdf");
        });
    }

    @Test
    void paraphraseWrongNumberAndTinyQuotesAreRejected() {
        List<CitedSource> result = CitationVerifier.verify(List.of(
                new Citation(1, "PRO is limited to twenty-five users."),
                new Citation(3, "PRO costs INR 999 per user per month"),
                new Citation(1, "PRO")), this.docs);

        assertThat(result).extracting(CitedSource::verified).containsExactly(false, false, false);
    }

}
