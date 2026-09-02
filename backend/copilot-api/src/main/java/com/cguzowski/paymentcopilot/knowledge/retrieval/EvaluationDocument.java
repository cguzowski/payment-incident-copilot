package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.time.Instant;
import java.util.UUID;

record EvaluationDocument(
        String key,
        UUID documentId,
        String version,
        String type,
        String approvalStatus,
        String incidentFamily,
        String source,
        String pdf,
        String sourceSha256,
        String pdfSha256,
        int pageCount,
        String replacement,
        Instant approvedAt,
        Instant effectiveAt) {}
