package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeTimelineSnapshot(
        UUID retrievalId,
        UUID investigationCorrelationId,
        UUID requestedBy,
        String status,
        Instant requestedAt,
        Instant completedAt) {}
