package com.acme.supportpilot.eval;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.acme.supportpilot.rag.GroundedAnswer;

import static com.acme.supportpilot.eval.DeterministicChecksTest.ON_PREM;
import static com.acme.supportpilot.eval.DeterministicChecksTest.REFUND;
import static com.acme.supportpilot.eval.DeterministicChecksTest.answer;
import static com.acme.supportpilot.eval.DeterministicChecksTest.cite;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EvalReportTest {

    private static CaseResult result(GoldenCase c, GroundedAnswer a, JudgeScore j) {
        return new CaseResult(c, a, DeterministicChecks.run(c, a), j, null, null, null);
    }

    private final List<CaseResult> results = List.of(
            result(REFUND, answer(true, List.of(cite("refund-policy.pdf", true)), "refund-policy.pdf"), new JudgeScore("ok", 5, 5, 5)),
            result(REFUND, answer(true, List.of(cite("faq.md", false)), "faq.md"), new JudgeScore("weak", 2, 4, 2)),
            result(ON_PREM, answer(false, List.of()), null));

    @Test
    void computesRatesAndAveragesOnlyOverCasesTheyApplyTo() {
        Map<String, Double> m = EvalReport.of(this.results, Map.of()).metrics();

        assertThat(m.get("pass-rate")).isCloseTo(2.0 / 3, within(1e-9));
        assertThat(m.get("answerable-accuracy")).isEqualTo(1.0);
        assertThat(m.get("retrieval-hit-rate")).isEqualTo(0.5);           // only the two answerable cases
        assertThat(m.get("citation-verified-rate")).isEqualTo(0.5);
        assertThat(m.get("faithfulness")).isEqualTo(3.5);                 // the unanswerable case was not judged
    }

    @Test
    void gateReportsEveryMetricBelowItsThresholdAndSkipsUnmeasuredOnes() {
        Thresholds t = new Thresholds(Map.of("faithfulness", 4.0, "answerable-accuracy", 0.9, "triage-category-accuracy", 0.8));

        List<String> failures = EvalReport.of(this.results, Map.of()).gateFailures(t);

        assertThat(failures).singleElement().asString().startsWith("faithfulness = 3.50");
    }

    @Test
    void worstCasesComeFirstInTheMarkdownReport() {
        String md = EvalReport.of(this.results, Map.of("prompt version", "v2")).markdown(new Thresholds(Map.of()));

        assertThat(md).contains("**prompt version:** v2").contains("## Worst cases").contains("failed **citations-verified**");
        assertThat(md.indexOf("weak")).isPositive();
    }

}
