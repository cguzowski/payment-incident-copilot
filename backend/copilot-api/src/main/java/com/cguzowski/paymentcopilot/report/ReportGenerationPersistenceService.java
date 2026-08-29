package com.cguzowski.paymentcopilot.report;

import com.cguzowski.paymentcopilot.incident.ReportLifecyclePort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReportGenerationPersistenceService {

    private final ReportGenerationRepository repository;
    private final ReportLifecyclePort lifecycle;

    ReportGenerationPersistenceService(ReportGenerationRepository repository, ReportLifecyclePort lifecycle) {
        this.repository = repository;
        this.lifecycle = lifecycle;
    }

    @Transactional
    boolean start(ReportGenerationAttempt attempt) {
        return repository.insertStarted(attempt);
    }

    @Transactional
    boolean completeFailure(ReportGenerationAttempt attempt) {
        return repository.completeFailure(attempt);
    }

    @Transactional
    boolean completeAvailable(ReportGenerationAttempt attempt) {
        if (!repository.completeAvailable(attempt)) {
            return false;
        }
        if (!lifecycle.transitionToAwaitingReview(attempt.tenantId(), attempt.incidentId())) {
            throw new IllegalStateException("The incident could not transition to awaiting review.");
        }
        return true;
    }

    @Transactional(readOnly = true)
    List<ReportGenerationAttempt> findAll(UUID tenantId, UUID investigationId) {
        return repository.findAll(tenantId, investigationId);
    }
}
