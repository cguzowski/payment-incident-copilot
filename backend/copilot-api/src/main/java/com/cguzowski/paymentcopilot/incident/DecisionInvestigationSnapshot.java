package com.cguzowski.paymentcopilot.incident;

import java.util.UUID;

public record DecisionInvestigationSnapshot(
        UUID tenantId,
        UUID investigationId,
        UUID incidentId,
        UUID investigationCorrelationId,
        IncidentStatus incidentStatus) {}
