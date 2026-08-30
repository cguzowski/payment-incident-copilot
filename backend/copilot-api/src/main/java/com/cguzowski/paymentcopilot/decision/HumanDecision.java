package com.cguzowski.paymentcopilot.decision;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

record HumanDecision(
        UUID decisionId,
        UUID tenantId,
        UUID investigationId,
        UUID incidentId,
        UUID investigationCorrelationId,
        UUID reportAttemptId,
        UUID decidedBy,
        DecisionOutcome outcome,
        String reason,
        Instant decidedAt) {

    HumanDecision {
        Objects.requireNonNull(decisionId, "decisionId");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(investigationId, "investigationId");
        Objects.requireNonNull(incidentId, "incidentId");
        Objects.requireNonNull(investigationCorrelationId, "investigationCorrelationId");
        Objects.requireNonNull(reportAttemptId, "reportAttemptId");
        Objects.requireNonNull(decidedBy, "decidedBy");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(decidedAt, "decidedAt");
        reason = normalizeReason(reason);
    }

    static String normalizeReason(String value) {
        if (value == null) {
            throw new IllegalArgumentException("A decision reason is required.");
        }
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > 1_000) {
            throw new IllegalArgumentException("A decision reason must contain between 1 and 1000 characters.");
        }
        return normalized;
    }
}
