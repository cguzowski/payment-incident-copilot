package com.cguzowski.paymentcopilot.incident;

import java.util.Optional;
import java.util.UUID;

public interface EvidenceCollectionContextProvider {

    Optional<EvidenceCollectionContext> findEvidenceCollectionContext(UUID tenantId, UUID investigationId);
}
