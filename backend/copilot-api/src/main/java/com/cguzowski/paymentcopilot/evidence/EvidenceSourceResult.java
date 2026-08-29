package com.cguzowski.paymentcopilot.evidence;

import java.time.Instant;
import java.util.UUID;

record EvidenceSourceResult(
        String sourceSystem,
        String sourceTool,
        Instant retrievedAt,
        UUID correlationId,
        UUID toolCallId,
        EvidenceCollectionStatus status,
        String statusDetail,
        String contentSchemaVersion,
        ServiceErrorEvidenceContent content) {}
