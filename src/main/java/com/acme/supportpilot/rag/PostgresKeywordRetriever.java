package com.acme.supportpilot.rag;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Checkpoint D: the keyword leg of hybrid search, using PostgreSQL full-text search on the same
 * table that pgvector uses. No second database, no copy of the data.
 *
 * <p>The question is turned into an OR query of its words: "What does ERR-4471 mean?" becomes
 * {@code what or does or err-4471 or mean}. (Full-text search ANDs words by default, so a natural
 * question would rarely match.) Stop words like "what" are dropped by PostgreSQL itself.
 *
 * <p>Note: PostgreSQL's ts_rank_cd scores word frequency and proximity, not BM25 - it does not
 * know that "err-4471" is rarer than "error". For BM25, use a search engine (Elasticsearch,
 * OpenSearch) or a PostgreSQL extension such as pg_search or VectorChord-bm25.
 */
@Component("keywordRetriever")
public class PostgresKeywordRetriever implements DocumentRetriever {

    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}_.-]*");

    private static final Pattern TABLE = Pattern.compile("[a-z_][a-z0-9_]{0,62}");

    private final JdbcClient jdbc;

    private final String table;

    private final String language;

    private final int limit;

    public PostgresKeywordRetriever(JdbcTemplate jdbcTemplate,
                                    @Value("${spring.ai.vectorstore.pgvector.table-name}") String table,
                                    @Value("${supportpilot.rag.text-search-config:english}") String language,
                                    @Value("${supportpilot.rag.candidates:20}") int limit) {
        if (!TABLE.matcher(table).matches() || !TABLE.matcher(language).matches()) {
            throw new IllegalArgumentException("Invalid table name or text search configuration");
        }
        this.jdbc = JdbcClient.create(jdbcTemplate);
        this.table = table;
        this.language = language;
        this.limit = limit;
    }

    @Override
    public List<Document> retrieve(Query query) {
        String orQuery = toOrQuery(query.text());
        if (orQuery.isEmpty()) {
            return List.of();
        }
        KnowledgeFilter filter = KnowledgeFilter.from(query.context());
        // The expression to_tsvector('<language>', content) must match the GIN index exactly.
        String tsv = "to_tsvector('" + this.language + "', content)";
        StringBuilder sql = new StringBuilder("""
                SELECT id::text AS id, content,
                       metadata->>'source' AS source, metadata->>'doc_title' AS doc_title,
                       metadata->>'title' AS title, metadata->>'doc_type' AS doc_type,
                       metadata->>'product_area' AS product_area, metadata->>'page_number' AS page_number,
                       metadata->>'chunk_index' AS chunk_index,
                       ts_rank_cd(%s, q) AS rank
                FROM %s, websearch_to_tsquery('%s', :q) AS q
                WHERE %s @@ q
                """.formatted(tsv, this.table, this.language, tsv));
        Map<String, Object> params = new HashMap<>();
        params.put("q", orQuery);
        // Same metadata filter as the vector leg (Checkpoint C) - otherwise the keyword leg
        // would bring back chunks the filter was meant to exclude.
        if (filter.docType() != null) {
            sql.append(" AND metadata->>'doc_type' = :docType");
            params.put("docType", filter.docType());
        }
        if (filter.area() != null) {
            sql.append(" AND metadata->>'product_area' = :area");
            params.put("area", filter.area());
        }
        sql.append(" ORDER BY rank DESC LIMIT :limit");
        params.put("limit", this.limit);
        return this.jdbc.sql(sql.toString()).params(params).query(PostgresKeywordRetriever::toDocument).list();
    }

    /** "What does ERR-4471 mean?" -> "what or does or err-4471 or mean" (max 32 words). */
    static String toOrQuery(String text) {
        List<String> words = new ArrayList<>();
        Matcher m = WORD.matcher(text.toLowerCase());
        while (m.find() && words.size() < 32) {
            String w = m.group().replaceAll("[.-]+$", "");
            if (!w.isEmpty() && !w.equals("or") && !w.equals("and")) {
                words.add(w);
            }
        }
        return String.join(" or ", words);
    }

    private static Document toDocument(ResultSet rs, int row) throws SQLException {
        Map<String, Object> metadata = new HashMap<>();
        for (String key : List.of("source", "doc_title", "title", "doc_type", "product_area", "page_number", "chunk_index")) {
            String value = rs.getString(key);
            if (value != null) {
                metadata.put(key, value);
            }
        }
        metadata.put("keyword_rank", rs.getDouble("rank"));
        return Document.builder()
                .id(rs.getString("id"))
                .text(rs.getString("content"))
                .metadata(metadata)
                .score(rs.getDouble("rank"))
                .build();
    }

}
