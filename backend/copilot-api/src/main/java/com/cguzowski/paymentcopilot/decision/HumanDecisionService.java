package com.cguzowski.paymentcopilot.decision;

import com.cguzowski.paymentcopilot.incident.DecisionInvestigationSnapshot;
import com.cguzowski.paymentcopilot.incident.DecisionInvestigationSnapshotProvider;
import com.cguzowski.paymentcopilot.incident.IncidentStatus;
import com.cguzowski.paymentcopilot.report.ReviewCandidate;
import com.cguzowski.paymentcopilot.report.ReviewCandidateProvider;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class HumanDecisionService {

    private final HumanDecisionPersistenceService persistence;
    private final DecisionInvestigationSnapshotProvider investigations;
    private final ReviewCandidateProvider reports;
    private final HumanDecisionIdentifierGenerator identifiers;
    private final Clock clock;

    HumanDecisionService(
            HumanDecisionPersistenceService persistence,
            DecisionInvestigationSnapshotProvider investigations,
            ReviewCandidateProvider reports,
            HumanDecisionIdentifierGenerator identifiers,
            Clock clock) {
        this.persistence = persistence;
        this.investigations = investigations;
        this.reports = reports;
        this.identifiers = identifiers;
        this.clock = clock;
    }

    HumanDecisionRecordResult record(
            UUID tenantId, UUID investigationId, UUID operatorId, DecisionOutcome outcome, String reason) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(investigationId, "investigationId");
        Objects.requireNonNull(operatorId, "operatorId");
        Objects.requireNonNull(outcome, "outcome");
        String normalizedReason = HumanDecision.normalizeReason(reason);

        DecisionInvestigationSnapshot investigation = investigations
                .findForDecision(tenantId, investigationId)
                .orElseThrow(DecisionInvestigationNotFoundException::new);

        Optional<HumanDecision> existing = persistence.find(tenantId, investigationId);
        if (existing.isPresent()) {
            return replayOrConflict(existing.orElseThrow(), operatorId, outcome, normalizedReason);
        }
        if (investigation.incidentStatus() != IncidentStatus.AWAITING_REVIEW) {
            throw new HumanDecisionConflictException("The investigation is not awaiting human review.");
        }

        ReviewCandidate candidate = reports.findReviewCandidate(tenantId, investigationId)
                .orElseThrow(() -> new HumanDecisionConflictException("No available report is awaiting review."));
        requireMatchingCandidate(investigation, candidate);

        HumanDecision decision = new HumanDecision(
                identifiers.next(),
                tenantId,
                investigationId,
                investigation.incidentId(),
                investigation.investigationCorrelationId(),
                candidate.reportAttemptId(),
                operatorId,
                outcome,
                normalizedReason,
                Instant.now(clock));

        return persistence.record(decision);
    }

    List<HumanDecision> history(UUID tenantId, UUID investigationId) {
        investigations
                .findForDecision(tenantId, investigationId)
                .orElseThrow(DecisionInvestigationNotFoundException::new);
        return persistence.find(tenantId, investigationId).stream().toList();
    }

    private static HumanDecisionRecordResult replayOrConflict(
            HumanDecision existing, UUID operatorId, DecisionOutcome outcome, String normalizedReason) {
        if (existing.decidedBy().equals(operatorId)
                && existing.outcome() == outcome
                && existing.reason().equals(normalizedReason)) {
            return new HumanDecisionRecordResult(existing, false);
        }
        throw new HumanDecisionConflictException("A final human decision is already recorded.");
    }

    private static void requireMatchingCandidate(
            DecisionInvestigationSnapshot investigation, ReviewCandidate candidate) {
        if (!candidate.tenantId().equals(investigation.tenantId())
                || !candidate.investigationId().equals(investigation.investigationId())
                || !candidate.incidentId().equals(investigation.incidentId())
                || !candidate.investigationCorrelationId().equals(investigation.investigationCorrelationId())) {
            throw new IllegalStateException("The review candidate does not match the investigation snapshot.");
        }
    }
}
