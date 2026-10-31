package com.acme.supportpilot.eval;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who judges the judge? src/test/resources/eval/calibration-v1.csv holds fixed answers - good,
 * wrong, incomplete, padded - scored by humans. The judge scores the same answers; if it does not
 * agree with the humans often enough, its scores cannot be trusted as a quality gate.
 * Add your own labelled rows: the more, the better the calibration.
 */
@EvalTest
class JudgeCalibrationEval {

    private static final Logger log = LoggerFactory.getLogger(JudgeCalibrationEval.class);

    @Autowired
    private ChatClient.Builder builder;

    @Test
    void judgeAgreesWithHumanLabels() throws IOException {
        LlmJudge judge = LlmJudge.fromClasspath(this.builder, EvalConfig.judgeModel());
        List<int[]> pairs = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        for (Map<String, String> row : EvalCsv.read(Path.of("src/test/resources/eval/calibration-v1.csv"))) {
            JudgeScore s = judge.judge(row.get("question"), row.get("expected_answer"), row.get("answer"), "[1] " + row.get("context"));
            int[] human = { Integer.parseInt(row.get("faithfulness")), Integer.parseInt(row.get("relevance")),
                    Integer.parseInt(row.get("correctness")) };
            int[] machine = { s.faithfulness(), s.relevance(), s.correctness() };
            for (int i = 0; i < 3; i++) {
                pairs.add(new int[] { machine[i], human[i] });
            }
            lines.add(String.format("  %s judge %d/%d/%d  human %d/%d/%d  %s", row.get("id"), machine[0], machine[1], machine[2],
                    human[0], human[1], human[2], s.reasoning().replaceAll("\\s+", " ")));
        }
        JudgeAgreement agreement = JudgeAgreement.of(pairs);
        log.info(String.format(Locale.ROOT, "%n===== Judge calibration: %d scores, exact %.2f, within one %.2f, bias %+.2f =====%n%s",
                agreement.pairs(), agreement.exact(), agreement.withinOne(), agreement.judgeBias(), String.join("\n", lines)));

        double min = Thresholds.load().get("judge-agreement-within-one").orElse(0.8);
        assertThat(agreement.withinOne()).as("share of judge scores within 1 point of the human label").isGreaterThanOrEqualTo(min);
    }

}
