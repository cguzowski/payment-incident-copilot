package com.cguzowski.paymentcopilot.report;

import java.time.Instant;
import java.util.UUID;

record ReportGenerationResponse(
        UUID attemptId,
        UUID investigationId,
        ReportGenerationStatus status,
        Instant requestedAt,
        Instant completedAt,
        String modelId,
        String promptVersion,
        String schemaVersion,
        UUID latestEvidenceId,
        UUID applicableEvidenceId,
        UUID retrievalId,
        String statusDetail,
        ReportDocument report) {

    static ReportGenerationResponse from(ReportGenerationAttempt attempt) {
        return new ReportGenerationResponse(
                attempt.attemptId(),
                attempt.investigationId(),
                attempt.status(),
                attempt.requestedAt(),
                attempt.completedAt(),
                attempt.modelId(),
                attempt.promptVersion(),
                attempt.schemaVersion(),
                attempt.latestEvidenceId(),
                attempt.applicableEvidenceId(),
                attempt.retrievalId(),
                attempt.statusDetail(),
                attempt.report());
    }
}
