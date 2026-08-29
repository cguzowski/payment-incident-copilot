package com.cguzowski.paymentcopilot.incident;

import java.time.Instant;
import java.util.UUID;

public record Incident(
        UUID id,
        UUID tenantId,
        String externalAlertId,
        IncidentType incidentType,
        IncidentSeverity severity,
        IncidentStatus status,
        String title,
        String description,
        Instant detectedAt,
        Instant receivedAt) {}
