package com.cguzowski.syntheticincidentgenerator.comparison;

import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioTruth;
import java.util.Set;
import tools.jackson.databind.JsonNode;

final class ComparisonRubric {
    static final String VERSION = "comparison-rubric/v1";

    private ComparisonRubric() {}

    static ComparisonGrade grade(ScenarioTruth truth, JsonNode report, String decision, TextScores scores) {
        validateReport(report);
        if (!Set.of("APPROVED", "REJECTED").contains(decision)) {
            throw new IllegalArgumentException("Invalid final decision");
        }
        int disposition =
                truth.expectedDisposition().equals(report.get("disposition").asText()) ? 100 : 0;
        int confidence = truth.expectedConfidence()
                        .equals(report.get("confidence").get("level").asText())
                ? 100
                : 0;
        int average = (int) Math.round((disposition + confidence + scores.rootCause() + scores.recommendation()) / 4.0);
        String expected =
                disposition == 100 && confidence == 100 && scores.rootCause() >= 80 && scores.recommendation() >= 80
                        ? "APPROVED"
                        : "REJECTED";
        int decisionScore = expected.equals(decision) ? 100 : 0;
        return new ComparisonGrade(
                disposition,
                confidence,
                scores.rootCause(),
                scores.rootCauseReason(),
                scores.recommendation(),
                scores.recommendationReason(),
                average,
                band(average),
                expected,
                decision,
                decisionScore,
                band(decisionScore));
    }

    static TextScores nullScores(ScenarioTruth truth, JsonNode report) {
        validateReport(report);
        if (!"INSUFFICIENT_EVIDENCE".equals(truth.expectedDisposition())) {
            return null;
        }
        return new TextScores(
                report.get("probableCause").isNull() ? 100 : 0,
                report.get("probableCause").isNull()
                        ? "Correctly withheld a cause for insufficient evidence."
                        : "A cause was asserted despite insufficient evidence.",
                report.get("recommendation").isNull() ? 100 : 0,
                report.get("recommendation").isNull()
                        ? "Correctly withheld a recommendation for insufficient evidence."
                        : "A recommendation was asserted despite insufficient evidence.");
    }

    static void validateReport(JsonNode report) {
        if (report == null
                || !report.isObject()
                || !Set.of("PROPOSED", "INSUFFICIENT_EVIDENCE")
                        .contains(report.path("disposition").asText())
                || !Set.of("LOW", "MEDIUM", "HIGH")
                        .contains(report.path("confidence").path("level").asText())
                || !report.has("probableCause")
                || !report.has("recommendation")) {
            throw new IllegalArgumentException("Missing report comparison inputs");
        }
        for (String field : new String[] {"probableCause", "recommendation"}) {
            JsonNode claim = report.get(field);
            if (!claim.isNull()
                    && (!claim.isObject()
                            || !claim.path("statement").isString()
                            || claim.path("statement").asText().isBlank())) {
                throw new IllegalArgumentException("Invalid report text");
            }
        }
    }

    static String band(int score) {
        return score >= 80 ? "GOOD" : score >= 50 ? "OK" : "BAD";
    }
}
