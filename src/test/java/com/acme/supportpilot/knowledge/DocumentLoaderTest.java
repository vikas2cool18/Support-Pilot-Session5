package com.acme.supportpilot.knowledge;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.ai.document.Document;

import static org.assertj.core.api.Assertions.assertThat;

/** Checkpoint C: each reader keeps a different kind of structure. */
class DocumentLoaderTest {

    private final DocumentLoader loader = new DocumentLoader();

    @Test
    void pdfBecomesOneDocumentPerPageWithPageNumbers() {
        List<Document> pages = this.loader.load(Path.of("kb/refund-policy.pdf"));

        assertThat(pages).hasSize(2);
        assertThat(pages.get(0).getMetadata()).containsEntry("page_number", 1);
        assertThat(pages.get(0).getText()).contains("30 days");
    }

    @Test
    void markdownBecomesOneDocumentPerSectionWithTitles() {
        List<Document> sections = this.loader.load(Path.of("kb/acme-cloud-user-guide.md"));

        assertThat(sections).hasSizeGreaterThan(10);
        assertThat(sections).anySatisfy(d -> assertThat(d.getMetadata()).containsEntry("title", "Resetting your password"));
    }

    @Test
    void wordDocumentIsReadWithTika() {
        List<Document> docs = this.loader.load(Path.of("kb/troubleshooting-guide.docx"));

        assertThat(docs).isNotEmpty();
        assertThat(docs.get(0).getText()).contains("Error 503");
    }

}
