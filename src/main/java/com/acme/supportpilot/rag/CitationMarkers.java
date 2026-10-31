package com.acme.supportpilot.rag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.document.Document;

/**
 * Checkpoint A: finds inline citation markers such as [1] or [2, 3] in an answer and maps them
 * to the numbered chunks that were in the prompt. A marker that points to a number that was not
 * in the prompt is returned with verified = false - a sign of an invented citation.
 */
public final class CitationMarkers {

    private static final Pattern MARKER = Pattern.compile("\\[(\\d+(?:\\s*,\\s*\\d+)*)]");

    private CitationMarkers() {
    }

    public static List<CitedSource> parse(String answer, List<Document> documents) {
        Set<Integer> numbers = new LinkedHashSet<>();
        Matcher m = MARKER.matcher((answer == null) ? "" : answer);
        while (m.find()) {
            for (String n : m.group(1).split(",")) {
                numbers.add(Integer.parseInt(n.strip()));
            }
        }
        List<CitedSource> result = new ArrayList<>();
        for (int n : numbers) {
            result.add(resolve(n, null, documents, true));
        }
        return result;
    }

    /** Maps source number n (1-based) to its document; unknown numbers are never verified. */
    static CitedSource resolve(int n, String quote, List<Document> documents, boolean quoteOk) {
        if (n < 1 || n > documents.size()) {
            return new CitedSource(n, "?", null, "", quote, false);
        }
        Document d = documents.get(n - 1);
        Object section = d.getMetadata().getOrDefault("title", d.getMetadata().getOrDefault("doc_title", ""));
        return new CitedSource(n, String.valueOf(d.getMetadata().getOrDefault("source", "?")),
                d.getMetadata().get("page_number"), String.valueOf(section), quote, quoteOk);
    }

}
