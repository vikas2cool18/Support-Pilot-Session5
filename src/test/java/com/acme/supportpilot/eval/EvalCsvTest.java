package com.acme.supportpilot.eval;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvalCsvTest {

    @Test
    void handlesQuotedCommasEscapedQuotesAndEmptyFields() {
        List<Map<String, String>> rows = EvalCsv.parse("""
                id,question,notes
                G1,"Hi, can I ""export"" contacts?",
                G2,plain,note
                """);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("question", "Hi, can I \"export\" contacts?").containsEntry("notes", "");
        assertThat(rows.get(1)).containsEntry("notes", "note");
    }

    @Test
    void rejectsRowsWithTheWrongNumberOfFields() {
        assertThatThrownBy(() -> EvalCsv.parse("a,b\n1,2,3\n")).isInstanceOf(IllegalArgumentException.class);
    }

}
