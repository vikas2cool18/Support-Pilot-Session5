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

    /** Checkpoint B: the share of cases that must pass every deterministic check. */
    static double minPassRate() {
        return Double.parseDouble(System.getProperty("eval.min-pass-rate", "0.8"));
    }

}
