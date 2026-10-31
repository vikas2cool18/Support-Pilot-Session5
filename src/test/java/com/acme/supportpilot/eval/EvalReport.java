package com.acme.supportpilot.eval;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

import com.acme.supportpilot.rag.CitedSource;

/**
 * Checkpoint D: turns the case results of one run into metrics, a Markdown report for humans,
 * a CSV for history, and the list of gate failures that decides whether the build passes.
 */
public final class EvalReport {

    /** Metric name (as in thresholds.properties) to value. NaN means "not measured in this run". */
    private final Map<String, Double> metrics = new LinkedHashMap<>();

    private final List<CaseResult> results;

    private final Map<String, String> config;

    private EvalReport(List<CaseResult> results, Map<String, String> config) {
        this.results = List.copyOf(results);
        this.config = config;
        this.metrics.put("pass-rate", rate(this.results, r -> true, CaseResult::passed));
        this.metrics.put("answerable-accuracy", rate(this.results, r -> r.hasCheck("answerable"), r -> r.checkPassed("answerable")));
        this.metrics.put("retrieval-hit-rate", rate(this.results, r -> r.hasCheck("retrieval-hit"), r -> r.checkPassed("retrieval-hit")));
        List<CitedSource> citations = this.results.stream().filter(r -> r.answer() != null)
                .flatMap(r -> r.answer().citations().stream()).toList();
        this.metrics.put("citation-verified-rate", citations.isEmpty() ? Double.NaN
                : (double) citations.stream().filter(CitedSource::verified).count() / citations.size());
        this.metrics.put("faithfulness", average(this.results, JudgeScore::faithfulness));
        this.metrics.put("relevance", average(this.results, JudgeScore::relevance));
        this.metrics.put("correctness", average(this.results, JudgeScore::correctness));
    }

    public static EvalReport of(List<CaseResult> results, Map<String, String> config) {
        return new EvalReport(results, config);
    }

    public Map<String, Double> metrics() {
        return this.metrics;
    }

    static double rate(List<CaseResult> rs, Predicate<CaseResult> applies, Predicate<CaseResult> ok) {
        List<CaseResult> relevant = rs.stream().filter(applies).toList();
        return relevant.isEmpty() ? Double.NaN : (double) relevant.stream().filter(ok).count() / relevant.size();
    }

    static double average(List<CaseResult> rs, ToDoubleFunction<JudgeScore> score) {
        OptionalDouble avg = rs.stream().filter(r -> r.judge() != null).mapToDouble(r -> score.applyAsDouble(r.judge())).average();
        return avg.isPresent() ? avg.getAsDouble() : Double.NaN;
    }

    /** One message per metric below its threshold. Metrics not measured in this run are skipped. */
    public List<String> gateFailures(Thresholds thresholds) {
        List<String> failures = new ArrayList<>();
        this.metrics.forEach((name, value) -> thresholds.get(name).ifPresent(min -> {
            if (!Double.isNaN(value) && value < min) {
                failures.add(String.format(Locale.ROOT, "%s = %.2f is below %.2f", name, value, min));
            }
        }));
        return failures;
    }

    /** The cases to look at first: failed checks first, then the lowest judge scores. */
    public List<CaseResult> worst(int n) {
        Comparator<CaseResult> byFailures = Comparator.comparingLong(r -> -r.checks().stream().filter(c -> !c.passed()).count());
        Comparator<CaseResult> byJudge = Comparator.comparingDouble(r -> Double.isNaN(r.judgeMean()) ? 6 : r.judgeMean());
        return this.results.stream()
                .filter(r -> !r.passed() || (!Double.isNaN(r.judgeMean()) && r.judgeMean() < 5))
                .sorted(byFailures.thenComparing(byJudge)).limit(n).toList();
    }

    public long latencyPercentile(double p) {
        List<Long> ms = this.results.stream().filter(r -> r.answer() != null).map(r -> r.answer().latencyMs()).sorted().toList();
        return ms.isEmpty() ? 0 : ms.get((int) Math.min(ms.size() - 1, Math.ceil(p * ms.size()) - 1));
    }

    public String summary(Thresholds thresholds) {
        StringBuilder sb = new StringBuilder("===== RAG evaluation: " + this.results.size() + " cases =====\n");
        this.metrics.forEach((name, value) -> sb.append(String.format(Locale.ROOT, "  %-24s %6s   (min %s)%n", name,
                fmt(value), thresholds.get(name).isPresent() ? fmt(thresholds.get(name).getAsDouble()) : "-")));
        sb.append(String.format(Locale.ROOT, "  latency p50 / p95        %d / %d ms%n", latencyPercentile(0.5), latencyPercentile(0.95)));
        List<String> failures = gateFailures(thresholds);
        sb.append(failures.isEmpty() ? "  GATE: PASSED" : "  GATE: FAILED - " + String.join("; ", failures));
        return sb.toString();
    }

    public String markdown(Thresholds thresholds) {
        StringBuilder md = new StringBuilder("# SupportPilot evaluation report\n\n");
        this.config.forEach((k, v) -> md.append("- **").append(k).append(":** ").append(v).append('\n'));
        md.append("\n## Quality gate\n\n| Metric | Value | Threshold | Status |\n|---|---|---|---|\n");
        this.metrics.forEach((name, value) -> {
            OptionalDouble min = thresholds.get(name);
            String status = Double.isNaN(value) ? "not measured" : (min.isPresent() && value < min.getAsDouble()) ? "**FAIL**" : "pass";
            md.append("| ").append(name).append(" | ").append(fmt(value)).append(" | ")
                    .append(min.isPresent() ? fmt(min.getAsDouble()) : "-").append(" | ").append(status).append(" |\n");
        });
        md.append(String.format(Locale.ROOT, "%nLatency p50 %d ms, p95 %d ms.%n", latencyPercentile(0.5), latencyPercentile(0.95)));
        md.append("\n## By question type\n\n| Type | Cases | Pass rate | Faithfulness | Correctness |\n|---|---|---|---|---|\n");
        for (String type : GoldenCase.TYPES) {
            List<CaseResult> rs = this.results.stream().filter(r -> r.goldenCase().type().equals(type)).toList();
            if (!rs.isEmpty()) {
                md.append("| ").append(type).append(" | ").append(rs.size()).append(" | ").append(fmt(rate(rs, r -> true, CaseResult::passed)))
                        .append(" | ").append(fmt(average(rs, JudgeScore::faithfulness))).append(" | ")
                        .append(fmt(average(rs, JudgeScore::correctness))).append(" |\n");
            }
        }
        md.append("\n## Worst cases\n\n");
        for (CaseResult r : worst(5)) {
            md.append("### ").append(r.goldenCase()).append("\n\n");
            r.checks().stream().filter(c -> !c.passed())
                    .forEach(c -> md.append("- failed **").append(c.name()).append("**: ").append(c.detail()).append('\n'));
            if (r.judge() != null) {
                md.append(String.format("- judge: faithfulness %d, relevance %d, correctness %d - %s%n",
                        r.judge().faithfulness(), r.judge().relevance(), r.judge().correctness(), r.judge().reasoning()));
            }
            md.append("- expected: ").append(r.goldenCase().expectedAnswer()).append('\n');
            md.append("- answer: ").append(r.answer() == null ? "(error) " + r.error() : r.answer().answer().replaceAll("\\s+", " ")).append("\n\n");
        }
        return md.toString();
    }

    /** One row per case, for comparing runs over time. */
    public List<String> csv() {
        List<String> rows = new ArrayList<>();
        rows.add("id,type,passed,failed_checks,faithfulness,relevance,correctness,latency_ms,prompt_tokens");
        for (CaseResult r : this.results) {
            JudgeScore j = r.judge();
            rows.add(String.join(",", r.goldenCase().id(), r.goldenCase().type(), String.valueOf(r.passed()),
                    String.join("|", r.checks().stream().filter(c -> !c.passed()).map(Check::name).toList()),
                    j == null ? "" : String.valueOf(j.faithfulness()), j == null ? "" : String.valueOf(j.relevance()),
                    j == null ? "" : String.valueOf(j.correctness()),
                    r.answer() == null ? "" : String.valueOf(r.answer().latencyMs()),
                    r.answer() == null ? "" : String.valueOf(r.answer().promptTokens())));
        }
        return rows;
    }

    public void write(Path dir, Thresholds thresholds) throws IOException {
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("eval-report.md"), markdown(thresholds), StandardCharsets.UTF_8);
        Files.write(dir.resolve("eval-results.csv"), csv(), StandardCharsets.UTF_8);
    }

    private static String fmt(double v) {
        return Double.isNaN(v) ? "-" : String.format(Locale.ROOT, "%.2f", v);
    }

}
