package com.cguzowski.paymentcopilot.incident;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface EvidenceCollectionRepository {

    Optional<EvidenceCollectionContext> findContext(UUID tenantId, UUID investigationId);

    void insertStarted(EvidenceCollectionAttempt attempt);

    boolean complete(EvidenceCollectionAttempt attempt);

    List<EvidenceCollectionAttempt> findAll(UUID tenantId, UUID investigationId);
}
