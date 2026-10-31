package com.acme.supportpilot.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RagOptionsTest {

    @Test
    void overridesReplaceOnlyTheGivenSwitches() {
        RagOptions defaults = new RagOptions(true, 2, true, true, 5);

        RagOptions changed = defaults.with(null, 0, false, null);

        assertThat(changed).isEqualTo(new RagOptions(true, 0, false, true, 5));
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertThatThrownBy(() -> new RagOptions(false, 9, false, false, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RagOptions.naive(0)).isInstanceOf(IllegalArgumentException.class);
    }

}
