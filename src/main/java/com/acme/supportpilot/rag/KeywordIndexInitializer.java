package com.acme.supportpilot.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Checkpoint D: creates a GIN full-text index on the vector store table, so keyword search does
 * not have to scan every row. Runs after start-up, when Spring AI has already created the table.
 * In production you would put this in a Flyway or Liquibase migration instead.
 */
@Component
class KeywordIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KeywordIndexInitializer.class);

    private final JdbcTemplate jdbc;

    private final String table;

    private final String language;

    KeywordIndexInitializer(JdbcTemplate jdbc,
                            @Value("${spring.ai.vectorstore.pgvector.table-name}") String table,
                            @Value("${supportpilot.rag.text-search-config:english}") String language) {
        this.jdbc = jdbc;
        this.table = table;
        this.language = language;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!this.table.matches("[a-z_][a-z0-9_]{0,50}") || !this.language.matches("[a-z_]{1,30}")) {
            log.warn("Skipping full-text index: unexpected table name or language");
            return;
        }
        String sql = "CREATE INDEX IF NOT EXISTS %s_content_fts ON %s USING GIN (to_tsvector('%s', content))"
                .formatted(this.table, this.table, this.language);
        try {
            this.jdbc.execute(sql);
            log.info("Full-text index ready on {}", this.table);
        }
        catch (RuntimeException ex) {
            log.warn("Could not create the full-text index on {}: {}", this.table, ex.getMessage());
        }
    }

}
