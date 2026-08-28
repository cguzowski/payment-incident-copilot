package com.cguzowski.paymentcopilot.incident;

import java.time.Instant;
import java.util.UUID;

record EvidenceCollectionAttempt(
        UUID evidenceId,
        UUID tenantId,
        UUID investigationId,
        UUID toolCallId,
        UUID investigationCorrelationId,
        String sourceSystem,
        String sourceTool,
        String scenarioReference,
        EvidenceCollectionStatus status,
        Instant requestedAt,
        Instant retrievedAt,
        Instant completedAt,
        String contentSchemaVersion,
        ServiceErrorEvidenceContent content,
        String statusDetail) {

    static EvidenceCollectionAttempt started(
            UUID evidenceId,
            UUID tenantId,
            UUID investigationId,
            UUID toolCallId,
            UUID investigationCorrelationId,
            String sourceSystem,
            String sourceTool,
            String scenarioReference,
            Instant requestedAt,
            String contentSchemaVersion) {
        return new EvidenceCollectionAttempt(
                evidenceId,
                tenantId,
                investigationId,
                toolCallId,
                investigationCorrelationId,
                sourceSystem,
                sourceTool,
                scenarioReference,
                EvidenceCollectionStatus.STARTED,
                requestedAt,
                null,
                null,
                contentSchemaVersion,
                null,
                null);
    }

    EvidenceCollectionAttempt complete(
            EvidenceCollectionStatus terminalStatus,
            Instant retrievedAt,
            Instant completedAt,
            ServiceErrorEvidenceContent content,
            String statusDetail) {
        if (status != EvidenceCollectionStatus.STARTED || !terminalStatus.isTerminal()) {
            throw new IllegalStateException("Only a started evidence attempt can move to a terminal status.");
        }
        return new EvidenceCollectionAttempt(
                evidenceId,
                tenantId,
                investigationId,
                toolCallId,
                investigationCorrelationId,
                sourceSystem,
                sourceTool,
                scenarioReference,
                terminalStatus,
                requestedAt,
                retrievedAt,
                completedAt,
                contentSchemaVersion,
                content,
                statusDetail);
    }
}
