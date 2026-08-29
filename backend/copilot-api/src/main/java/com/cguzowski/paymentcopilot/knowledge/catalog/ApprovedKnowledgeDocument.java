package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.Instant;
import java.util.UUID;

record ApprovedKnowledgeDocument(
        UUID documentId,
        UUID tenantId,
        KnowledgeDocumentType type,
        String title,
        String version,
        String incidentFamily,
        String appliesTo,
        KnowledgeApprovalStatus approvalStatus,
        UUID approvedBy,
        Instant approvedAt,
        Instant effectiveAt,
        String sourceName,
        int bodyStartLine,
        String body) {}
