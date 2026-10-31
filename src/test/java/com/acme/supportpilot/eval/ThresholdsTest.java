package com.acme.supportpilot.eval;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThresholdsTest {

    @AfterEach
    void clear() {
        System.clearProperty("eval.threshold.faithfulness");
    }

    @Test
    void loadsTheFileAndAllowsOverridesPerRun() throws IOException {
        assertThat(Thresholds.load().get("faithfulness")).hasValue(4.0);

        System.setProperty("eval.threshold.faithfulness", "3.5");

        assertThat(Thresholds.load().get("faithfulness")).hasValue(3.5);
        assertThat(Thresholds.load().get("no-such-metric")).isEmpty();
    }

}
