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

    static boolean quoteAppearsIn(String quote, String text) {
        String q = normalize(quote).replaceAll("[.\\s]+$", "");
        return q.length() >= 8 && normalize(text).contains(q);
    }

    static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFKC)
                .replaceAll("[‘’]", "'")
                .replaceAll("[“”]", "\"")
                .replaceAll("[–—]", "-")
                .replaceAll("\\*\\*|__|`", "")          // Markdown emphasis in the chunk text
                .replaceAll("\\s+", " ")
                .toLowerCase()
                .strip();
    }

}
