package com.cguzowski.syntheticincidentgenerator.comparison;

import java.util.Set;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

record TextScores(int rootCause, String rootCauseReason, int recommendation, String recommendationReason) {
    TextScores {
        if (rootCause < 0
                || rootCause > 100
                || recommendation < 0
                || recommendation > 100
                || rootCauseReason == null
                || rootCauseReason.isBlank()
                || rootCauseReason.length() > 1500
                || recommendationReason == null
                || recommendationReason.isBlank()
                || recommendationReason.length() > 1500) {
            throw new IllegalArgumentException("Invalid text scores");
        }
    }

    static TextScores parse(JsonMapper mapper, String output) {
        try {
            JsonNode node = mapper.rebuild()
                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .build()
                    .readTree(output);
            if (!node.isObject()
                    || node.size() != 4
                    || !node.propertyNames()
                            .equals(Set.of("rootCause", "rootCauseReason", "recommendation", "recommendationReason"))
                    || !node.path("rootCause").isIntegralNumber()
                    || !node.path("recommendation").isIntegralNumber()
                    || !node.path("rootCause").canConvertToInt()
                    || !node.path("recommendation").canConvertToInt()
                    || !node.path("rootCauseReason").isString()
                    || !node.path("recommendationReason").isString()) {
                throw new IllegalArgumentException("Invalid text score JSON");
            }
            return new TextScores(
                    node.get("rootCause").intValue(),
                    node.get("rootCauseReason").asText(),
                    node.get("recommendation").intValue(),
                    node.get("recommendationReason").asText());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid text score JSON", exception);
        }
    }
}
