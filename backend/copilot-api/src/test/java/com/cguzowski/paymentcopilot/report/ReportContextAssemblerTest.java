package com.cguzowski.paymentcopilot.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cguzowski.paymentcopilot.evidence.ReportEvidenceObservation;
import com.cguzowski.paymentcopilot.evidence.ReportEvidenceSnapshot;
import com.cguzowski.paymentcopilot.evidence.ReportEvidenceSnapshotProvider;
import com.cguzowski.paymentcopilot.incident.ReportInvestigationSnapshot;
import com.cguzowski.paymentcopilot.incident.ReportInvestigationSnapshotProvider;
import com.cguzowski.paymentcopilot.knowledge.retrieval.ReportKnowledgeChunk;
import com.cguzowski.paymentcopilot.knowledge.retrieval.ReportKnowledgeSnapshot;
import com.cguzowski.paymentcopilot.knowledge.retrieval.ReportKnowledgeSnapshotProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ReportContextAssemblerTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INVESTIGATION_ID = UUID.fromString("7f50162a-8dc5-45b0-9c88-dc2f77135e0f");
    private static final UUID INCIDENT_ID = UUID.fromString("ce22cb8d-10d6-4d6d-9a56-f644ae84573d");
    private static final UUID CORRELATION_ID = UUID.fromString("133767cf-8ec8-487d-a09d-19d5efcece07");
    private static final UUID LATEST_EVIDENCE_ID = UUID.fromString("103bf2a7-02bf-4cea-a2d1-c075bff44510");
    private static final UUID APPLICABLE_EVIDENCE_ID = UUID.fromString("d3a12cd9-ef1d-4328-b63d-2ecea6558e2d");
    private static final UUID RETRIEVAL_ID = UUID.fromString("a74f88ed-e295-4caf-9404-a22f733d86ec");
    private static final UUID CHUNK_ID = UUID.fromString("97ec5709-147d-458a-a5b4-c95de1a7a32a");

    @Test
    void composesExactTenantScopedReportSnapshotsInOrder() {
        ReportInvestigationSnapshotProvider investigations = mock(ReportInvestigationSnapshotProvider.class);
        ReportEvidenceSnapshotProvider evidence = mock(ReportEvidenceSnapshotProvider.class);
        ReportKnowledgeSnapshotProvider knowledge = mock(ReportKnowledgeSnapshotProvider.class);
        ReportInvestigationSnapshot incidentSnapshot = new ReportInvestigationSnapshot(
                TENANT_ID,
                INVESTIGATION_ID,
                INCIDENT_ID,
                CORRELATION_ID,
                "INVESTIGATING",
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Authorization declines elevated",
                "Synthetic gateway failures increased authorization declines.");
        ReportEvidenceSnapshot evidenceSnapshot = new ReportEvidenceSnapshot(
                LATEST_EVIDENCE_ID,
                "UNAVAILABLE",
                APPLICABLE_EVIDENCE_ID,
                "authorization-gateway",
                List.of(new ReportEvidenceObservation(
                        "evt-1", Instant.parse("2026-08-29T08:00:00Z"), "GATEWAY_TIMEOUT", 12)));
        ReportKnowledgeSnapshot knowledgeSnapshot = new ReportKnowledgeSnapshot(
                RETRIEVAL_ID,
                "AVAILABLE",
                List.of(new ReportKnowledgeChunk(
                        CHUNK_ID,
                        UUID.fromString("a9114c6f-a967-4bd7-a871-7e24716588e4"),
                        "RUNBOOK",
                        "Authorization Decline Runbook",
                        "1.0",
                        "Gateway Failures > Diagnosis",
                        "Inspect upstream gateway timeout telemetry.")));
        when(investigations.findForReport(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(incidentSnapshot));
        when(evidence.findForReport(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(evidenceSnapshot));
        when(knowledge.findForReport(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(knowledgeSnapshot));
        ReportContextAssembler assembler = new ReportContextAssembler(investigations, evidence, knowledge);

        Optional<ReportGenerationContext> context = assembler.find(TENANT_ID, INVESTIGATION_ID);

        assertThat(context)
                .contains(new ReportGenerationContext(incidentSnapshot, evidenceSnapshot, knowledgeSnapshot));
        InOrder order = inOrder(investigations, evidence, knowledge);
        order.verify(investigations).findForReport(TENANT_ID, INVESTIGATION_ID);
        order.verify(evidence).findForReport(TENANT_ID, INVESTIGATION_ID);
        order.verify(knowledge).findForReport(TENANT_ID, INVESTIGATION_ID);
    }

    @Test
    void crossTenantMissPerformsNoDownstreamSnapshotReads() {
        ReportInvestigationSnapshotProvider investigations = mock(ReportInvestigationSnapshotProvider.class);
        ReportEvidenceSnapshotProvider evidence = mock(ReportEvidenceSnapshotProvider.class);
        ReportKnowledgeSnapshotProvider knowledge = mock(ReportKnowledgeSnapshotProvider.class);
        when(investigations.findForReport(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.empty());
        ReportContextAssembler assembler = new ReportContextAssembler(investigations, evidence, knowledge);

        assertThat(assembler.find(TENANT_ID, INVESTIGATION_ID)).isEmpty();

        verify(evidence, never()).findForReport(TENANT_ID, INVESTIGATION_ID);
        verify(knowledge, never()).findForReport(TENANT_ID, INVESTIGATION_ID);
    }

    @Test
    void ownedInvestigationWithoutTerminalEvidenceIsAWorkflowConflict() {
        ReportInvestigationSnapshotProvider investigations = mock(ReportInvestigationSnapshotProvider.class);
        ReportEvidenceSnapshotProvider evidence = mock(ReportEvidenceSnapshotProvider.class);
        ReportKnowledgeSnapshotProvider knowledge = mock(ReportKnowledgeSnapshotProvider.class);
        ReportInvestigationSnapshot snapshot = new ReportInvestigationSnapshot(
                TENANT_ID,
                INVESTIGATION_ID,
                INCIDENT_ID,
                CORRELATION_ID,
                "INVESTIGATING",
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Authorization declines elevated",
                "Synthetic incident.");
        when(investigations.findForReport(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(snapshot));
        when(evidence.findForReport(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.empty());
        ReportContextAssembler assembler = new ReportContextAssembler(investigations, evidence, knowledge);

        assertThatThrownBy(() -> assembler.find(TENANT_ID, INVESTIGATION_ID))
                .isInstanceOf(ReportGenerationConflictException.class)
                .hasMessageContaining("Terminal evidence");

        verify(knowledge, never()).findForReport(TENANT_ID, INVESTIGATION_ID);
    }
}
