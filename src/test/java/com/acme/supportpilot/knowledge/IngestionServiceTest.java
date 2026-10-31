package com.acme.supportpilot.knowledge;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checkpoints C and D, offline: the real knowledge base in kb/, a fake embedding model
 * and Spring AI's in-memory SimpleVectorStore instead of pgvector.
 */
class IngestionServiceTest {

    private SimpleVectorStore store;

    private IngestionService ingestion;

    @BeforeEach
    void setUp() {
        this.store = SimpleVectorStore.builder(new FakeEmbeddingModel()).build();
        this.ingestion = new IngestionService(new KnowledgeCatalog("kb", "kb/catalog.csv"), new DocumentLoader(),
                new Chunkers("token", 512, 64), this.store);
    }

    private List<Document> everything() {
        return this.store.similaritySearch(SearchRequest.builder()
                .query("acme cloud").topK(10_000).similarityThresholdAll().build());
    }

    @Test
    void everyFileInTheCatalogIsIngested() {
        IngestionReport report = this.ingestion.ingestAll();

        assertThat(report.files()).hasSize(8);
        assertThat(report.files()).allSatisfy(f -> assertThat(f.chunks()).isPositive());
        assertThat(everything()).hasSize(report.totalChunks());
    }

    @Test
    void reIngestingDoesNotCreateDuplicates() {
        int first = this.ingestion.ingestAll().totalChunks();
        this.ingestion.ingestAll();

        assertThat(everything()).hasSize(first);
    }

    @Test
    void everyChunkCarriesCatalogMetadata() {
        this.ingestion.ingestAll();

        assertThat(everything()).allSatisfy(d -> assertThat(d.getMetadata())
                .containsKeys("source", "doc_type", "product_area", "version", "doc_title", "chunk_index"));
        assertThat(everything()).anySatisfy(d -> assertThat(d.getMetadata())
                .containsEntry("source", "refund-policy.pdf").containsEntry("doc_type", "policy")
                .containsKey("page_number"));
    }

    @Test
    void eachChunkStartsWithAContextHeader() {
        this.ingestion.ingestAll();

        assertThat(everything()).anySatisfy(d ->
                assertThat(d.getText()).startsWith("Acme Cloud User Guide > Resetting your password"));
    }

    @Test
    void smallerChunksMeanMoreChunks() {
        int big = this.ingestion.ingestAll().totalChunks();
        IngestionService small = new IngestionService(new KnowledgeCatalog("kb", "kb/catalog.csv"),
                new DocumentLoader(), new Chunkers("token", 128, 0),
                SimpleVectorStore.builder(new FakeEmbeddingModel()).build());

        assertThat(small.ingestAll().totalChunks()).isGreaterThan(big);
    }

    @Test
    void searchFindsTheRefundPolicy() {
        this.ingestion.ingestAll();
        List<SearchHit> hits = new SearchController(this.store)
                .search("full refund within 30 days of the annual payment", 3, 0.0, null);

        assertThat(hits).extracting(SearchHit::source).contains("refund-policy.pdf");
        assertThat(hits.get(0).score()).isGreaterThanOrEqualTo(hits.get(hits.size() - 1).score());
    }

    @Test
    void metadataFilterRestrictsResults() {
        this.ingestion.ingestAll();
        List<SearchHit> hits = new SearchController(this.store).search("refund", 10, 0.0, "policy");

        assertThat(hits).isNotEmpty();
        assertThat(hits).extracting(SearchHit::source)
                .allMatch(s -> s.equals("refund-policy.pdf") || s.equals("security-and-data-residency.md"));
    }

}
