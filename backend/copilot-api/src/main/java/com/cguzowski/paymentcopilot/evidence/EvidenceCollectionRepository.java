package com.cguzowski.paymentcopilot.evidence;

import java.util.List;
import java.util.UUID;

interface EvidenceCollectionRepository {

    void insertStarted(EvidenceCollectionAttempt attempt);

    boolean complete(EvidenceCollectionAttempt attempt);

    List<EvidenceCollectionAttempt> findAll(UUID tenantId, UUID investigationId);
}
