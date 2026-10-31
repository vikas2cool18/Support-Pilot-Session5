package com.acme.supportpilot.eval;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checkpoint A: test your test data. A typo in a source file name would make every retrieval
 * check fail and look like a model problem. These tests run in the normal, offline `mvn test`.
 */
class GoldenDatasetTest {

    private final List<GoldenCase> cases = GoldenCase.load(GoldenCase.DEFAULT_DATASET);

    GoldenDatasetTest() throws IOException {
    }

    @Test
    void hasUniqueIdsKnownTypesAndEnoughHardCases() {
        assertThat(this.cases).hasSizeGreaterThanOrEqualTo(25);
        assertThat(this.cases).extracting(GoldenCase::id).doesNotHaveDuplicates();
        assertThat(this.cases).extracting(GoldenCase::type).allMatch(GoldenCase.TYPES::contains);
        Map<String, Long> perType = this.cases.stream().collect(Collectors.groupingBy(GoldenCase::type, Collectors.counting()));
        assertThat(perType.get("multi-hop")).isGreaterThanOrEqualTo(5);
        assertThat(perType.get("unanswerable")).isGreaterThanOrEqualTo(5);
    }

    @Test
    void everyExpectedSourceExistsInTheKnowledgeBase() throws IOException {
        Set<String> files = EvalCsv.read(Path.of("kb/catalog.csv")).stream().map(r -> r.get("file")).collect(Collectors.toSet());
        for (GoldenCase c : this.cases) {
            if (c.answerable()) {
                assertThat(files).as(c.id()).containsAll(c.expectedSources());
            }
            else {
                assertThat(c.expectedSources()).as(c.id()).containsExactly("none");
            }
        }
    }

    @Test
    void everyQuestionHasAnExpectedAnswer() {
        assertThat(this.cases).allSatisfy(c -> {
            assertThat(c.question()).as(c.id()).isNotBlank();
            assertThat(c.expectedAnswer()).as(c.id()).isNotBlank();
        });
    }

}
