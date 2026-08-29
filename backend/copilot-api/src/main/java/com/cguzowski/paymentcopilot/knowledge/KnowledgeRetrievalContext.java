package com.cguzowski.paymentcopilot.knowledge;

import com.cguzowski.paymentcopilot.incident.IncidentType;
import java.util.UUID;

record KnowledgeRetrievalContext(
        UUID tenantId,
        UUID investigationId,
        UUID investigationCorrelationId,
        IncidentType incidentType,
        String incidentTitle,
        String incidentDescription,
        KnowledgeEvidenceReference evidence) {
}
