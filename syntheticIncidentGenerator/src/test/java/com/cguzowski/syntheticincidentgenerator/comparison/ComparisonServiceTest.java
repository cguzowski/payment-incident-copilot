package com.cguzowski.syntheticincidentgenerator.comparison;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.cguzowski.syntheticincidentgenerator.generation.AnswerKeyRevealResponse;
import com.cguzowski.syntheticincidentgenerator.generation.AnswerKeyRevealService;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioTruth;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class ComparisonServiceTest {
    @TempDir
    Path directory;

    private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    private final UUID incident = UUID.randomUUID();
    private final UUID operator = UUID.randomUUID();
    private final UUID tenant = UUID.randomUUID();
    private final AnswerKeyRevealService reveal = mock(AnswerKeyRevealService.class);
    private final FrozenOutcomeClient outcomes = mock(FrozenOutcomeClient.class);
    private final TextJudge judge = mock(TextJudge.class);
    private final ScenarioTruth truth = new ScenarioTruth(
            "Gateway unreachable",
            "PROPOSED",
            "HIGH",
            List.of("UPSTREAM_CONNECTION_RESET"),
            "Escalate; do not retry",
            "Approve matching reports only");
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void boundedRateLimitEvidenceMatchesMediumInsteadOfTheStaticHighKey() {
        var rateTruth = new ScenarioTruth(
                "Rate limited",
                "PROPOSED",
                "HIGH",
                List.of("UPSTREAM_RATE_LIMITED dominates the window"),
                "Escalate",
                "Match");
        prepare(rateTruth);
        var prior = outcomes.find(incident, "APPROVED");
        var evidence = mapper.readTree("""
                [{"evidenceId":"latest","status":"AVAILABLE","contentSchemaVersion":"service-errors/v1",
                "content":{"serviceName":"payment-authorization","observedFrom":"2026-10-01T11:55:00Z",
                "observedTo":"2026-10-01T12:00:00Z","errors":[{"errorCode":"UPSTREAM_RATE_LIMITED",
                "count":83,"observedAt":"2026-10-01T11:59:00Z"}]}}]
                """);
        var report = mapper.readTree(prior.report().toString().replace("HIGH", "MEDIUM"));
        when(outcomes.find(incident, "APPROVED"))
                .thenReturn(new FrozenOutcome(
                        prior.investigationId(),
                        prior.reportAttemptId(),
                        prior.decisionId(),
                        prior.outcome(),
                        report,
                        evidence,
                        prior.decision(),
                        mapper.readTree("{\"latestEvidenceId\":\"latest\"}")));
        when(judge.evaluate(anyString()))
                .thenReturn(new TextJudge.Reply(
                        "{\"rootCause\":100,\"rootCauseReason\":\"Match\",\"recommendation\":100,\"recommendationReason\":\"Match\"}",
                        "raw"));
        var result = service().compare(incident, operator);
        assertThat(result.grade().confidence()).isEqualTo(100);
        assertThat(result.grade().expectedConfidence()).isEqualTo("MEDIUM");
        assertThat(result.grade().originalExpectedConfidence()).isEqualTo("HIGH");
        assertThat(result.grade().confidenceRuleVersion()).isEqualTo(ConfidenceExpectation.VERSION);
        assertThat(result.grade().confidenceReason()).contains("traffic shape");
        assertThat(result.grade().reportScore()).isEqualTo(100);
        assertThat(rateTruth.expectedConfidence()).isEqualTo("HIGH");
        // Changing the actual level never changes the expectation; HIGH now fails.
        when(outcomes.find(incident, "APPROVED"))
                .thenReturn(new FrozenOutcome(
                        prior.investigationId(),
                        prior.reportAttemptId(),
                        prior.decisionId(),
                        prior.outcome(),
                        mapper.readTree(prior.report().toString().replace("MEDIUM", "HIGH")),
                        evidence,
                        prior.decision(),
                        mapper.readTree("{\"latestEvidenceId\":\"latest\"}")));
        var mismatch = service().compare(incident, operator);
        assertThat(mismatch.grade().expectedConfidence()).isEqualTo("MEDIUM");
        assertThat(mismatch.grade().confidence()).isZero();
    }

    @Test
    void terminalGatePrecedesAnyOutcomeOrModelAccess() {
        when(reveal.reveal(incident, operator)).thenThrow(new IllegalStateException("sealed"));
        assertThatThrownBy(() -> service().compare(incident, operator)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(outcomes, judge);
    }

    @Test
    void textOnlyPromptAndImmutableArtifactsBindTheExactFrozenReport() throws Exception {
        prepare(truth);
        when(judge.evaluate(anyString()))
                .thenReturn(new TextJudge.Reply(
                        "{\"rootCause\":90,\"rootCauseReason\":\"Matches the gateway outage\",\"recommendation\":80,\"recommendationReason\":\"Matches escalation\"}",
                        "raw-provider-response"));
        var result = service().compare(incident, operator);
        assertThat(result.status()).isEqualTo("AVAILABLE");
        assertThat(result.grade().reportScore()).isEqualTo(93);
        assertThat(result.grade().decisionScore()).isEqualTo(100);
        var captured = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(judge).evaluate(captured.capture());
        assertThat(captured.getValue())
                .contains("Gateway unreachable", "Escalate; do not retry", "UPSTREAM_CONNECTION_RESET")
                .doesNotContain(
                        "APPROVED", "expectedConfidence", "expectedDisposition", "decisionRule", "human-secret-reason");
        Path artifact = directory.resolve(tenant.toString()).resolve(result.comparisonId() + ".json");
        var retained = mapper.readTree(Files.readString(artifact));
        assertThat(retained.path("inputHash").asText()).hasSize(64);
        assertThat(retained.path("promptHash").asText()).hasSize(64);
        assertThat(retained.path("response").path("rubricVersion").asText()).isEqualTo(ComparisonRubric.VERSION);
        assertThat(retained.path("input")
                        .path("answerKey")
                        .path("answerKey")
                        .path("expectedConfidence")
                        .asText())
                .isEqualTo("HIGH");
        assertThat(retained.path("response")
                        .path("grade")
                        .path("expectedConfidence")
                        .asText())
                .isEqualTo("MEDIUM");
        assertThat(retained.path("calls").get(0).path("providerResponse").asText())
                .isEqualTo("raw-provider-response");
        assertThat(retained.path("input")
                        .path("outcomes")
                        .path("decision")
                        .path("reason")
                        .asText())
                .isEqualTo("human-secret-reason");
        assertThat(Files.list(directory.resolve(tenant.toString())).count()).isEqualTo(1);
    }

    @Test
    void malformedJudgeResponseRetriesOnceThenLeavesBothCardsUnscored() {
        prepare(truth);
        when(judge.evaluate(anyString())).thenReturn(new TextJudge.Reply("{}", "{}"));
        var result = service().compare(incident, operator);
        assertThat(result.status()).isEqualTo("UNAVAILABLE");
        assertThat(result.grade()).isNull();
        verify(judge, times(2)).evaluate(anyString());
    }

    @Test
    void retryCanRecoverAndNeverOverwritesEarlierAttempts() throws Exception {
        prepare(truth);
        when(judge.evaluate(anyString()))
                .thenThrow(new IllegalStateException("timeout"))
                .thenReturn(new TextJudge.Reply(
                        "{\"rootCause\":100,\"rootCauseReason\":\"Match\",\"recommendation\":100,\"recommendationReason\":\"Match\"}",
                        "raw"));
        var first = service().compare(incident, operator);
        var second = service().compare(incident, operator);
        assertThat(first.status()).isEqualTo("AVAILABLE");
        assertThat(first.comparisonId()).isNotEqualTo(second.comparisonId());
        assertThat(Files.list(directory.resolve(tenant.toString())).count()).isEqualTo(2);
    }

    @Test
    void insufficientEvidenceCorrectNullsSkipModelEntirely() {
        prepare(new ScenarioTruth(
                "Unknown", "INSUFFICIENT_EVIDENCE", "LOW", List.of(), "No action", "Reject assertions"));
        var result = service().compare(incident, operator);
        assertThat(result.status()).isEqualTo("AVAILABLE");
        assertThat(result.grade().rootCause()).isEqualTo(100);
        assertThat(result.grade().recommendation()).isEqualTo(100);
        verifyNoInteractions(judge);
    }

    @Test
    void unavailableInputsDoNotInvokeModelOrFabricateScores() {
        prepare(truth);
        when(outcomes.find(incident, "APPROVED")).thenThrow(new IllegalArgumentException("missing report"));
        var result = service().compare(incident, operator);
        assertThat(result.status()).isEqualTo("UNAVAILABLE");
        assertThat(result.grade()).isNull();
        verifyNoInteractions(judge);
    }

    private ComparisonService service() {
        var properties = new ComparisonProperties(
                java.net.URI.create("http://localhost:11434"),
                "judge-test",
                java.time.Duration.ofSeconds(2),
                directory);
        return new ComparisonService(
                reveal,
                outcomes,
                judge,
                new ComparisonArtifactStore(mapper, properties),
                mapper,
                properties,
                tenant,
                clock);
    }

    private void prepare(ScenarioTruth key) {
        when(reveal.reveal(incident, operator))
                .thenReturn(new AnswerKeyRevealResponse(
                        incident, "APPROVED", operator, clock.instant(), "scenario-oracle/v1", key));
        boolean nulls = key.expectedDisposition().equals("INSUFFICIENT_EVIDENCE");
        var report = mapper.readTree(
                "{\"disposition\":\"%s\",\"confidence\":{\"level\":\"%s\"},\"probableCause\":%s,\"recommendation\":%s}"
                        .formatted(
                                key.expectedDisposition(),
                                nulls ? "LOW" : "MEDIUM",
                                nulls ? "null" : "{\"statement\":\"Gateway unreachable\"}",
                                nulls ? "null" : "{\"statement\":\"Escalate; do not retry\"}"));
        when(outcomes.find(incident, "APPROVED"))
                .thenReturn(new FrozenOutcome(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "APPROVED",
                        report,
                        mapper.readTree("""
                                [{"evidenceId":"latest","status":"%s","contentSchemaVersion":"service-errors/v1",
                                "content":{"serviceName":"payment-authorization","observedFrom":"2026-10-01T11:55:00Z",
                                "observedTo":"2026-10-01T12:00:00Z","errors":[{"errorCode":"UPSTREAM_CONNECTION_RESET",
                                "count":83,"observedAt":"2026-10-01T11:59:00Z"}]}}]
                                """.formatted(nulls ? "PARTIAL" : "AVAILABLE")),
                        mapper.readTree("{\"reason\":\"human-secret-reason\"}"),
                        mapper.readTree("{\"latestEvidenceId\":\"latest\"}")));
    }
}
