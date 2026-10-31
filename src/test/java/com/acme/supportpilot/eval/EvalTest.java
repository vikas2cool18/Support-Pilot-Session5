package com.acme.supportpilot.eval;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Marks an evaluation test: it starts the whole application against real models and a
 * PostgreSQL + pgvector container. Evaluation tests cost money and minutes, so they are tagged
 * "eval" and run only with {@code mvn -Peval verify} - never in the normal {@code mvn test}.
 * All classes with this annotation share one Spring context and one database container.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("eval")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(EvalContainers.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public @interface EvalTest {
}
