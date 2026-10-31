package com.acme.supportpilot.eval;

import java.util.List;

import com.acme.supportpilot.rag.GroundedAnswer;

/**
 * Everything recorded for one golden case. {@code answer} is null when the pipeline threw.
 */
public record CaseResult(
        GoldenCase goldenCase,
        GroundedAnswer answer,
        List<Check> checks,
        JudgeScore judge,
        Boolean springRelevancy,
        Boolean springFactCheck,
        String error) {

    static CaseResult failed(GoldenCase c, Exception ex) {
        return new CaseResult(c, null, List.of(new Check("no-error", false, ex.getClass().getSimpleName() + ": " + ex.getMessage())),
                null, null, null,
                ex.getMessage());
    }

    /** True if every deterministic check passed. */
    public boolean passed() {
        return this.checks.stream().allMatch(Check::passed);
    }

    public boolean checkPassed(String name) {
        return this.checks.stream().filter(c -> c.name().equals(name)).allMatch(Check::passed);
    }

    public boolean hasCheck(String name) {
        return this.checks.stream().anyMatch(c -> c.name().equals(name));
    }

    /** Average of the three judge scores, or NaN if the case was not judged. */
    public double judgeMean() {
        return (this.judge == null) ? Double.NaN
                : (this.judge.faithfulness() + this.judge.relevance() + this.judge.correctness()) / 3.0;
    }

}
