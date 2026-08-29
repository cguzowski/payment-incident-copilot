package com.cguzowski.paymentcopilot.knowledge;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface KnowledgeRetrievalRepository {

    Optional<KnowledgeRetrievalContext> findContext(UUID tenantId, UUID investigationId);

    void insertStarted(KnowledgeRetrievalAttempt attempt);

    boolean complete(KnowledgeRetrievalAttempt attempt);

    List<KnowledgeRetrievalAttempt> findAll(UUID tenantId, UUID investigationId);
}
