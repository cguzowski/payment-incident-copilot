package com.cguzowski.syntheticincidentgenerator.comparison;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
final class OllamaTextJudge implements TextJudge {
    private final JsonMapper mapper;
    private final ComparisonProperties properties;
    private final HttpClient http;

    OllamaTextJudge(JsonMapper mapper, ComparisonProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
        http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
    }

    @Override
    public Reply evaluate(String prompt) {
        String separator = "\nCOMPARISON_DATA_JSON\n";
        int boundary = prompt.indexOf(separator);
        String instructions = boundary < 0 ? prompt : prompt.substring(0, boundary);
        String data = boundary < 0 ? "{}" : prompt.substring(boundary + separator.length());
        Map<String, Object> score = Map.of("type", "integer", "minimum", 0, "maximum", 100);
        Map<String, Object> reason = Map.of("type", "string", "minLength", 1, "maxLength", 1500);
        Map<String, Object> schema = Map.of(
                "type",
                "object",
                "additionalProperties",
                false,
                "required",
                List.of("rootCause", "rootCauseReason", "recommendation", "recommendationReason"),
                "properties",
                Map.of(
                        "rootCause",
                        score,
                        "rootCauseReason",
                        reason,
                        "recommendation",
                        score,
                        "recommendationReason",
                        reason));
        String body = mapper.writeValueAsString(Map.of(
                "model",
                properties.model(),
                "stream",
                false,
                "think",
                false,
                "format",
                schema,
                "options",
                Map.of("temperature", 0, "num_predict", 1024, "num_ctx", 8192),
                "messages",
                List.of(Map.of("role", "system", "content", instructions), Map.of("role", "user", "content", data))));
        try {
            var request = HttpRequest.newBuilder(properties.baseUrl().resolve("/api/chat"))
                    .timeout(properties.timeout())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().length() > 100_000)
                throw new IllegalArgumentException("Text evaluator unavailable");
            var envelope = mapper.readTree(response.body());
            if (!envelope.path("done").booleanValue()
                    || !envelope.path("message").path("content").isString()
                    || envelope.path("message").path("content").asText().isBlank()
                    || "length".equals(envelope.path("done_reason").asText()))
                throw new IllegalArgumentException("Text evaluator response incomplete");
            return new Reply(envelope.get("message").get("content").asText(), response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Text evaluator interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Text evaluator unavailable", exception);
        }
    }
}
