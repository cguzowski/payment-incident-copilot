package com.cguzowski.paymentcopilot.incident;

import java.util.UUID;

public record InvestigationSnapshot(
        UUID tenantId,
        UUID investigationId,
        UUID correlationId,
        String incidentFamily,
        String title,
        String description) {}
