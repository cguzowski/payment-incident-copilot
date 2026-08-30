package com.cguzowski.paymentcopilot.evidence;

import java.time.Instant;
import java.util.UUID;

public record EvidenceTimelineSnapshot(
        UUID evidenceId,
        UUID investigationCorrelationId,
        UUID requestedBy,
        UUID toolCallId,
        String status,
        Instant requestedAt,
        Instant completedAt) {}
