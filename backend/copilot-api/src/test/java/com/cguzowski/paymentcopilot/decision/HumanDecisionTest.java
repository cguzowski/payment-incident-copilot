package com.cguzowski.paymentcopilot.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HumanDecisionTest {

    private static final UUID DECISION_ID = UUID.fromString("955865d8-f60a-4c37-a7f4-92d51b41f01a");
    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INVESTIGATION_ID = UUID.fromString("7f50162a-8dc5-45b0-9c88-dc2f77135e0f");
    private static final UUID INCIDENT_ID = UUID.fromString("ce22cb8d-10d6-4d6d-9a56-f644ae84573d");
    private static final UUID CORRELATION_ID = UUID.fromString("133767cf-8ec8-487d-a09d-19d5efcece07");
    private static final UUID REPORT_ATTEMPT_ID = UUID.fromString("28165339-8e37-49c7-9859-493277b34da2");
    private static final UUID OPERATOR_ID = UUID.fromString("6904706f-d9e6-4543-a1bf-fc4b729e4c05");
    private static final Instant DECIDED_AT = Instant.parse("2026-08-30T12:00:00Z");

    @Test
    void requiresBoundedReasonForApprovalAndRejection() {
        HumanDecision approved = decision(DecisionOutcome.APPROVED, "  Evidence and guidance support escalation.  ");
        HumanDecision rejected = decision(DecisionOutcome.REJECTED, "The proposed cause is not supported.");

        assertThat(approved.reason()).isEqualTo("Evidence and guidance support escalation.");
        assertThat(rejected.reason()).isEqualTo("The proposed cause is not supported.");
        assertThatThrownBy(() -> decision(DecisionOutcome.APPROVED, "   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> decision(DecisionOutcome.REJECTED, "x".repeat(1_001)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void preservesExactReportAndOperatorProvenance() {
        HumanDecision decision = decision(DecisionOutcome.APPROVED, "Reviewed against the cited sources.");

        assertThat(decision.decisionId()).isEqualTo(DECISION_ID);
        assertThat(decision.tenantId()).isEqualTo(TENANT_ID);
        assertThat(decision.investigationId()).isEqualTo(INVESTIGATION_ID);
        assertThat(decision.incidentId()).isEqualTo(INCIDENT_ID);
        assertThat(decision.investigationCorrelationId()).isEqualTo(CORRELATION_ID);
        assertThat(decision.reportAttemptId()).isEqualTo(REPORT_ATTEMPT_ID);
        assertThat(decision.decidedBy()).isEqualTo(OPERATOR_ID);
        assertThat(decision.decidedAt()).isEqualTo(DECIDED_AT);
    }

    private static HumanDecision decision(DecisionOutcome outcome, String reason) {
        return new HumanDecision(
                DECISION_ID,
                TENANT_ID,
                INVESTIGATION_ID,
                INCIDENT_ID,
                CORRELATION_ID,
                REPORT_ATTEMPT_ID,
                OPERATOR_ID,
                outcome,
                reason,
                DECIDED_AT);
    }
}
