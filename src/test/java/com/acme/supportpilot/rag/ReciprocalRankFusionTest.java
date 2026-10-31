package com.acme.supportpilot.rag;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.ai.document.Document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ReciprocalRankFusionTest {

    static Document doc(String id) {
        return Document.builder().id(id).text("text " + id).metadata(Map.of("source", id + ".md")).score(0.5).build();
    }

    @Test
    void documentHighInBothListsBeatsOneThatIsFirstInOnlyOne() {
        List<Document> vector = List.of(doc("a"), doc("b"), doc("c"));
        List<Document> keyword = List.of(doc("d"), doc("b"));

        List<Document> fused = ReciprocalRankFusion.fuse(List.of(vector, keyword), 60);

        assertThat(fused).extracting(Document::getId).containsExactly("b", "a", "d", "c");
        // b: 1/62 + 1/62 ; a: 1/61 ; d: 1/61 (tie keeps first-seen order) ; c: 1/63
        assertThat(fused.get(0).getScore()).isCloseTo(2.0 / 62, within(1e-9));
        assertThat(fused.get(0).getMetadata()).containsKey("rrf_score");
    }

    @Test
    void ignoresRawScoresAndUsesOnlyRanks() {
        Document high = Document.builder().id("x").text("x").score(0.99).build();
        Document low = Document.builder().id("y").text("y").score(0.01).build();

        List<Document> fused = ReciprocalRankFusion.fuse(List.of(List.of(low, high)), 60);

        assertThat(fused).extracting(Document::getId).containsExactly("y", "x");
    }

}
