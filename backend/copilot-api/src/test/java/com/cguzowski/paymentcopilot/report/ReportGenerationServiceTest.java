package com.cguzowski.paymentcopilot.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cguzowski.paymentcopilot.evidence.ReportEvidenceObservation;
import com.cguzowski.paymentcopilot.evidence.ReportEvidenceSnapshot;
import com.cguzowski.paymentcopilot.incident.ReportInvestigationSnapshot;
import com.cguzowski.paymentcopilot.knowledge.retrieval.ReportKnowledgeChunk;
import com.cguzowski.paymentcopilot.knowledge.retrieval.ReportKnowledgeSnapshot;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ReportGenerationServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INVESTIGATION_ID = UUID.fromString("7f50162a-8dc5-45b0-9c88-dc2f77135e0f");
    private static final UUID INCIDENT_ID = UUID.fromString("ce22cb8d-10d6-4d6d-9a56-f644ae84573d");
    private static final UUID CORRELATION_ID = UUID.fromString("133767cf-8ec8-487d-a09d-19d5efcece07");
    private static final UUID OPERATOR_ID = UUID.fromString("6904706f-d9e6-4543-a1bf-fc4b729e4c05");
    private static final UUID ATTEMPT_ID = UUID.fromString("28165339-8e37-49c7-9859-493277b34da2");
    private static final UUID EVIDENCE_ID = UUID.fromString("d3a12cd9-ef1d-4328-b63d-2ecea6558e2d");
    private static final UUID RETRIEVAL_ID = UUID.fromString("a74f88ed-e295-4caf-9404-a22f733d86ec");
    private static final UUID CHUNK_ID = UUID.fromString("97ec5709-147d-458a-a5b4-c95de1a7a32a");
    private static final Instant NOW = Instant.parse("2026-08-29T10:00:00Z");

    private final ReportContextAssembler contexts = mock(ReportContextAssembler.class);
    private final ReportGenerationPersistenceService persistence = mock(ReportGenerationPersistenceService.class);
    private final ReportModel model = mock(ReportModel.class);
    private final ReportPromptFactory prompts = mock(ReportPromptFactory.class);
    private final ReportOutputParser parser = mock(ReportOutputParser.class);
    private final ReportIdentifierGenerator identifiers = mock(ReportIdentifierGenerator.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final ReportModelCallExecutor modelCalls = new ReportModelCallExecutor(Duration.ofSeconds(1));

    private ReportGenerationService service;

    @BeforeEach
    void setUp() {
        service = new ReportGenerationService(
                contexts, persistence, model, modelCalls, prompts, parser, identifiers, clock);
    }

    @Test
    void recordsStartedBeforeCallingModelAndCompletesAvailableAtomically() {
        ReportGenerationContext context = context("INVESTIGATING");
        ReportPrompt prompt =
                new ReportPrompt("prompt", "report-prompt/v1", "a".repeat(64), "report-v1", "b".repeat(64));
        ReportDocument document = proposedReport();
        when(contexts.find(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(context));
        when(identifiers.next()).thenReturn(ATTEMPT_ID);
        when(prompts.build(context)).thenReturn(prompt);
        when(persistence.start(any())).thenReturn(true);
        when(model.modelId()).thenReturn("global.amazon.nova-2-lite-v1:0");
        when(model.generate(prompt.text())).thenReturn(new ReportModelResponse("{json}", "provider-request-1"));
        when(parser.parse("{json}", context)).thenReturn(document);
        when(persistence.completeAvailable(any())).thenReturn(true);

        ReportGenerationResponse response = service.generate(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID);

        assertThat(response.status()).isEqualTo(ReportGenerationStatus.AVAILABLE);
        assertThat(response.report()).isEqualTo(document);
        InOrder order = inOrder(contexts, persistence, model, parser);
        order.verify(contexts).find(TENANT_ID, INVESTIGATION_ID);
        order.verify(persistence).start(any());
        order.verify(model).generate("prompt");
        order.verify(parser).parse("{json}", context);
        order.verify(persistence).completeAvailable(any());
    }

    @Test
    void rejectsInvalidIncidentStateWithoutAttemptOrModelCall() {
        when(contexts.find(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(context("AWAITING_REVIEW")));

        assertThatThrownBy(() -> service.generate(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID))
                .isInstanceOf(ReportGenerationConflictException.class);

        verify(persistence, never()).start(any());
        verify(model, never()).generate(any());
    }

    @Test
    void rejectsConcurrentOrPriorSuccessfulGenerationBeforeModelCall() {
        ReportGenerationContext context = context("INVESTIGATING");
        when(contexts.find(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(context));
        when(identifiers.next()).thenReturn(ATTEMPT_ID);
        when(prompts.build(context))
                .thenReturn(
                        new ReportPrompt("prompt", "report-prompt/v1", "a".repeat(64), "report-v1", "b".repeat(64)));
        when(model.modelId()).thenReturn("global.amazon.nova-2-lite-v1:0");
        when(persistence.start(any())).thenReturn(false);

        assertThatThrownBy(() -> service.generate(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID))
                .isInstanceOf(ReportGenerationConflictException.class);

        verify(model, never()).generate(any());
        verify(persistence, never()).completeAvailable(any());
        verify(persistence, never()).completeFailure(any());
    }

    @Test
    void mapsProviderTimeoutToVisibleTerminalAttemptAndLeavesReportAbsent() {
        ReportGenerationContext context = context("INVESTIGATING");
        ReportPrompt prompt =
                new ReportPrompt("prompt", "report-prompt/v1", "a".repeat(64), "report-v1", "b".repeat(64));
        when(contexts.find(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(context));
        when(identifiers.next()).thenReturn(ATTEMPT_ID);
        when(prompts.build(context)).thenReturn(prompt);
        when(persistence.start(any())).thenReturn(true);
        when(model.modelId()).thenReturn("global.amazon.nova-2-lite-v1:0");
        when(model.generate("prompt")).thenThrow(new ReportModelTimedOutException());
        when(persistence.completeFailure(any())).thenReturn(true);

        ReportGenerationResponse response = service.generate(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID);

        assertThat(response.status()).isEqualTo(ReportGenerationStatus.TIMED_OUT);
        assertThat(response.report()).isNull();
        verify(persistence).completeFailure(any());
        verify(parser, never()).parse(any(), any());
        verify(persistence, never()).completeAvailable(any());
    }

    @Test
    void doesNotParseOrPersistALateResponseAfterTimeout() {
        ReportGenerationContext context = context("INVESTIGATING");
        ReportPrompt prompt =
                new ReportPrompt("prompt", "report-prompt/v1", "a".repeat(64), "report-v1", "b".repeat(64));
        when(contexts.find(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(context));
        when(identifiers.next()).thenReturn(ATTEMPT_ID);
        when(prompts.build(context)).thenReturn(prompt);
        when(persistence.start(any())).thenReturn(true);
        when(model.modelId()).thenReturn("test-report-model");
        when(model.generate("prompt")).thenAnswer(invocation -> {
            try {
                new java.util.concurrent.CountDownLatch(1).await();
                throw new AssertionError("The stalled model should be cancelled.");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return new ReportModelResponse("late output", "late-request");
            }
        });
        when(persistence.completeFailure(any())).thenReturn(true);
        service = new ReportGenerationService(
                contexts,
                persistence,
                model,
                new ReportModelCallExecutor(Duration.ofMillis(100)),
                prompts,
                parser,
                identifiers,
                clock);

        ReportGenerationResponse response = service.generate(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID);

        assertThat(response.status()).isEqualTo(ReportGenerationStatus.TIMED_OUT);
        assertThat(response.report()).isNull();
        verify(parser, never()).parse(any(), any());
        verify(persistence).completeFailure(any());
        verify(persistence, never()).completeAvailable(any());
    }

    @Test
    void mapsUntrustedInvalidOutputToMalformedWithoutPersistingRawOutput() {
        ReportGenerationContext context = context("INVESTIGATING");
        ReportPrompt prompt =
                new ReportPrompt("prompt", "report-prompt/v1", "a".repeat(64), "report-v1", "b".repeat(64));
        when(contexts.find(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(context));
        when(identifiers.next()).thenReturn(ATTEMPT_ID);
        when(prompts.build(context)).thenReturn(prompt);
        when(persistence.start(any())).thenReturn(true);
        when(model.modelId()).thenReturn("global.amazon.nova-2-lite-v1:0");
        when(model.generate("prompt")).thenReturn(new ReportModelResponse("untrusted", "provider-request-2"));
        when(parser.parse("untrusted", context)).thenThrow(new InvalidReportDocumentException("invalid"));
        when(persistence.completeFailure(any())).thenReturn(true);

        ReportGenerationResponse response = service.generate(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID);

        assertThat(response.status()).isEqualTo(ReportGenerationStatus.MALFORMED);
        assertThat(response.statusDetail()).isEqualTo("The model response did not match report-v1.");
        verify(persistence).completeFailure(any());
    }

    private static ReportGenerationContext context(String incidentStatus) {
        return new ReportGenerationContext(
                new ReportInvestigationSnapshot(
                        TENANT_ID,
                        INVESTIGATION_ID,
                        INCIDENT_ID,
                        CORRELATION_ID,
                        incidentStatus,
                        "AUTHORIZATION_DECLINE_RATE_SPIKE",
                        "Authorization declines elevated",
                        "Synthetic gateway failures increased authorization declines."),
                new ReportEvidenceSnapshot(
                        EVIDENCE_ID,
                        "AVAILABLE",
                        EVIDENCE_ID,
                        "authorization-gateway",
                        List.of(new ReportEvidenceObservation(
                                "evt-1", Instant.parse("2026-08-29T08:00:00Z"), "GATEWAY_TIMEOUT", 12))),
                new ReportKnowledgeSnapshot(
                        RETRIEVAL_ID,
                        "AVAILABLE",
                        List.of(new ReportKnowledgeChunk(
                                CHUNK_ID,
                                UUID.fromString("a9114c6f-a967-4bd7-a871-7e24716588e4"),
                                "RUNBOOK",
                                "Authorization Decline Runbook",
                                "1.0",
                                "Gateway Failures > Diagnosis",
                                "Inspect upstream gateway timeout telemetry."))));
    }

    private static ReportDocument proposedReport() {
        ReportClaim citedEvidence = new ReportClaim("Observed gateway timeouts.", List.of(EVIDENCE_ID), List.of());
        ReportClaim citedKnowledge =
                new ReportClaim("Follow gateway diagnostics.", List.of(EVIDENCE_ID), List.of(CHUNK_ID));
        return new ReportDocument(
                ReportDisposition.PROPOSED,
                citedEvidence,
                List.of(citedEvidence),
                List.of(citedKnowledge),
                citedKnowledge,
                new ReportConfidence(ReportConfidenceLevel.MEDIUM, "One source is available.", List.of(EVIDENCE_ID)),
                citedKnowledge,
                List.of(),
                List.of());
    }
}
