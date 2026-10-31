package com.acme.supportpilot.eval;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * One question of the golden dataset: what a customer asks, what a good answer says, and which
 * documents it must come from. {@code expectedSources = [none]} marks a question the knowledge
 * base cannot answer - the assistant must say so.
 */
public record GoldenCase(String id, String type, String question, String expectedAnswer,
                         List<String> expectedSources, String notes) {

    public static final List<String> TYPES = List.of("simple", "multi-hop", "unanswerable");

    /** The default dataset; a new version gets a new file (golden-v2.csv) so results stay comparable. */
    public static final Path DEFAULT_DATASET = Path.of("src/test/resources/eval/golden-v1.csv");

    public boolean answerable() {
        return !"unanswerable".equals(this.type);
    }

    public static List<GoldenCase> load(Path file) throws IOException {
        return EvalCsv.read(file).stream().map(GoldenCase::from).toList();
    }

    static GoldenCase from(Map<String, String> row) {
        return new GoldenCase(row.get("id"), row.get("type"), row.get("question"), row.get("expected_answer"),
                Arrays.stream(row.get("expected_sources").split("\\|")).map(String::strip).toList(),
                row.getOrDefault("notes", ""));
    }

    /** JUnit shows this in the test name, e.g. "G04 simple: typed my password wrong ..." */
    @Override
    public String toString() {
        String q = this.question.length() <= 60 ? this.question : this.question.substring(0, 57) + "...";
        return this.id + " " + this.type + ": " + q;
    }

}
