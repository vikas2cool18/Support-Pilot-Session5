package com.acme.supportpilot.rag;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.ai.document.Document;

import static org.assertj.core.api.Assertions.assertThat;

class CitationMarkersTest {

    private final List<Document> docs = List.of(
            new Document("a", Map.of("source", "faq.md", "title", "Billing")),
            new Document("b", Map.of("source", "refund-policy.pdf", "page_number", 2)));

    @Test
    void findsSingleAndGroupedMarkersInOrderWithoutDuplicates() {
        List<CitedSource> cited = CitationMarkers.parse("Yes [2]. You can also pay by UPI [1, 2].", this.docs);

        assertThat(cited).extracting(CitedSource::number).containsExactly(2, 1);
        assertThat(cited.get(0).source()).isEqualTo("refund-policy.pdf");
        assertThat(cited.get(0).page()).isEqualTo(2);
        assertThat(cited.get(1).section()).isEqualTo("Billing");
    }

    @Test
    void unknownNumbersAreNotVerifiedAndTextWithoutMarkersHasNoCitations() {
        assertThat(CitationMarkers.parse("See [7].", this.docs)).singleElement()
                .satisfies(c -> assertThat(c.verified()).isFalse());
        assertThat(CitationMarkers.parse("No citations here.", this.docs)).isEmpty();
    }

}
