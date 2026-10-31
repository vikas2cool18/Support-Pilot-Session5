package com.acme.supportpilot.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The SQL itself needs PostgreSQL (Session 5 adds Testcontainers); the query building does not. */
class PostgresKeywordRetrieverTest {

    @Test
    void turnsAQuestionIntoAnOrQueryAndKeepsErrorCodesWhole() {
        assertThat(PostgresKeywordRetriever.toOrQuery("What does ERR-4471 mean?"))
                .isEqualTo("what or does or err-4471 or mean");
    }

    @Test
    void dropsPunctuationAndOperatorWords() {
        assertThat(PostgresKeywordRetriever.toOrQuery("refund -- or \"annual\" plan!!")).isEqualTo("refund or annual or plan");
        assertThat(PostgresKeywordRetriever.toOrQuery("?!")).isEmpty();
    }

}
