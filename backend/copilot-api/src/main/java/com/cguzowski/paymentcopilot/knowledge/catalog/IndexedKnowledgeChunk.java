package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.Instant;
import java.util.UUID;

record IndexedKnowledgeChunk(UUID id, KnowledgeChunkDraft draft, KnowledgeEmbedding embedding, Instant embeddedAt) {}
