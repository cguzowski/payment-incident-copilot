package com.cguzowski.paymentcopilot.knowledge.retrieval;

import com.cguzowski.paymentcopilot.evidence.EvidenceSnapshot;
import com.cguzowski.paymentcopilot.evidence.EvidenceSnapshotProvider;
import com.cguzowski.paymentcopilot.incident.InvestigationSnapshot;
import com.cguzowski.paymentcopilot.incident.InvestigationSnapshotProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class KnowledgeRetrievalContextAssembler {

    private final InvestigationSnapshotProvider investigations;
    private final EvidenceSnapshotProvider evidence;

    KnowledgeRetrievalContextAssembler(
            InvestigationSnapshotProvider investigations, EvidenceSnapshotProvider evidence) {
        this.investigations = investigations;
        this.evidence = evidence;
    }

    Optional<KnowledgeRetrievalContext> find(UUID tenantId, UUID investigationId) {
        return investigations
                .findKnowledgeRetrievalSnapshot(tenantId, investigationId)
                .map(investigation -> assemble(
                        investigation,
                        evidence.findByTenantIdAndInvestigationId(tenantId, investigationId)
                                .orElse(null)));
    }

    private static KnowledgeRetrievalContext assemble(InvestigationSnapshot investigation, EvidenceSnapshot evidence) {
        KnowledgeEvidenceReference evidenceReference = evidence == null
                ? null
                : new KnowledgeEvidenceReference(
                        evidence.latestAttemptId(),
                        evidence.latestStatus(),
                        evidence.applicableAttemptId(),
                        evidence.serviceName(),
                        evidence.errorCounts().stream()
                                .map(error -> new KnowledgeErrorCount(error.errorCode(), error.count()))
                                .toList());
        return new KnowledgeRetrievalContext(
                investigation.tenantId(),
                investigation.investigationId(),
                investigation.correlationId(),
                investigation.incidentFamily(),
                investigation.title(),
                investigation.description(),
                evidenceReference);
    }
}
