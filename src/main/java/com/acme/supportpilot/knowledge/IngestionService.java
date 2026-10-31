package com.acme.supportpilot.knowledge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

/**
 * Checkpoint C: the ETL pipeline for RAG.
 *   Extract   - DocumentLoader reads the file into Documents
 *   Transform - add catalog metadata and a context header, split into chunks
 *   Load      - VectorStore embeds each chunk and stores text + vector + metadata
 *
 * Re-running is safe (idempotent): old chunks of a file are deleted before the new ones are added.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final KnowledgeCatalog catalog;

    private final DocumentLoader loader;

    private final Chunkers chunkers;

    private final VectorStore vectorStore;

    public IngestionService(KnowledgeCatalog catalog, DocumentLoader loader, Chunkers chunkers,
                            VectorStore vectorStore) {
        this.catalog = catalog;
        this.loader = loader;
        this.chunkers = chunkers;
        this.vectorStore = vectorStore;
    }

    public IngestionReport ingestAll() {
        long start = System.nanoTime();
        List<IngestionReport.FileReport> files = new ArrayList<>();
        for (KnowledgeCatalog.Entry entry : this.catalog.entries()) {
            files.add(ingest(entry));
        }
        int total = files.stream().mapToInt(IngestionReport.FileReport::chunks).sum();
        return new IngestionReport(this.chunkers.describe(), files, total, (System.nanoTime() - start) / 1_000_000);
    }

    public IngestionReport ingestOne(String file) {
        long start = System.nanoTime();
        IngestionReport.FileReport report = ingest(this.catalog.entry(file));
        return new IngestionReport(this.chunkers.describe(), List.of(report), report.chunks(),
                (System.nanoTime() - start) / 1_000_000);
    }

    private IngestionReport.FileReport ingest(KnowledgeCatalog.Entry entry) {
        Path path = this.catalog.directory().resolve(entry.file());
        if (!Files.exists(path)) {
            throw new IllegalStateException("File listed in the catalog does not exist: " + path);
        }

        // 1. Extract
        List<Document> sections = this.loader.load(path);

        // 2. Transform: metadata + a short context header on each section, then split.
        //    The splitter copies the metadata to every chunk.
        List<Document> prepared = sections.stream()
                .filter(d -> d.getText() != null && !d.getText().isBlank())
                .map(d -> withContext(d, entry))
                .toList();
        List<Document> chunks = this.chunkers.splitter().apply(prepared).stream()
                .filter(d -> d.getText() != null && !d.getText().isBlank())
                .toList();

        // 3. Load: delete this file's old chunks, then add the new ones (embedding happens here)
        this.vectorStore.delete(new FilterExpressionBuilder().eq("source", entry.file()).build());
        this.vectorStore.add(chunks);

        log.info("Ingested {}: {} sections -> {} chunks", entry.file(), prepared.size(), chunks.size());
        return new IngestionReport.FileReport(entry.file(), prepared.size(), chunks.size());
    }

    /**
     * Readers keep headings and page numbers in metadata, not in the text. Only text is embedded,
     * so we prepend a one-line "context header" - document title and section - to each section.
     * A chunk that only says "The link is valid for 30 minutes" is then still findable as
     * "User Guide > Resetting your password".
     */
    static Document withContext(Document section, KnowledgeCatalog.Entry entry) {
        Map<String, Object> metadata = new HashMap<>(section.getMetadata());
        metadata.putAll(entry.metadata());
        Object heading = section.getMetadata().get("title");
        String header = (heading != null) ? entry.title() + " > " + heading : entry.title();
        return Document.builder()
                .text(header + "\n\n" + section.getText())
                .metadata(metadata)
                .build();
    }

}
