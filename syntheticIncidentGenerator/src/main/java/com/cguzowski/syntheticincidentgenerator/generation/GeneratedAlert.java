package com.cguzowski.syntheticincidentgenerator.generation;

import java.time.Instant;

public record GeneratedAlert(
        String externalAlertId, String severity, Instant detectedAt, String title, String description) {}
