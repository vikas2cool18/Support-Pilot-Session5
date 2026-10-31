package com.acme.supportpilot.eval;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;

/**
 * A fresh PostgreSQL + pgvector database for every evaluation run, started by Testcontainers.
 * {@code @ServiceConnection} points the application's DataSource at it, so docker compose is not
 * needed and the results never depend on what happens to be in your local database.
 */
@TestConfiguration(proxyBeanMethods = false)
public class EvalContainers {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));
    }

}
