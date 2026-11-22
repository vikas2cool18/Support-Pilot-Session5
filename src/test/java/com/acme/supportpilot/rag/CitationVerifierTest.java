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

    @Test
    void formattingTheModelDropsIsIgnored() {
        String chunk = """
                | Role | What they can do |
                |---|---|
                | Admin | Manage users, settings, integrations and all data; cannot close the account |

                Change a role from **Settings → Users → Edit role**.

                - Exports are available as CSV
                - Imports accept up to 10,000 rows""";

        assertThat(CitationVerifier.quoteAppearsIn("Admin: Manage users, settings, integrations and all data", chunk)).isTrue();
        assertThat(CitationVerifier.quoteAppearsIn("Settings > Users > Edit role", chunk)).isTrue();
        assertThat(CitationVerifier.quoteAppearsIn("Exports are available as CSV. Imports accept up to 10,000 rows.", chunk)).isTrue();
        assertThat(CitationVerifier.quoteAppearsIn("\"Change a role from Settings\"", chunk)).isTrue();
    }

    @Test
    void ellipsisPartsMustBeVerbatimAndInOrder() {
        String chunk = "Data for Indian accounts is stored only in India: primary data in Mumbai and encrypted backups in Hyderabad.";

        assertThat(CitationVerifier.quoteAppearsIn("stored only in India ... encrypted backups in Hyderabad", chunk)).isTrue();
        assertThat(CitationVerifier.quoteAppearsIn("stored only in India… backups in Mumbai", chunk)).isFalse();
        assertThat(CitationVerifier.quoteAppearsIn("encrypted backups in Hyderabad ... stored only in India", chunk)).isFalse();
    }

    @Test
    void onlyWholeWordsMatch() {
        assertThat(CitationVerifier.quoteAppearsIn("PRO costs INR 99 per user", this.docs.get(0).getText())).isFalse();
        assertThat(CitationVerifier.quoteAppearsIn("costs INR 999 per user", this.docs.get(0).getText())).isTrue();
    }

}
