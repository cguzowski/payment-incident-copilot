package com.cguzowski.paymentcopilot.incident;

import java.util.Optional;
import java.util.UUID;

public interface DecisionInvestigationSnapshotProvider {

    Optional<DecisionInvestigationSnapshot> findForDecision(UUID tenantId, UUID investigationId);
}
