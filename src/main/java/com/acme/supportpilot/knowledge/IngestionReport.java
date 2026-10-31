package com.acme.supportpilot.knowledge;

import java.util.List;

/** What one ingestion run did. */
public record IngestionReport(String chunking, List<FileReport> files, int totalChunks, long durationMs) {

    /** sections = Documents produced by the reader (pages or headings); chunks = what was embedded. */
    public record FileReport(String file, int sections, int chunks) {
    }

}
