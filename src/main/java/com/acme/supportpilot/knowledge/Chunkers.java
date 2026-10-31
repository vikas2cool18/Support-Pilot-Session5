package com.acme.supportpilot.knowledge;

import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Checkpoint C (Transform): chunking settings come from configuration,
 * so you can experiment without changing code:
 *   supportpilot.knowledge.chunking:   token | window
 *   supportpilot.knowledge.chunk-size: 512   (tokens per chunk)
 *   supportpilot.knowledge.overlap:    64    (tokens shared by neighbouring chunks; window only)
 */
@Component
public class Chunkers {

    private final String strategy;

    private final int chunkSize;

    private final int overlap;

    public Chunkers(@Value("${supportpilot.knowledge.chunking:token}") String strategy,
                    @Value("${supportpilot.knowledge.chunk-size:512}") int chunkSize,
                    @Value("${supportpilot.knowledge.overlap:64}") int overlap) {
        this.strategy = strategy;
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    public TextSplitter splitter() {
        if ("window".equals(this.strategy)) {
            return new SlidingWindowSplitter(this.chunkSize, this.overlap);
        }
        return TokenTextSplitter.builder()
                .withChunkSize(this.chunkSize)
                .build();
    }

    public String describe() {
        return "window".equals(this.strategy)
                ? "window/" + this.chunkSize + "/" + this.overlap
                : "token/" + this.chunkSize;
    }

}
