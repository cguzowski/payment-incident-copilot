package com.cguzowski.paymentcopilot.incident;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class EvidenceCollectionPersistenceService {

    private final EvidenceCollectionRepository repository;

    EvidenceCollectionPersistenceService(EvidenceCollectionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    Optional<EvidenceCollectionContext> findContext(UUID tenantId, UUID investigationId) {
        return repository.findContext(tenantId, investigationId);
    }

    @Transactional
    void insertStarted(EvidenceCollectionAttempt attempt) {
        repository.insertStarted(attempt);
    }

    @Transactional
    boolean complete(EvidenceCollectionAttempt attempt) {
        return repository.complete(attempt);
    }

    @Transactional(readOnly = true)
    List<EvidenceCollectionAttempt> findAll(UUID tenantId, UUID investigationId) {
        return repository.findAll(tenantId, investigationId);
    }
}
