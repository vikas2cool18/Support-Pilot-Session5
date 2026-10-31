package com.acme.supportpilot.rag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

import static com.acme.supportpilot.rag.ReciprocalRankFusionTest.doc;
import static org.assertj.core.api.Assertions.assertThat;

class RrfDocumentJoinerTest {

    @Test
    void chunkFoundByEveryQueryVariantComesFirstAndDuplicatesAreRemoved() {
        Map<Query, List<List<Document>>> results = new LinkedHashMap<>();
        results.put(new Query("original"), List.of(List.of(doc("a"), doc("shared"))));
        results.put(new Query("variant 1"), List.of(List.of(doc("shared"), doc("b"))));
        results.put(new Query("variant 2"), List.of(List.of(doc("c"), doc("shared"))));

        List<Document> joined = new RrfDocumentJoiner(60).join(results);

        assertThat(joined).extracting(Document::getId).startsWith("shared").doesNotHaveDuplicates().hasSize(4);
    }

}
