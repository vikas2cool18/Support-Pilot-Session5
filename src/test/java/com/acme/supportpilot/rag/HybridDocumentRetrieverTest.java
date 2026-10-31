package com.acme.supportpilot.rag;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

import static com.acme.supportpilot.rag.ReciprocalRankFusionTest.doc;
import static org.assertj.core.api.Assertions.assertThat;

class HybridDocumentRetrieverTest {

    @Test
    void fusesBothLegsAndTagsWhichLegFoundEachChunk() {
        HybridDocumentRetriever hybrid = new HybridDocumentRetriever(
                query -> List.of(doc("a"), doc("b")),
                query -> List.of(doc("err-4471"), doc("b")),
                60, 10);

        List<Document> result = hybrid.retrieve(new Query("ERR-4471"));

        assertThat(result).extracting(Document::getId).containsExactly("b", "a", "err-4471");
        assertThat(result).extracting(d -> d.getMetadata().get("matched_by")).containsExactly("both", "vector", "keyword");
    }

    @Test
    void respectsTheLimit() {
        HybridDocumentRetriever hybrid = new HybridDocumentRetriever(
                query -> List.of(doc("a"), doc("b"), doc("c")), query -> List.of(doc("d")), 60, 2);

        assertThat(hybrid.retrieve(new Query("q"))).hasSize(2);
    }

}
