package com.acme.supportpilot.eval;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import com.acme.supportpilot.triage.LabelledTicket;
import com.acme.supportpilot.triage.TicketTriage;
import com.acme.supportpilot.triage.TriageResult;
import com.acme.supportpilot.triage.TriageService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checkpoint D: Session 2's triage accuracy, now part of the quality gate. A change to the triage
 * prompt or model can no longer lower accuracy without failing the build.
 */
@EvalTest
class TriageEval {

    private static final Logger log = LoggerFactory.getLogger(TriageEval.class);

    record Outcome(LabelledTicket expected, TicketTriage actual, boolean fallback) {

        boolean categoryOk() {
            return this.actual.category() == this.expected.category();
        }

        boolean priorityOk() {
            return this.actual.priority() == this.expected.priority();
        }

    }

    @Autowired
    private TriageService triageService;

    private final List<Outcome> outcomes = Collections.synchronizedList(new ArrayList<>());

    static Stream<Arguments> tickets() throws IOException {
        return EvalConfig.sample(LabelledTicket.readCsv(Path.of("data/tickets-labelled.csv"))).stream()
                .map(t -> Arguments.of(Named.of(t.id() + " " + t.category(), t)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tickets")
    void triage(LabelledTicket ticket) {
        TriageResult result = this.triageService.triage(ticket.ticket());
        this.outcomes.add(new Outcome(ticket, result.triage(), result.fallback()));
    }

    @AfterAll
    void gate() throws IOException {
        double category = share(Outcome::categoryOk);
        double priority = share(Outcome::priorityOk);
        double fallback = share(Outcome::fallback);
        List<String> lines = new ArrayList<>(List.of("# Triage evaluation", "",
                String.format(Locale.ROOT, "- category accuracy: %.2f", category),
                String.format(Locale.ROOT, "- priority accuracy: %.2f", priority),
                String.format(Locale.ROOT, "- fallbacks: %.2f", fallback), "", "## Mismatches", ""));
        this.outcomes.stream().filter(o -> !o.categoryOk() || !o.priorityOk()).forEach(o -> lines.add(String.format(
                "- %s expected %s/%s, got %s/%s%s", o.expected().id(), o.expected().category(), o.expected().priority(),
                o.actual().category(), o.actual().priority(), o.fallback() ? " (fallback)" : "")));
        Files.createDirectories(EvalConfig.outputDir());
        Files.write(EvalConfig.outputDir().resolve("triage-report.md"), lines, StandardCharsets.UTF_8);
        log.info("\n{}", String.join("\n", lines));

        double min = Thresholds.load().get("triage-category-accuracy").orElse(0.8);
        assertThat(category).as("triage category accuracy").isGreaterThanOrEqualTo(min);
    }

    private double share(java.util.function.Predicate<Outcome> ok) {
        return this.outcomes.isEmpty() ? 0 : (double) this.outcomes.stream().filter(ok).count() / this.outcomes.size();
    }

}
