package com.cguzowski.paymentcopilot.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cguzowski.paymentcopilot.incident.DecisionInvestigationSnapshot;
import com.cguzowski.paymentcopilot.incident.DecisionInvestigationSnapshotProvider;
import com.cguzowski.paymentcopilot.incident.IncidentStatus;
import com.cguzowski.paymentcopilot.report.ReviewCandidate;
import com.cguzowski.paymentcopilot.report.ReviewCandidateProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HumanDecisionServiceTest {

    private static final UUID DECISION_ID = UUID.fromString("955865d8-f60a-4c37-a7f4-92d51b41f01a");
    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INVESTIGATION_ID = UUID.fromString("7f50162a-8dc5-45b0-9c88-dc2f77135e0f");
    private static final UUID INCIDENT_ID = UUID.fromString("ce22cb8d-10d6-4d6d-9a56-f644ae84573d");
    private static final UUID CORRELATION_ID = UUID.fromString("133767cf-8ec8-487d-a09d-19d5efcece07");
    private static final UUID REPORT_ATTEMPT_ID = UUID.fromString("28165339-8e37-49c7-9859-493277b34da2");
    private static final UUID OPERATOR_ID = UUID.fromString("6904706f-d9e6-4543-a1bf-fc4b729e4c05");
    private static final Instant DECIDED_AT = Instant.parse("2026-08-30T12:00:00Z");

    private HumanDecisionPersistenceService persistence;
    private DecisionInvestigationSnapshotProvider investigations;
    private ReviewCandidateProvider reports;
    private HumanDecisionIdentifierGenerator identifiers;
    private HumanDecisionService service;

    @BeforeEach
    void setUp() {
        persistence = mock(HumanDecisionPersistenceService.class);
        investigations = mock(DecisionInvestigationSnapshotProvider.class);
        reports = mock(ReviewCandidateProvider.class);
        identifiers = mock(HumanDecisionIdentifierGenerator.class);
        service = new HumanDecisionService(
                persistence, investigations, reports, identifiers, Clock.fixed(DECIDED_AT, ZoneOffset.UTC));
    }

    @Test
    void recordsDecisionAgainstServerResolvedAvailableReport() {
        when(investigations.findForDecision(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(investigation()));
        when(persistence.find(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.empty());
        when(reports.findReviewCandidate(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(candidate()));
        when(identifiers.next()).thenReturn(DECISION_ID);
        when(persistence.record(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> new HumanDecisionRecordResult(invocation.getArgument(0), true));

        HumanDecisionRecordResult result =
                service.record(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID, DecisionOutcome.APPROVED, "  Reviewed.  ");

        assertThat(result.created()).isTrue();
        assertThat(result.decision().reportAttemptId()).isEqualTo(REPORT_ATTEMPT_ID);
        assertThat(result.decision().reason()).isEqualTo("Reviewed.");
        assertThat(result.decision().outcome()).isEqualTo(DecisionOutcome.APPROVED);
        verify(persistence).record(result.decision());
    }

    @Test
    void returnsExistingDecisionForExactSameOperatorReplay() {
        HumanDecision existing = existing(DecisionOutcome.REJECTED, OPERATOR_ID, "Not supported.");
        when(investigations.findForDecision(TENANT_ID, INVESTIGATION_ID))
                .thenReturn(Optional.of(terminalInvestigation()));
        when(persistence.find(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(existing));

        HumanDecisionRecordResult result =
                service.record(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID, DecisionOutcome.REJECTED, " Not supported. ");

        assertThat(result).isEqualTo(new HumanDecisionRecordResult(existing, false));
        verify(reports, never()).findReviewCandidate(TENANT_ID, INVESTIGATION_ID);
        verify(persistence, never()).record(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsDifferentOrStaleSecondDecision() {
        when(investigations.findForDecision(TENANT_ID, INVESTIGATION_ID))
                .thenReturn(Optional.of(terminalInvestigation()));
        when(persistence.find(TENANT_ID, INVESTIGATION_ID))
                .thenReturn(Optional.of(existing(DecisionOutcome.APPROVED, OPERATOR_ID, "Accepted.")));

        assertThatThrownBy(() -> service.record(
                        TENANT_ID, INVESTIGATION_ID, OPERATOR_ID, DecisionOutcome.REJECTED, "Changed decision."))
                .isInstanceOf(HumanDecisionConflictException.class);
    }

    @Test
    void rejectsMissingReviewCandidateOrIncidentOutsideReview() {
        when(investigations.findForDecision(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(investigation()));
        when(persistence.find(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.empty());
        when(reports.findReviewCandidate(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                        service.record(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID, DecisionOutcome.APPROVED, "Reviewed."))
                .isInstanceOf(HumanDecisionConflictException.class);

        when(investigations.findForDecision(TENANT_ID, INVESTIGATION_ID))
                .thenReturn(Optional.of(terminalInvestigation()));

        assertThatThrownBy(() ->
                        service.record(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID, DecisionOutcome.APPROVED, "Reviewed."))
                .isInstanceOf(HumanDecisionConflictException.class);
    }

    @Test
    void hidesMissingAndCrossTenantInvestigationsBehindNotFound() {
        when(investigations.findForDecision(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                        service.record(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID, DecisionOutcome.APPROVED, "Reviewed."))
                .isInstanceOf(DecisionInvestigationNotFoundException.class);
    }

    private static DecisionInvestigationSnapshot investigation() {
        return new DecisionInvestigationSnapshot(
                TENANT_ID, INVESTIGATION_ID, INCIDENT_ID, CORRELATION_ID, IncidentStatus.AWAITING_REVIEW);
    }

    private static DecisionInvestigationSnapshot terminalInvestigation() {
        return new DecisionInvestigationSnapshot(
                TENANT_ID, INVESTIGATION_ID, INCIDENT_ID, CORRELATION_ID, IncidentStatus.REJECTED);
    }

    private static ReviewCandidate candidate() {
        return new ReviewCandidate(TENANT_ID, INVESTIGATION_ID, INCIDENT_ID, CORRELATION_ID, REPORT_ATTEMPT_ID);
    }

    private static HumanDecision existing(DecisionOutcome outcome, UUID operatorId, String reason) {
        return new HumanDecision(
                DECISION_ID,
                TENANT_ID,
                INVESTIGATION_ID,
                INCIDENT_ID,
                CORRELATION_ID,
                REPORT_ATTEMPT_ID,
                operatorId,
                outcome,
                reason,
                DECIDED_AT);
    }
}
