package com.cguzowski.paymentcopilot.knowledge;

import java.time.Instant;
import java.util.UUID;

record IndexedKnowledgeChunk(
        UUID id,
        KnowledgeChunkDraft draft,
        KnowledgeEmbedding embedding,
        Instant embeddedAt) {
}
