package com.cguzowski.paymentcopilot.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record AlertRequest(
        @NotBlank(message = "is required") @Size(max = 120) String externalAlertId,
        @NotNull(message = "is required") IncidentSeverity severity,
        @NotNull(message = "is required") Instant detectedAt,
        @NotBlank(message = "is required") @Size(max = 500) String title,
        @NotBlank(message = "is required") @Size(max = 2000) String description,
        IncidentType incidentType) {

    public AlertRequest {
        if (incidentType == null) incidentType = IncidentType.AUTHORIZATION_DECLINE_RATE_SPIKE;
    }

    public AlertRequest(
            String externalAlertId, IncidentSeverity severity, Instant detectedAt, String title, String description) {
        this(externalAlertId, severity, detectedAt, title, description, IncidentType.AUTHORIZATION_DECLINE_RATE_SPIKE);
    }

    IngestAlertCommand toCommand(UUID tenantId) {
        return new IngestAlertCommand(
                tenantId, externalAlertId, severity, detectedAt, title, description, incidentType);
    }
}
