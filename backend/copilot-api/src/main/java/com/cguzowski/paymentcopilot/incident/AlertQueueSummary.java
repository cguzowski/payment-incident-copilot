package com.cguzowski.paymentcopilot.incident;

import java.time.Instant;
import java.util.UUID;

public record AlertQueueSummary(
        UUID incidentId,
        String externalAlertId,
        IncidentType incidentType,
        IncidentSeverity severity,
        IncidentStatus status,
        String title,
        Instant detectedAt,
        Instant receivedAt) {

    static AlertQueueSummary from(Incident incident) {
        return new AlertQueueSummary(
                incident.id(),
                incident.externalAlertId(),
                incident.incidentType(),
                incident.severity(),
                incident.status(),
                incident.title(),
                incident.detectedAt(),
                incident.receivedAt());
    }
}
