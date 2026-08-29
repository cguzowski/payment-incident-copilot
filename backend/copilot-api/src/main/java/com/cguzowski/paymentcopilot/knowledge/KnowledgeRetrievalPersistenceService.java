package com.cguzowski.paymentcopilot.knowledge;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class KnowledgeRetrievalPersistenceService {

    private final KnowledgeRetrievalRepository repository;

    KnowledgeRetrievalPersistenceService(KnowledgeRetrievalRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    Optional<KnowledgeRetrievalContext> findContext(UUID tenantId, UUID investigationId) {
        return repository.findContext(tenantId, investigationId);
    }

    @Transactional
    void insertStarted(KnowledgeRetrievalAttempt attempt) {
        repository.insertStarted(attempt);
    }

    @Transactional
    boolean complete(KnowledgeRetrievalAttempt attempt) {
        return repository.complete(attempt);
    }

    @Transactional(readOnly = true)
    List<KnowledgeRetrievalAttempt> findAll(UUID tenantId, UUID investigationId) {
        return repository.findAll(tenantId, investigationId);
    }
}
