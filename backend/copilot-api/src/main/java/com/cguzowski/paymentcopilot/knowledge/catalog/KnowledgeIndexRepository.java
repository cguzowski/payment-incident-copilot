package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.util.Optional;
import java.util.UUID;

interface KnowledgeIndexRepository {
    Optional<String> findSourceContentHash(UUID tenantId, UUID documentId, String version);

    boolean insert(IndexedKnowledgeDocument indexedDocument);
}
