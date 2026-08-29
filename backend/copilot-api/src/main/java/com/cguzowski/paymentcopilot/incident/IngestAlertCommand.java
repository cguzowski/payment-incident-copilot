package com.cguzowski.paymentcopilot.incident;

import java.time.Instant;
import java.util.UUID;

public record IngestAlertCommand(
        UUID tenantId,
        String externalAlertId,
        IncidentSeverity severity,
        Instant detectedAt,
        String title,
        String description) {}
