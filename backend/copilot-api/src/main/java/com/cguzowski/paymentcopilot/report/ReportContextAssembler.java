package com.cguzowski.paymentcopilot.report;

import com.cguzowski.paymentcopilot.evidence.ReportEvidenceSnapshotProvider;
import com.cguzowski.paymentcopilot.incident.ReportInvestigationSnapshotProvider;
import com.cguzowski.paymentcopilot.knowledge.retrieval.ReportKnowledgeSnapshotProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class ReportContextAssembler {

    private final ReportInvestigationSnapshotProvider investigations;
    private final ReportEvidenceSnapshotProvider evidence;
    private final ReportKnowledgeSnapshotProvider knowledge;

    public ReportContextAssembler(
            ReportInvestigationSnapshotProvider investigations,
            ReportEvidenceSnapshotProvider evidence,
            ReportKnowledgeSnapshotProvider knowledge) {
        this.investigations = investigations;
        this.evidence = evidence;
        this.knowledge = knowledge;
    }

    public Optional<ReportGenerationContext> find(UUID tenantId, UUID investigationId) {
        return investigations.findForReport(tenantId, investigationId).map(investigation -> {
            var evidenceSnapshot = evidence.findForReport(tenantId, investigationId)
                    .orElseThrow(() -> new ReportGenerationConflictException(
                            "Terminal evidence is required before report generation."));
            var knowledgeSnapshot = knowledge
                    .findForReport(tenantId, investigationId)
                    .orElseThrow(() -> new ReportGenerationConflictException(
                            "A terminal knowledge retrieval is required before report generation."));
            return new ReportGenerationContext(investigation, evidenceSnapshot, knowledgeSnapshot);
        });
    }

    public Optional<com.cguzowski.paymentcopilot.incident.ReportInvestigationSnapshot> findInvestigation(
            UUID tenantId, UUID investigationId) {
        return investigations.findForReport(tenantId, investigationId);
    }
}
