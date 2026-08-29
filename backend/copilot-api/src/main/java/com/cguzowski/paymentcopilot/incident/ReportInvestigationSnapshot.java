package com.cguzowski.paymentcopilot.incident;

import java.util.UUID;

public record ReportInvestigationSnapshot(
        UUID tenantId,
        UUID investigationId,
        UUID incidentId,
        UUID correlationId,
        String incidentStatus,
        String incidentFamily,
        String title,
        String description) {}
