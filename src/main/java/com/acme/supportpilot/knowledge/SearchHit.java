package com.acme.supportpilot.knowledge;

import java.util.Map;

import org.springframework.ai.document.Document;

/** Checkpoint D: one search result, with what you need to judge it. */
public record SearchHit(double score, String source, String section, Object page, Object chunkIndex,
                        String preview) {

    static SearchHit from(Document d) {
        Map<String, Object> m = d.getMetadata();
        String text = (d.getText() == null) ? "" : d.getText().replaceAll("\\s+", " ").strip();
        double score = (d.getScore() == null) ? 0.0 : Math.round(d.getScore() * 1000) / 1000.0;
        return new SearchHit(score,
                String.valueOf(m.getOrDefault("source", "?")),
                String.valueOf(m.getOrDefault("title", m.getOrDefault("doc_title", ""))),
                m.get("page_number"),
                m.get("chunk_index"),
                text.length() <= 200 ? text : text.substring(0, 197) + "...");
    }

}
