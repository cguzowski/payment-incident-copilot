package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.UUID;

record KnowledgeRetrievalContext(
        UUID tenantId,
        UUID investigationId,
        UUID investigationCorrelationId,
        String incidentFamily,
        String incidentTitle,
        String incidentDescription,
        KnowledgeEvidenceReference evidence) {}
