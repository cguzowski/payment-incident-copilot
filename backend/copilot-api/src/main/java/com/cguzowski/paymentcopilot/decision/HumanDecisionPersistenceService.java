package com.cguzowski.paymentcopilot.decision;

import com.cguzowski.paymentcopilot.incident.DecisionLifecyclePort;
import com.cguzowski.paymentcopilot.incident.IncidentStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class HumanDecisionPersistenceService {

    private final HumanDecisionRepository repository;
    private final DecisionLifecyclePort lifecycle;

    HumanDecisionPersistenceService(HumanDecisionRepository repository, DecisionLifecyclePort lifecycle) {
        this.repository = repository;
        this.lifecycle = lifecycle;
    }

    @Transactional
    HumanDecisionRecordResult record(HumanDecision decision) {
        Optional<HumanDecision> existing = repository.find(decision.tenantId(), decision.investigationId());
        if (existing.isPresent()) {
            return replayOrConflict(existing.orElseThrow(), decision);
        }
        if (!repository.insertIfAbsent(decision)) {
            HumanDecision concurrent = repository
                    .find(decision.tenantId(), decision.investigationId())
                    .orElseThrow(() -> new HumanDecisionConflictException("A concurrent decision could not be read."));
            return replayOrConflict(concurrent, decision);
        }
        IncidentStatus terminalStatus =
                decision.outcome() == DecisionOutcome.APPROVED ? IncidentStatus.APPROVED : IncidentStatus.REJECTED;
        if (!lifecycle.transitionFromAwaitingReview(decision.tenantId(), decision.incidentId(), terminalStatus)) {
            throw new HumanDecisionConflictException("The investigation state changed before the decision committed.");
        }
        return new HumanDecisionRecordResult(decision, true);
    }

    @Transactional(readOnly = true)
    Optional<HumanDecision> find(UUID tenantId, UUID investigationId) {
        return repository.find(tenantId, investigationId);
    }

    private static HumanDecisionRecordResult replayOrConflict(HumanDecision existing, HumanDecision requested) {
        if (existing.decidedBy().equals(requested.decidedBy())
                && existing.outcome() == requested.outcome()
                && existing.reason().equals(requested.reason())) {
            return new HumanDecisionRecordResult(existing, false);
        }
        throw new HumanDecisionConflictException("A final human decision is already recorded.");
    }
}
