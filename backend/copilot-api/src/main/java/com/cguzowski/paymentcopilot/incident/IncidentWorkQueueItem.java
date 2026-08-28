package com.cguzowski.paymentcopilot.incident;

import java.time.Instant;
import java.util.UUID;

public record IncidentWorkQueueItem(
        UUID incidentId,
        String externalAlertId,
        IncidentType incidentType,
        IncidentSeverity severity,
        IncidentStatus status,
        String title,
        Instant detectedAt,
        Instant receivedAt,
        UUID activeInvestigationId) {

    static IncidentWorkQueueItem from(IncidentWorkQueueEntry entry) {
        Incident incident = entry.incident();
        return new IncidentWorkQueueItem(
                incident.id(),
                incident.externalAlertId(),
                incident.incidentType(),
                incident.severity(),
                incident.status(),
                incident.title(),
                incident.detectedAt(),
                incident.receivedAt(),
                entry.activeInvestigationId());
    }
}
