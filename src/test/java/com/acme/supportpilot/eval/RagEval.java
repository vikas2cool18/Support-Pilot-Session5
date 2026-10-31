package com.acme.supportpilot.eval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import com.acme.supportpilot.knowledge.IngestionService;
import com.acme.supportpilot.rag.GroundedAnswer;
import com.acme.supportpilot.rag.GroundedAssistant;
import com.acme.supportpilot.rag.KnowledgeFilter;
import com.acme.supportpilot.rag.RagRun;
import com.acme.supportpilot.rag.RagSettings;
import com.acme.supportpilot.rag.RagPrompts;

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

    @Autowired
    private ChatClient.Builder judgeBuilder;

    @Autowired
    private Environment env;

    private final List<CaseResult> results = Collections.synchronizedList(new ArrayList<>());

    private LlmJudge judge;

    @BeforeAll
    void setUp() throws IOException {
        // A fresh database in a container: load the knowledge base first.
        this.ingestion.ingestAll();
        this.judge = LlmJudge.fromClasspath(this.judgeBuilder, EvalConfig.judgeModel());
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

            // Checkpoint C: the judge grades answerable cases; unanswerable ones are fully covered
            // by the deterministic checks (refused, no citations).
            JudgeScore score = null;
            Boolean relevancy = null;
            Boolean factCheck = null;
            if (c.answerable() && EvalConfig.judge()) {
                score = this.judge.judge(c.question(), c.expectedAnswer(), a.answer(), RagPrompts.numbered(run.context()));
            }
            if (c.answerable() && EvalConfig.springEvaluators()) {
                // Spring AI's built-in evaluators: yes/no answers instead of a 1-5 rubric.
                EvaluationRequest request = new EvaluationRequest(c.question(), run.context(), a.answer());
                relevancy = new RelevancyEvaluator(this.judgeBuilder).evaluate(request).isPass();
                factCheck = FactCheckingEvaluator.builder(this.judgeBuilder).build().evaluate(request).isPass();
            }
            this.results.add(new CaseResult(c, a, checks, score, relevancy, factCheck, null));
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
        Thresholds thresholds = Thresholds.load();
        EvalReport report = EvalReport.of(this.results, runConfig());
        report.write(EvalConfig.outputDir(), thresholds);
        log.info("\n{}", report.summary(thresholds));
        // The quality gate: every metric must meet its threshold (src/test/resources/eval/thresholds.properties).
        assertThat(report.gateFailures(thresholds)).as("quality gate - see target/eval/eval-report.md").isEmpty();
    }

    /** What was evaluated - so two reports can be compared fairly. */
    private Map<String, String> runConfig() {
        Map<String, String> config = new LinkedHashMap<>();
        String provider = this.env.getProperty("spring.ai.model.chat", "?");
        config.put("chat model", provider + " / " + this.env.getProperty("spring.ai." + provider + ".chat.model", "?"));
        config.put("embedding model", this.env.getProperty("spring.ai.model.embedding", "?"));
        config.put("prompt version", this.env.getProperty("supportpilot.rag.prompt-version", "?"));
        config.put("pipeline", this.settings.defaults().toString());
        config.put("judge model", EvalConfig.judge() ? (EvalConfig.judgeModel().isBlank() ? "same as chat" : EvalConfig.judgeModel()) : "off");
        config.put("dataset", EvalConfig.dataset().getFileName() + " (" + this.results.size() + " cases)");
        return config;
    }

}
