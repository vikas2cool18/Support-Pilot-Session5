package com.acme.supportpilot.eval;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JudgeAgreementTest {

    @Test
    void measuresExactAgreementCloseAgreementAndBias() {
        JudgeAgreement a = JudgeAgreement.of(List.of(new int[] { 5, 5 }, new int[] { 4, 2 }, new int[] { 3, 2 }, new int[] { 5, 4 }));

        assertThat(a.pairs()).isEqualTo(4);
        assertThat(a.exact()).isEqualTo(0.25);
        assertThat(a.withinOne()).isEqualTo(0.75);
        assertThat(a.meanAbsError()).isEqualTo(1.0);
        assertThat(a.judgeBias()).isEqualTo(1.0);   // this judge is more lenient than the humans
    }

    @Test
    void noLabelsMeansNotMeasured() {
        assertThat(JudgeAgreement.of(List.of()).withinOne()).isNaN();
    }

}
