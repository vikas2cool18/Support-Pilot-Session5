package com.acme.supportpilot.knowledge;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class VectorMathTest {

    @Test
    void identicalVectorsHaveSimilarityOne() {
        assertThat(VectorMath.cosine(new float[] { 1, 2, 3 }, new float[] { 1, 2, 3 })).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void orthogonalVectorsHaveSimilarityZero() {
        assertThat(VectorMath.cosine(new float[] { 1, 0 }, new float[] { 0, 1 })).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void lengthDoesNotMatterOnlyDirection() {
        assertThat(VectorMath.cosine(new float[] { 1, 1 }, new float[] { 10, 10 })).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void differentDimensionsAreRejected() {
        assertThatThrownBy(() -> VectorMath.cosine(new float[] { 1 }, new float[] { 1, 2 }))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
