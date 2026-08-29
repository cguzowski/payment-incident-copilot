package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.Optional;
import java.util.UUID;

public interface ReportKnowledgeSnapshotProvider {

    Optional<ReportKnowledgeSnapshot> findForReport(UUID tenantId, UUID investigationId);
}
