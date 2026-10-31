package com.acme.supportpilot.eval;

import java.nio.file.Path;
import java.util.List;

/**
 * Settings for an evaluation run, passed as -D options to Maven, e.g.
 * <pre>
 * mvn -Peval verify -Dspring.profiles.active=local -Deval.sample=8
 * </pre>
 */
final class EvalConfig {

    private EvalConfig() {
    }

    /** -Deval.dataset=src/test/resources/eval/golden-v1.csv */
    static Path dataset() {
        return Path.of(System.getProperty("eval.dataset", GoldenCase.DEFAULT_DATASET.toString()));
    }

    /** -Deval.sample=8 runs only the first 8 cases: a quick smoke run. 0 = all. */
    static <T> List<T> sample(List<T> all) {
        int n = Integer.getInteger("eval.sample", 0);
        return (n > 0 && n < all.size()) ? all.subList(0, n) : all;
    }

    /** -Deval.judge=false skips the LLM judge (faster, cheaper, deterministic checks only). */
    static boolean judge() {
        return Boolean.parseBoolean(System.getProperty("eval.judge", "true"));
    }

    /** -Deval.judge-model=gpt-4.1 - ideally a different, stronger model than the one being judged. */
    static String judgeModel() {
        return System.getProperty("eval.judge-model", "");
    }

    /** -Deval.spring-evaluators=true also runs Spring AI's RelevancyEvaluator and FactCheckingEvaluator. */
    static boolean springEvaluators() {
        return Boolean.parseBoolean(System.getProperty("eval.spring-evaluators", "false"));
    }

    /** Where reports are written. */
    static Path outputDir() {
        return Path.of(System.getProperty("eval.output", "target/eval"));
    }

}
