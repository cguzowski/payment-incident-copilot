package com.cguzowski.paymentcopilot.incident;

import java.util.Optional;
import java.util.UUID;

public interface InvestigationSnapshotProvider {

    Optional<InvestigationSnapshot> findKnowledgeRetrievalSnapshot(UUID tenantId, UUID investigationId);
}
