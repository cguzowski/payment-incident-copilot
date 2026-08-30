package com.cguzowski.paymentcopilot.audit;

import com.cguzowski.paymentcopilot.decision.DecisionTimelineSnapshotProvider;
import com.cguzowski.paymentcopilot.evidence.EvidenceTimelineSnapshotProvider;
import com.cguzowski.paymentcopilot.incident.IncidentTimelineSnapshot;
import com.cguzowski.paymentcopilot.incident.IncidentTimelineSnapshotProvider;
import com.cguzowski.paymentcopilot.knowledge.retrieval.KnowledgeTimelineSnapshotProvider;
import com.cguzowski.paymentcopilot.report.ReportTimelineSnapshotProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class AuditTimelineService {

    private final IncidentTimelineSnapshotProvider incidents;
    private final EvidenceTimelineSnapshotProvider evidence;
    private final KnowledgeTimelineSnapshotProvider knowledge;
    private final ReportTimelineSnapshotProvider reports;
    private final DecisionTimelineSnapshotProvider decisions;

    AuditTimelineService(
            IncidentTimelineSnapshotProvider incidents,
            EvidenceTimelineSnapshotProvider evidence,
            KnowledgeTimelineSnapshotProvider knowledge,
            ReportTimelineSnapshotProvider reports,
            DecisionTimelineSnapshotProvider decisions) {
        this.incidents = incidents;
        this.evidence = evidence;
        this.knowledge = knowledge;
        this.reports = reports;
        this.decisions = decisions;
    }

    List<AuditTimelineEvent> timeline(UUID tenantId, UUID investigationId) {
        IncidentTimelineSnapshot incident = incidents
                .findTimelineSnapshot(tenantId, investigationId)
                .orElseThrow(AuditTimelineNotFoundException::new);
        List<AuditTimelineEvent> events = new ArrayList<>();
        events.add(new AuditTimelineEvent(
                incident.incidentId(),
                AuditTimelineEventType.ALERT_RECEIVED,
                incident.alertReceivedAt(),
                null,
                AuditActorKind.SYSTEM,
                null,
                "RECEIVED",
                incident.investigationCorrelationId(),
                "NEW",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Synthetic alert received."));
        events.add(new AuditTimelineEvent(
                incident.investigationId(),
                AuditTimelineEventType.INVESTIGATION_STARTED,
                incident.investigationStartedAt(),
                incident.investigationStartedAt(),
                AuditActorKind.OPERATOR,
                incident.investigationStartedBy(),
                "STARTED",
                incident.investigationCorrelationId(),
                "INVESTIGATING",
                incident.incidentId(),
                null,
                null,
                null,
                null,
                null,
                null,
                "Investigation started by an operator."));
        evidence.findTimelineSnapshots(tenantId, investigationId)
                .forEach(snapshot -> events.add(new AuditTimelineEvent(
                        snapshot.evidenceId(),
                        AuditTimelineEventType.EVIDENCE_COLLECTION,
                        snapshot.requestedAt(),
                        snapshot.completedAt(),
                        actorKind(snapshot.requestedBy()),
                        snapshot.requestedBy(),
                        snapshot.status(),
                        snapshot.investigationCorrelationId(),
                        null,
                        null,
                        snapshot.toolCallId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Synthetic service-error evidence collection.")));
        knowledge
                .findTimelineSnapshots(tenantId, investigationId)
                .forEach(snapshot -> events.add(new AuditTimelineEvent(
                        snapshot.retrievalId(),
                        AuditTimelineEventType.KNOWLEDGE_RETRIEVAL,
                        snapshot.requestedAt(),
                        snapshot.completedAt(),
                        actorKind(snapshot.requestedBy()),
                        snapshot.requestedBy(),
                        snapshot.status(),
                        snapshot.investigationCorrelationId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Approved-knowledge retrieval.")));
        reports.findTimelineSnapshots(tenantId, investigationId)
                .forEach(snapshot -> events.add(new AuditTimelineEvent(
                        snapshot.reportAttemptId(),
                        AuditTimelineEventType.REPORT_GENERATION,
                        snapshot.requestedAt(),
                        snapshot.completedAt(),
                        actorKind(snapshot.requestedBy()),
                        snapshot.requestedBy(),
                        snapshot.status(),
                        snapshot.investigationCorrelationId(),
                        "AVAILABLE".equals(snapshot.status()) ? "AWAITING_REVIEW" : null,
                        null,
                        null,
                        snapshot.modelId(),
                        snapshot.promptVersion(),
                        snapshot.schemaVersion(),
                        snapshot.disposition(),
                        null,
                        "Advisory incident report generation.")));
        decisions
                .findTimelineSnapshots(tenantId, investigationId)
                .forEach(snapshot -> events.add(new AuditTimelineEvent(
                        snapshot.decisionId(),
                        AuditTimelineEventType.HUMAN_DECISION,
                        snapshot.decidedAt(),
                        snapshot.decidedAt(),
                        AuditActorKind.OPERATOR,
                        snapshot.decidedBy(),
                        snapshot.outcome(),
                        snapshot.investigationCorrelationId(),
                        snapshot.outcome(),
                        snapshot.reportAttemptId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        snapshot.reason(),
                        "Final human decision recorded.")));
        return events.stream()
                .sorted(Comparator.comparing(AuditTimelineEvent::occurredAt)
                        .thenComparing(AuditTimelineEvent::eventType)
                        .thenComparing(event -> event.sourceId().toString()))
                .toList();
    }

    private static AuditActorKind actorKind(UUID actorId) {
        return actorId == null ? AuditActorKind.UNATTRIBUTED : AuditActorKind.OPERATOR;
    }
}
