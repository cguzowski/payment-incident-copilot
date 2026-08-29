package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class KnowledgeIndexPersistenceService {

    private final KnowledgeIndexRepository repository;

    KnowledgeIndexPersistenceService(KnowledgeIndexRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    Optional<String> findSourceContentHash(UUID tenantId, UUID documentId, String version) {
        return repository.findSourceContentHash(tenantId, documentId, version);
    }

    @Transactional
    boolean insert(IndexedKnowledgeDocument indexedDocument) {
        return repository.insert(indexedDocument);
    }
}
