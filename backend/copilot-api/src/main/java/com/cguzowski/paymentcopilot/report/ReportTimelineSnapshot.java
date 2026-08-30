package com.cguzowski.paymentcopilot.report;

import java.time.Instant;
import java.util.UUID;

public record ReportTimelineSnapshot(
        UUID reportAttemptId,
        UUID investigationCorrelationId,
        UUID requestedBy,
        String status,
        Instant requestedAt,
        Instant completedAt,
        String modelId,
        String promptVersion,
        String schemaVersion,
        String disposition) {}
