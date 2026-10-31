package com.acme.supportpilot.rag;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.acme.supportpilot.knowledge.FakeEmbeddingModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checkpoint E: hermetic tests for the SQL that Session 4 could only test by hand. A real
 * PostgreSQL + pgvector runs in a container; the embedding model is the offline fake, so no API
 * key and no Ollama are needed - only Docker. Run with {@code mvn -Pdocker test}.
 */
@Tag("docker")
class PostgresRetrievalIT {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));

    private static final String TABLE = "vector_store_it";

    private static JdbcTemplate jdbc;

    private static PgVectorStore store;

    @BeforeAll
    static void start() {
        POSTGRES.start();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        store = PgVectorStore.builder(jdbc, new FakeEmbeddingModel())
                .vectorTableName(TABLE)
                .dimensions(256)
                .initializeSchema(true)
                .build();
        store.afterPropertiesSet();
        store.add(List.of(
                new Document("API v2 Error Codes > ERR-4471 - API token expired\n\nThe API token has passed its expiry date.",
                        Map.of("source", "api-error-codes.md", "doc_type", "reference", "product_area", "api")),
                new Document("API v2 Error Codes > ERR-4290 - Rate limit exceeded\n\nToo many requests.",
                        Map.of("source", "api-error-codes.md", "doc_type", "reference", "product_area", "api")),
                new Document("Refund and Billing Policy\n\nPRO customers get a full refund within 30 days. An error on an invoice is corrected.",
                        Map.of("source", "refund-policy.pdf", "doc_type", "policy", "product_area", "billing"))));
        new KeywordIndexInitializer(jdbc, TABLE, "english").run(null);
    }

    @AfterAll
    static void stop() {
        POSTGRES.stop();
    }

    private static PostgresKeywordRetriever keyword() {
        return new PostgresKeywordRetriever(jdbc, TABLE, "english", 20);
    }

    @Test
    void fullTextIndexIsCreated() {
        List<String> indexes = jdbc.queryForList("select indexname from pg_indexes where tablename = ?", String.class, TABLE);

        assertThat(indexes).contains(TABLE + "_content_fts");
    }

    @Test
    void keywordSearchFindsTheExactErrorCodeFirst() {
        List<Document> hits = keyword().retrieve(new Query("What does ERR-4471 mean?"));

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).getText()).contains("ERR-4471");
        assertThat(hits.get(0).getMetadata()).containsEntry("source", "api-error-codes.md").containsKey("keyword_rank");
    }

    @Test
    void keywordSearchAppliesTheMetadataFilter() {
        Map<String, Object> context = new java.util.HashMap<>();
        new KnowledgeFilter("policy", null).putInto(context);

        List<Document> hits = keyword().retrieve(Query.builder().text("error").context(context).build());

        assertThat(hits).extracting(d -> d.getMetadata().get("source")).containsOnly("refund-policy.pdf");
    }

    @Test
    void hybridSearchKeepsTheKeywordMatchAndIdsMatchAcrossLegs() {
        HybridDocumentRetriever hybrid = new HybridDocumentRetriever(
                query -> store.similaritySearch(org.springframework.ai.vectorstore.SearchRequest.builder().query(query.text()).topK(3).build()),
                keyword(), 60, 3);

        List<Document> hits = hybrid.retrieve(new Query("ERR-4471"));

        assertThat(hits.get(0).getText()).contains("ERR-4471");
        assertThat(hits.get(0).getMetadata()).containsEntry("matched_by", "both");
    }

}
