package com.acme.supportpilot.rag;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.ai.document.Document;

/**
 * Checkpoint E: never trust a citation you have not checked. A citation is verified only if
 * <ol>
 *   <li>its source number was really in the prompt, and</li>
 *   <li>its quote really appears in that chunk (ignoring case, spacing and typographic quotes).</li>
 * </ol>
 * Models sometimes paraphrase "quotes" or cite the wrong number; this catches both cheaply,
 * without another LLM call. Session 5 turns the same check into an automated test.
 */
public final class CitationVerifier {

    private CitationVerifier() {
    }

    public static List<CitedSource> verify(List<AnswerWithCitations.Citation> citations, List<Document> documents) {
        if (citations == null) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<CitedSource> result = new ArrayList<>();
        for (AnswerWithCitations.Citation c : citations) {
            if (c == null || !seen.add(c.source() + "|" + c.quote())) {
                continue;
            }
            boolean inRange = c.source() >= 1 && c.source() <= documents.size();
            boolean quoteFound = inRange && quoteAppearsIn(c.quote(), documents.get(c.source() - 1).getText());
            result.add(CitationMarkers.resolve(c.source(), c.quote(), documents, quoteFound));
        }
        return result;
    }

    /**
     * The words of the quote must appear verbatim and in order in the chunk. Punctuation and
     * formatting are ignored: models drop Markdown table pipes, list bullets and arrows, or turn
     * "Admin | Manage users" into "Admin: Manage users" - same words, still a real quote. A quote
     * shortened with an ellipsis is accepted when every part is verbatim and the parts appear in order.
     */
    static boolean quoteAppearsIn(String quote, String text) {
        // Padded with spaces so only whole words match: "inr 99" must not match "inr 999".
        String haystack = " " + normalize(text) + " ";
        String[] parts = (quote == null) ? new String[0] : quote.split("\\.{3,}|…");
        int from = 0;
        int matched = 0;
        for (String part : parts) {
            String p = normalize(part);
            if (p.isEmpty()) {
                continue;
            }
            int at = haystack.indexOf(" " + p + " ", from);
            if (at < 0) {
                return false;
            }
            from = at + p.length() + 1;
            matched += p.length();
        }
        return matched >= 8;
    }

    /** Lower case, letters and digits only, single spaces: "**INR 999** / user" -> "inr 999 user". */
    static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFKC)
                .toLowerCase()
                .replaceAll("['’‘]", "")                  // don't -> dont, so curly and straight apostrophes match
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .strip();
    }

}
