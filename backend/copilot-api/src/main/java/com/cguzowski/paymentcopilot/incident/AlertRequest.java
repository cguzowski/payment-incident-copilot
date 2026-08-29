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
        @NotBlank(message = "is required") @Size(max = 2000) String description) {

    IngestAlertCommand toCommand(UUID tenantId) {
        return new IngestAlertCommand(tenantId, externalAlertId, severity, detectedAt, title, description);
    }
}
