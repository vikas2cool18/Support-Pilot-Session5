package com.acme.supportpilot.eval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import com.acme.supportpilot.knowledge.IngestionService;
import com.acme.supportpilot.rag.GroundedAnswer;
import com.acme.supportpilot.rag.GroundedAssistant;
import com.acme.supportpilot.rag.KnowledgeFilter;
import com.acme.supportpilot.rag.RagRun;
import com.acme.supportpilot.rag.RagSettings;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The evaluation suite for SupportPilot's RAG pipeline. Every golden case runs through the real
 * pipeline (real models, real pgvector in a container); results are collected, and the quality
 * gate in {@link #gate()} judges the whole run.
 *
 * <pre>
 * mvn -Peval verify -Dspring.profiles.active=local -Deval.sample=8      quick smoke run
 * mvn -Peval verify                                                     full run, OpenAI
 * </pre>
 *
 * A single case fails only when the pipeline throws. Answer quality is never asserted per case:
 * with non-deterministic models one flaky case would make the build flaky. The gate asserts
 * aggregates - pass rates and average scores - against thresholds.
 */
@EvalTest
class RagEval {

    private static final Logger log = LoggerFactory.getLogger(RagEval.class);

    @Autowired
    private GroundedAssistant assistant;

    @Autowired
    private RagSettings settings;

    @Autowired
    private IngestionService ingestion;

    private final List<CaseResult> results = Collections.synchronizedList(new ArrayList<>());

    @BeforeAll
    void setUp() throws IOException {
        // A fresh database in a container: load the knowledge base first.
        this.ingestion.ingestAll();
    }

    static Stream<GoldenCase> cases() throws IOException {
        return EvalConfig.sample(GoldenCase.load(EvalConfig.dataset())).stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void evaluate(GoldenCase c) {
        try {
            RagRun run = this.assistant.run(c.question(), this.settings.defaults(), KnowledgeFilter.NONE);
            GroundedAnswer a = run.answer();
            List<Check> checks = DeterministicChecks.run(c, a);
            this.results.add(new CaseResult(c, a, checks, null));
            log.info("{} -> {} | {}", c.id(), checks.stream().filter(x -> !x.passed()).map(Check::name).toList(),
                    a.answer().replaceAll("\\s+", " "));
        }
        catch (RuntimeException ex) {
            this.results.add(CaseResult.failed(c, ex));
            throw ex;
        }
    }

    @AfterAll
    void gate() throws IOException {
        long passed = this.results.stream().filter(CaseResult::passed).count();
        double passRate = this.results.isEmpty() ? 0 : (double) passed / this.results.size();
        log.info("\n===== RAG evaluation: {}/{} cases passed every check ({}%) =====\n{}", passed, this.results.size(),
                Math.round(passRate * 100), String.join("\n", this.results.stream()
                        .filter(r -> !r.passed())
                        .map(r -> "  " + r.goldenCase().id() + " failed " + r.checks().stream().filter(x -> !x.passed()).map(Check::name).toList())
                        .toList()));
        // The quality gate: fail the build if too few cases pass.
        assertThat(passRate).as("share of cases passing every deterministic check").isGreaterThanOrEqualTo(EvalConfig.minPassRate());
    }

}
