package com.cguzowski.paymentcopilot.incident;

import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID incidentId,
        UUID tenantId,
        String externalAlertId,
        IncidentType incidentType,
        IncidentSeverity severity,
        IncidentStatus status,
        String title,
        String description,
        Instant detectedAt,
        Instant receivedAt) {

    static AlertResponse from(Incident incident) {
        return new AlertResponse(
                incident.id(),
                incident.tenantId(),
                incident.externalAlertId(),
                incident.incidentType(),
                incident.severity(),
                incident.status(),
                incident.title(),
                incident.description(),
                incident.detectedAt(),
                incident.receivedAt());
    }
}
