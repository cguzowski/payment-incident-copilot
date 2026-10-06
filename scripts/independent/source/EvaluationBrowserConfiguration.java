package com.cguzowski.syntheticincidentgenerator;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

@RestController
class EvaluationBrowserConfiguration {
    private final String script;

    EvaluationBrowserConfiguration(@Value("${evaluation.browser-base-url}") URI baseUrl, JsonMapper mapper) {
        if (!("http".equals(baseUrl.getScheme()) || "https".equals(baseUrl.getScheme()))
                || baseUrl.getHost() == null
                || baseUrl.getUserInfo() != null
                || baseUrl.getQuery() != null
                || baseUrl.getFragment() != null
                || !(baseUrl.getPath().isEmpty() || "/".equals(baseUrl.getPath()))) {
            throw new IllegalArgumentException("Evaluator browser URL must be an HTTP(S) origin");
        }
        script = "const evaluationBaseUrl = "
                + mapper.writeValueAsString(baseUrl.toString().replaceAll("/$", "")) + ";";
    }

    @GetMapping(value = "/evaluation-config.js", produces = "text/javascript")
    String configuration() {
        return script;
    }
}
