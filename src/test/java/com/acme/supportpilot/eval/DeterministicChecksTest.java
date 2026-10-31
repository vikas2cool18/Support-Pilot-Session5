package com.acme.supportpilot.eval;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.acme.supportpilot.knowledge.SearchHit;
import com.acme.supportpilot.rag.CitedSource;
import com.acme.supportpilot.rag.GroundedAnswer;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicChecksTest {

    static final GoldenCase REFUND = new GoldenCase("G10", "simple", "How long for a refund?", "30 days",
            List.of("refund-policy.pdf"), "");

    static final GoldenCase ON_PREM = new GoldenCase("G03", "unanswerable", "On-premise price?", "unknown",
            List.of("none"), "");

    static GroundedAnswer answer(boolean answerable, List<CitedSource> citations, String... sources) {
        List<SearchHit> hits = java.util.Arrays.stream(sources).map(s -> new SearchHit(0.5, s, "", null, 0, "...")).toList();
        return new GroundedAnswer("Within 30 days [1].", answerable, citations, hits, List.of(), 100, 20, 1200);
    }

    static CitedSource cite(String source, boolean verified) {
        return new CitedSource(1, source, null, "", "quote", verified);
    }

    @Test
    void goodAnswerPassesEveryCheck() {
        List<Check> checks = DeterministicChecks.run(REFUND, answer(true, List.of(cite("refund-policy.pdf", true)), "refund-policy.pdf", "faq.md"));

        assertThat(checks).allMatch(Check::passed);
        assertThat(checks).extracting(Check::name).contains("retrieval-hit", "citations-verified", "cites-expected-source");
    }

    @Test
    void unverifiedQuoteAndWrongSourceAreCaught() {
        List<Check> checks = DeterministicChecks.run(REFUND, answer(true, List.of(cite("faq.md", false)), "faq.md"));

        assertThat(checks).filteredOn(c -> !c.passed()).extracting(Check::name)
                .containsExactlyInAnyOrder("retrieval-hit", "citations-verified", "cites-expected-source");
    }

    @Test
    void unanswerableQuestionMustBeRefusedWithoutCitations() {
        assertThat(DeterministicChecks.run(ON_PREM, answer(false, List.of())))
                .allMatch(Check::passed);
        assertThat(DeterministicChecks.run(ON_PREM, answer(true, List.of(cite("plans-and-pricing.pdf", true)), "plans-and-pricing.pdf")))
                .filteredOn(c -> !c.passed()).extracting(Check::name).containsExactlyInAnyOrder("answerable", "no-citations");
    }

}
