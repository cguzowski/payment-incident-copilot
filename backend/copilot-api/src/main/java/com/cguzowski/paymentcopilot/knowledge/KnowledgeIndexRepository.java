package com.cguzowski.paymentcopilot.knowledge;

import java.util.Optional;
import java.util.UUID;

interface KnowledgeIndexRepository {
    Optional<String> findSourceContentHash(UUID tenantId, UUID documentId, String version);

    boolean insert(IndexedKnowledgeDocument indexedDocument);
}
