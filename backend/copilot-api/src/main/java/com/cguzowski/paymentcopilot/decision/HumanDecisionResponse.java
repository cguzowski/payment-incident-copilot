package com.cguzowski.paymentcopilot.decision;

import java.time.Instant;
import java.util.UUID;

record HumanDecisionResponse(
        UUID decisionId,
        UUID investigationId,
        UUID reportAttemptId,
        DecisionOutcome outcome,
        String incidentStatus,
        String reason,
        UUID decidedBy,
        Instant decidedAt) {

    static HumanDecisionResponse from(HumanDecision decision) {
        return new HumanDecisionResponse(
                decision.decisionId(),
                decision.investigationId(),
                decision.reportAttemptId(),
                decision.outcome(),
                decision.outcome().name(),
                decision.reason(),
                decision.decidedBy(),
                decision.decidedAt());
    }
}
