package com.cguzowski.syntheticincidentgenerator.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ComparisonUiBehaviorTest {
    @Test
    void exercisesPostRevealUiWithDeterministicHttpResponses() throws Exception {
        var process = new ProcessBuilder("node", "--test", "src/test/js/comparison-ui.test.cjs")
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor()).as(output).isZero();
    }
}
