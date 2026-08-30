package com.cguzowski.syntheticincidentgenerator.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class StaticUiContractTest {

    @Test
    void clearlyLabelsSeparateTestSystemAndProvidesOneRedGenerationAction() throws IOException {
        String html = resource("static/index.html");
        String css = resource("static/styles.css");
        String javascript = resource("static/app.js");

        assertThat(html)
                .contains("STANDALONE TEST SYSTEM")
                .contains("Not part of the operator console")
                .contains("id=\"generate-incident\"")
                .contains("Generate synthetic incident")
                .contains("<details")
                .contains("Reveal deterministic answer key after review");
        assertThat(css)
                .contains("--danger: #b42318")
                .contains(".generate-button")
                .contains("background: var(--danger)");
        assertThat(javascript)
                .contains("fetch('/api/generations', { method: 'POST' })")
                .contains("button.disabled = true")
                .contains("textContent")
                .doesNotContain("innerHTML");
    }

    private static String resource(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
