package com.cguzowski.syntheticincidentgenerator.generation;

import java.time.Instant;

public record AlertIntakeRequest(
        String externalAlertId,
        String severity,
        Instant detectedAt,
        String title,
        String description,
        String incidentType) {
    public AlertIntakeRequest(
            String externalAlertId, String severity, Instant detectedAt, String title, String description) {
        this(externalAlertId, severity, detectedAt, title, description, "AUTHORIZATION_DECLINE_RATE_SPIKE");
    }
}
