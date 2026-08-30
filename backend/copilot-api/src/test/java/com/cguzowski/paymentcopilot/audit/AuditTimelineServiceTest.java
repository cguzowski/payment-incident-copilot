package com.cguzowski.paymentcopilot.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cguzowski.paymentcopilot.decision.DecisionTimelineSnapshot;
import com.cguzowski.paymentcopilot.decision.DecisionTimelineSnapshotProvider;
import com.cguzowski.paymentcopilot.evidence.EvidenceTimelineSnapshot;
import com.cguzowski.paymentcopilot.evidence.EvidenceTimelineSnapshotProvider;
import com.cguzowski.paymentcopilot.incident.IncidentTimelineSnapshot;
import com.cguzowski.paymentcopilot.incident.IncidentTimelineSnapshotProvider;
import com.cguzowski.paymentcopilot.knowledge.retrieval.KnowledgeTimelineSnapshot;
import com.cguzowski.paymentcopilot.knowledge.retrieval.KnowledgeTimelineSnapshotProvider;
import com.cguzowski.paymentcopilot.report.ReportTimelineSnapshot;
import com.cguzowski.paymentcopilot.report.ReportTimelineSnapshotProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditTimelineServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INVESTIGATION_ID = UUID.fromString("a012c9cb-85a6-4d77-9703-3b53377b56c3");
    private static final UUID INCIDENT_ID = UUID.fromString("f4749ecb-49b0-4277-a140-cb69485b082f");
    private static final UUID CORRELATION_ID = UUID.fromString("a5d978b5-34c7-42da-9076-22f8e5169315");
    private static final UUID OPERATOR_ID = UUID.fromString("6904706f-d9e6-4543-a1bf-fc4b729e4c05");

    @Test
    void composesEveryAuthoritativeRecordOldestFirstAndPreservesUnattributedHistory() {
        IncidentTimelineSnapshotProvider incidents = mock(IncidentTimelineSnapshotProvider.class);
        EvidenceTimelineSnapshotProvider evidence = mock(EvidenceTimelineSnapshotProvider.class);
        KnowledgeTimelineSnapshotProvider knowledge = mock(KnowledgeTimelineSnapshotProvider.class);
        ReportTimelineSnapshotProvider reports = mock(ReportTimelineSnapshotProvider.class);
        DecisionTimelineSnapshotProvider decisions = mock(DecisionTimelineSnapshotProvider.class);
        when(incidents.findTimelineSnapshot(TENANT_ID, INVESTIGATION_ID))
                .thenReturn(Optional.of(new IncidentTimelineSnapshot(
                        INCIDENT_ID,
                        INVESTIGATION_ID,
                        CORRELATION_ID,
                        Instant.parse("2026-08-30T09:00:00Z"),
                        Instant.parse("2026-08-30T09:01:00Z"),
                        OPERATOR_ID)));
        UUID evidenceId = UUID.fromString("10000000-0000-4000-8000-000000000001");
        when(evidence.findTimelineSnapshots(TENANT_ID, INVESTIGATION_ID))
                .thenReturn(List.of(new EvidenceTimelineSnapshot(
                        evidenceId,
                        CORRELATION_ID,
                        null,
                        UUID.fromString("10000000-0000-4000-8000-000000000002"),
                        "AVAILABLE",
                        Instant.parse("2026-08-30T09:02:00Z"),
                        Instant.parse("2026-08-30T09:03:00Z"))));
        UUID retrievalId = UUID.fromString("20000000-0000-4000-8000-000000000001");
        when(knowledge.findTimelineSnapshots(TENANT_ID, INVESTIGATION_ID))
                .thenReturn(List.of(new KnowledgeTimelineSnapshot(
                        retrievalId,
                        CORRELATION_ID,
                        OPERATOR_ID,
                        "NO_MATCH",
                        Instant.parse("2026-08-30T09:04:00Z"),
                        Instant.parse("2026-08-30T09:05:00Z"))));
        UUID reportId = UUID.fromString("30000000-0000-4000-8000-000000000001");
        when(reports.findTimelineSnapshots(TENANT_ID, INVESTIGATION_ID))
                .thenReturn(List.of(new ReportTimelineSnapshot(
                        reportId,
                        CORRELATION_ID,
                        OPERATOR_ID,
                        "AVAILABLE",
                        Instant.parse("2026-08-30T09:06:00Z"),
                        Instant.parse("2026-08-30T09:07:00Z"),
                        "qwen3:8b",
                        "report-prompt/v1",
                        "incident-report/v1",
                        "PROPOSED")));
        UUID decisionId = UUID.fromString("40000000-0000-4000-8000-000000000001");
        when(decisions.findTimelineSnapshots(TENANT_ID, INVESTIGATION_ID))
                .thenReturn(List.of(new DecisionTimelineSnapshot(
                        decisionId,
                        CORRELATION_ID,
                        reportId,
                        OPERATOR_ID,
                        "APPROVED",
                        "Reviewed evidence supports escalation.",
                        Instant.parse("2026-08-30T09:08:00Z"))));

        List<AuditTimelineEvent> events = new AuditTimelineService(incidents, evidence, knowledge, reports, decisions)
                .timeline(TENANT_ID, INVESTIGATION_ID);

        assertThat(events)
                .extracting(AuditTimelineEvent::eventType)
                .containsExactly(
                        AuditTimelineEventType.ALERT_RECEIVED,
                        AuditTimelineEventType.INVESTIGATION_STARTED,
                        AuditTimelineEventType.EVIDENCE_COLLECTION,
                        AuditTimelineEventType.KNOWLEDGE_RETRIEVAL,
                        AuditTimelineEventType.REPORT_GENERATION,
                        AuditTimelineEventType.HUMAN_DECISION);
        assertThat(events.get(2).actorKind()).isEqualTo(AuditActorKind.UNATTRIBUTED);
        assertThat(events.get(2).actorId()).isNull();
        assertThat(events.get(2).toolCallId()).isNotNull();
        assertThat(events.get(4).resultingIncidentStatus()).isEqualTo("AWAITING_REVIEW");
        assertThat(events.get(4).modelId()).isEqualTo("qwen3:8b");
        assertThat(events.get(5).reason()).isEqualTo("Reviewed evidence supports escalation.");
        assertThat(events.get(5).relatedSourceId()).isEqualTo(reportId);
        assertThat(events.get(5).resultingIncidentStatus()).isEqualTo("APPROVED");
    }
}
