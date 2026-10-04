package com.cguzowski.syntheticincidentgenerator.comparison;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioTruth;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ComparisonRubricTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final ScenarioTruth truth = new ScenarioTruth(
            "Gateway unreachable",
            "PROPOSED",
            "HIGH",
            List.of("GATEWAY_TIMEOUT"),
            "Escalate; do not retry",
            "Approve matching reports only");

    @Test
    void exactEnumsAndRoundedMeanAreDeterministic() {
        var grade = grade(
                truth,
                report("PROPOSED", "HIGH", false),
                "APPROVED",
                new TextScores(81, "Cause matches", 80, "Action matches"));
        assertThat(grade.disposition()).isEqualTo(100);
        assertThat(grade.confidence()).isEqualTo(100);
        assertThat(grade.reportScore()).isEqualTo(90);
        assertThat(grade.reportBand()).isEqualTo("GOOD");
        assertThat(grade.expectedDecision()).isEqualTo("APPROVED");
        assertThat(grade.decisionScore()).isEqualTo(100);
    }

    @Test
    void correctlyRejectingPoorReportIsGoodDecisionButBadReport() {
        var grade = grade(
                truth,
                report("INSUFFICIENT_EVIDENCE", "LOW", false),
                "REJECTED",
                new TextScores(0, "Wrong cause", 0, "Wrong action"));
        assertThat(grade.reportScore()).isZero();
        assertThat(grade.reportBand()).isEqualTo("BAD");
        assertThat(grade.decisionScore()).isEqualTo(100);
        assertThat(grade.decisionBand()).isEqualTo("GOOD");
    }

    @Test
    void incorrectApprovalAndExactConfidenceMismatchDoNotPassDecisionRubric() {
        var grade = grade(
                truth, report("PROPOSED", "MEDIUM", false), "APPROVED", new TextScores(100, "Matches", 100, "Matches"));
        assertThat(grade.confidence()).isZero();
        assertThat(grade.expectedDecision()).isEqualTo("REJECTED");
        assertThat(grade.decisionScore()).isZero();
    }

    @Test
    void bandBoundariesAreStable() {
        assertThat(ComparisonRubric.band(49)).isEqualTo("BAD");
        assertThat(ComparisonRubric.band(50)).isEqualTo("OK");
        assertThat(ComparisonRubric.band(79)).isEqualTo("OK");
        assertThat(ComparisonRubric.band(80)).isEqualTo("GOOD");
    }

    @Test
    void insufficientEvidenceNullsAreScoredWithoutJudgeAndAssertionsFail() {
        var insufficient = new ScenarioTruth(
                "Unknown", "INSUFFICIENT_EVIDENCE", "LOW", List.of(), "No action", "Reject assertions");
        var correct = ComparisonRubric.nullScores(insufficient, report("INSUFFICIENT_EVIDENCE", "LOW", true));
        assertThat(correct.rootCause()).isEqualTo(100);
        assertThat(correct.recommendation()).isEqualTo(100);
        var incorrect = ComparisonRubric.nullScores(insufficient, report("INSUFFICIENT_EVIDENCE", "LOW", false));
        assertThat(incorrect.rootCause()).isZero();
        assertThat(incorrect.recommendation()).isZero();
        assertThat(ComparisonRubric.nullScores(truth, report("PROPOSED", "HIGH", false)))
                .isNull();
    }

    @Test
    void malformedMissingAndOutOfRangeScoresFailRatherThanBecomeZero() {
        for (String output : List.of(
                "{}",
                "{\"rootCause\":101,\"rootCauseReason\":\"x\",\"recommendation\":80,\"recommendationReason\":\"x\"}",
                "{\"rootCause\":80.5,\"rootCauseReason\":\"x\",\"recommendation\":80,\"recommendationReason\":\"x\"}",
                "{\"rootCause\":80,\"rootCauseReason\":\"x\",\"recommendation\":80,\"recommendationReason\":\"x\",\"decision\":100}")) {
            assertThatThrownBy(() -> TextScores.parse(mapper, output)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> grade(truth, mapper.readTree("{}"), "APPROVED", new TextScores(80, "x", 80, "x")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicateFieldsAndTrailingDocumentsAreRejected() {
        String valid =
                "{\"rootCause\":80,\"rootCauseReason\":\"match\",\"recommendation\":80,\"recommendationReason\":\"match\"}";
        assertThatThrownBy(() -> TextScores.parse(mapper, valid + " {}")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                        TextScores.parse(mapper, valid.replace("\"rootCause\":80", "\"rootCause\":0,\"rootCause\":80")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ComparisonGrade grade(
            ScenarioTruth key, tools.jackson.databind.JsonNode report, String decision, TextScores scores) {
        return ComparisonRubric.grade(
                key,
                report,
                decision,
                scores,
                new ConfidenceExpectation(key.expectedConfidence(), "Independent test expectation"));
    }

    private tools.jackson.databind.JsonNode report(String disposition, String confidence, boolean nulls) {
        return mapper.readTree(
                "{\"disposition\":\"%s\",\"confidence\":{\"level\":\"%s\"},\"probableCause\":%s,\"recommendation\":%s}"
                        .formatted(
                                disposition,
                                confidence,
                                nulls ? "null" : "{\"statement\":\"Gateway unreachable\"}",
                                nulls ? "null" : "{\"statement\":\"Escalate; do not retry\"}"));
    }
}
