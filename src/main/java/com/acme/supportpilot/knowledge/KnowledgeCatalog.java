package com.acme.supportpilot.knowledge;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Checkpoint C: kb/catalog.csv lists every document and its metadata.
 * Metadata is what lets you filter later ("only billing documents") and show sources.
 */
@Component
public class KnowledgeCatalog {

    public record Entry(String file, String docType, String productArea, String version, String title) {

        Map<String, Object> metadata() {
            return Map.of("source", this.file, "doc_type", this.docType, "product_area", this.productArea,
                    "version", this.version, "doc_title", this.title);
        }

    }

    private final Path directory;

    private final Path catalogFile;

    public KnowledgeCatalog(@Value("${supportpilot.knowledge.dir:kb}") String directory,
                            @Value("${supportpilot.knowledge.catalog:kb/catalog.csv}") String catalogFile) {
        this.directory = Path.of(directory);
        this.catalogFile = Path.of(catalogFile);
    }

    public Path directory() {
        return this.directory;
    }

    public List<Entry> entries() {
        try {
            List<String> lines = Files.readAllLines(this.catalogFile, StandardCharsets.UTF_8);
            List<Entry> entries = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {          // skip the header
                String line = lines.get(i).strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] f = line.split(",", -1);
                if (f.length != 5) {
                    throw new IllegalStateException(this.catalogFile + " line " + (i + 1) + ": expected 5 fields");
                }
                entries.add(new Entry(f[0].strip(), f[1].strip(), f[2].strip(), f[3].strip(), f[4].strip()));
            }
            return entries;
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Cannot read " + this.catalogFile, ex);
        }
    }

    public Entry entry(String file) {
        return entries().stream()
                .filter(e -> e.file().equals(file))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Not in the catalog: " + file));
    }

}
