package com.cguzowski.paymentcopilot.incident;

import java.time.Instant;
import java.util.UUID;

public record IncidentDetailResponse(
        UUID incidentId,
        String externalAlertId,
        IncidentType incidentType,
        IncidentSeverity severity,
        IncidentStatus status,
        String title,
        String description,
        Instant detectedAt,
        Instant receivedAt,
        UUID activeInvestigationId) {

    static IncidentDetailResponse from(IncidentWorkQueueEntry entry) {
        Incident incident = entry.incident();
        return new IncidentDetailResponse(
                incident.id(),
                incident.externalAlertId(),
                incident.incidentType(),
                incident.severity(),
                incident.status(),
                incident.title(),
                incident.description(),
                incident.detectedAt(),
                incident.receivedAt(),
                entry.activeInvestigationId());
    }
}
