package com.acme.supportpilot.knowledge;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Checkpoint C: trigger ingestion. In production this would be protected and run as a background job. */
@RestController
@RequestMapping("/admin/ingest")
public class IngestionController {

    private final IngestionService ingestion;

    public IngestionController(IngestionService ingestion) {
        this.ingestion = ingestion;
    }

    @PostMapping
    public IngestionReport ingestAll() {
        return this.ingestion.ingestAll();
    }

    @PostMapping("/{file}")
    public IngestionReport ingestOne(@PathVariable String file) {
        return this.ingestion.ingestOne(file);
    }

}
