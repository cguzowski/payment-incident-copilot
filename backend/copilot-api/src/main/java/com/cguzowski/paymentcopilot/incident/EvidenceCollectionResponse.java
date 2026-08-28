package com.cguzowski.paymentcopilot.incident;

import java.time.Instant;
import java.util.UUID;

public record EvidenceCollectionResponse(
        UUID evidenceId,
        EvidenceCollectionStatus status,
        String sourceSystem,
        String sourceTool,
        UUID toolCallId,
        Instant requestedAt,
        Instant retrievedAt,
        Instant completedAt,
        String contentSchemaVersion,
        ServiceErrorEvidenceContent content,
        String statusDetail) {

    static EvidenceCollectionResponse from(EvidenceCollectionAttempt attempt) {
        return new EvidenceCollectionResponse(
                attempt.evidenceId(),
                attempt.status(),
                attempt.sourceSystem(),
                attempt.sourceTool(),
                attempt.toolCallId(),
                attempt.requestedAt(),
                attempt.retrievedAt(),
                attempt.completedAt(),
                attempt.contentSchemaVersion(),
                attempt.content(),
                attempt.statusDetail());
    }
}
