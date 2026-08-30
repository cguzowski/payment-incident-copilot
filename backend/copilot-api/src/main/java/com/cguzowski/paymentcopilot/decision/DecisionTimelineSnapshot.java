package com.cguzowski.paymentcopilot.decision;

import java.time.Instant;
import java.util.UUID;

public record DecisionTimelineSnapshot(
        UUID decisionId,
        UUID investigationCorrelationId,
        UUID reportAttemptId,
        UUID decidedBy,
        String outcome,
        String reason,
        Instant decidedAt) {}
