package com.acme.supportpilot.knowledge;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.EncodingType;
import org.junit.jupiter.api.Test;

import org.springframework.ai.document.Document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlidingWindowSplitterTest {

    private static final String TEXT = IntStream.rangeClosed(1, 300)
            .mapToObj(i -> "word" + i)
            .collect(Collectors.joining(" "));

    @Test
    void neighbouringChunksShareTheOverlap() {
        List<Document> chunks = new SlidingWindowSplitter(100, 20).apply(List.of(new Document(TEXT)));

        assertThat(chunks).hasSizeGreaterThan(2);
        String first = chunks.get(0).getText();
        String second = chunks.get(1).getText();
        String tailOfFirst = first.substring(first.length() - 15);
        assertThat(second).contains(tailOfFirst.strip());
    }

    @Test
    void numberOfChunksFollowsSizeAndOverlap() {
        int tokens = Encodings.newLazyEncodingRegistry().getEncoding(EncodingType.CL100K_BASE).countTokens(TEXT);

        int noOverlap = new SlidingWindowSplitter(100, 0).apply(List.of(new Document(TEXT))).size();
        int withOverlap = new SlidingWindowSplitter(100, 20).apply(List.of(new Document(TEXT))).size();

        assertThat(noOverlap).isEqualTo((tokens + 99) / 100);          // ceil(tokens / 100)
        assertThat(withOverlap).isEqualTo((tokens - 20 + 79) / 80);     // windows advance 80 tokens
    }

    @Test
    void chunksKeepTheMetadataOfTheirSource() {
        Document source = new Document(TEXT, Map.of("source", "faq.md"));
        List<Document> chunks = new SlidingWindowSplitter(100, 20).apply(List.of(source));
        assertThat(chunks).allSatisfy(c -> {
            assertThat(c.getMetadata()).containsEntry("source", "faq.md");
            assertThat(c.getMetadata()).containsKey("chunk_index");
        });
    }

    @Test
    void overlapMustBeSmallerThanTheChunk() {
        assertThatThrownBy(() -> new SlidingWindowSplitter(100, 100)).isInstanceOf(IllegalArgumentException.class);
    }

}
