package com.cguzowski.paymentcopilot.report;

import java.util.Optional;
import java.util.UUID;

public interface ReviewCandidateProvider {

    Optional<ReviewCandidate> findReviewCandidate(UUID tenantId, UUID investigationId);
}
