package com.acme.supportpilot.knowledge;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeCatalogTest {

    private final KnowledgeCatalog catalog = new KnowledgeCatalog("kb", "kb/catalog.csv");

    @Test
    void everyCatalogEntryPointsToAnExistingFile() {
        assertThat(this.catalog.entries()).hasSize(8)
                .allSatisfy(e -> assertThat(Files.exists(Path.of("kb", e.file()))).isTrue());
    }

    @Test
    void ragQuestionsReferenceCatalogFiles() throws Exception {
        var files = this.catalog.entries().stream().map(KnowledgeCatalog.Entry::file).toList();
        assertThat(RagQuestion.readCsv(Path.of("data/rag-questions.csv"))).hasSize(10)
                .allSatisfy(q -> assertThat(files).containsAll(q.expectedSources()));
    }

}
