package com.cguzowski.syntheticincidentgenerator.generation;

import java.time.Instant;

public record AlertIntakeRequest(
        String externalAlertId, String severity, Instant detectedAt, String title, String description) {}
