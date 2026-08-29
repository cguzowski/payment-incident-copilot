package com.cguzowski.paymentcopilot.report;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record ReportGenerationAttempt(
        UUID attemptId,
        UUID tenantId,
        UUID investigationId,
        UUID incidentId,
        UUID correlationId,
        UUID requestedBy,
        ReportGenerationStatus status,
        Instant requestedAt,
        Instant completedAt,
        String modelId,
        int temperature,
        int maxOutputTokens,
        String promptVersion,
        String promptHash,
        String schemaVersion,
        String schemaHash,
        UUID latestEvidenceId,
        UUID applicableEvidenceId,
        UUID retrievalId,
        String providerRequestId,
        String statusDetail,
        ReportDocument report) {

    static ReportGenerationAttempt started(
            UUID attemptId,
            UUID operatorId,
            Instant requestedAt,
            ReportGenerationContext context,
            String modelId,
            ReportPrompt prompt) {
        return new ReportGenerationAttempt(
                attemptId,
                context.investigation().tenantId(),
                context.investigation().investigationId(),
                context.investigation().incidentId(),
                context.investigation().correlationId(),
                operatorId,
                ReportGenerationStatus.STARTED,
                requestedAt,
                null,
                modelId,
                0,
                4_096,
                prompt.promptVersion(),
                prompt.promptHash(),
                prompt.schemaVersion(),
                prompt.schemaHash(),
                context.evidence().latestAttemptId(),
                context.evidence().applicableAttemptId(),
                context.knowledge().retrievalId(),
                null,
                null,
                null);
    }

    ReportGenerationAttempt completeAvailable(
            Instant completedAt, ReportModelResponse response, ReportDocument report) {
        return terminal(ReportGenerationStatus.AVAILABLE, completedAt, response.providerRequestId(), null, report);
    }

    ReportGenerationAttempt completeFailure(
            ReportGenerationStatus terminalStatus, Instant completedAt, String providerRequestId, String statusDetail) {
        if (terminalStatus == ReportGenerationStatus.AVAILABLE || !terminalStatus.isTerminal()) {
            throw new IllegalArgumentException("A report failure requires a terminal failure status.");
        }
        return terminal(terminalStatus, completedAt, providerRequestId, statusDetail, null);
    }

    List<UUID> evidenceSnapshotIds() {
        if (applicableEvidenceId == null || latestEvidenceId.equals(applicableEvidenceId)) {
            return List.of(latestEvidenceId);
        }
        return List.of(latestEvidenceId, applicableEvidenceId);
    }

    private ReportGenerationAttempt terminal(
            ReportGenerationStatus terminalStatus,
            Instant terminalAt,
            String terminalProviderRequestId,
            String detail,
            ReportDocument document) {
        if (status != ReportGenerationStatus.STARTED) {
            throw new IllegalStateException("Only a started report attempt can be completed.");
        }
        return new ReportGenerationAttempt(
                attemptId,
                tenantId,
                investigationId,
                incidentId,
                correlationId,
                requestedBy,
                terminalStatus,
                requestedAt,
                terminalAt,
                modelId,
                temperature,
                maxOutputTokens,
                promptVersion,
                promptHash,
                schemaVersion,
                schemaHash,
                latestEvidenceId,
                applicableEvidenceId,
                retrievalId,
                terminalProviderRequestId,
                detail,
                document);
    }
}
