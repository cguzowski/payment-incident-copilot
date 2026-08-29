package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.List;
import java.util.UUID;

interface KnowledgeRetrievalRepository {

    void insertStarted(KnowledgeRetrievalAttempt attempt);

    boolean complete(KnowledgeRetrievalAttempt attempt);

    List<KnowledgeRetrievalAttempt> findAll(UUID tenantId, UUID investigationId);
}
