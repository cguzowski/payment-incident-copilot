package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record IndexedKnowledgeDocument(
        UUID id,
        ApprovedKnowledgeDocument document,
        String sourceContentHash,
        Instant importedAt,
        List<IndexedKnowledgeChunk> chunks) {

    IndexedKnowledgeDocument {
        chunks = List.copyOf(chunks);
    }
}
