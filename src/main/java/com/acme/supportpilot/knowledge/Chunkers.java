package com.acme.supportpilot.knowledge;

import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Checkpoint C (Transform): chunking settings come from configuration,
 * so you can experiment without changing code:
 *   supportpilot.knowledge.chunk-size: 512   (tokens per chunk)
 */
@Component
public class Chunkers {

    private final int chunkSize;

    public Chunkers(@Value("${supportpilot.knowledge.chunk-size:512}") int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public TextSplitter splitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(this.chunkSize)
                .build();
    }

    public String describe() {
        return "token/" + this.chunkSize;
    }

}
