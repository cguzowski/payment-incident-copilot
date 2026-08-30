package com.cguzowski.paymentcopilot.incident;

import java.time.Instant;
import java.util.UUID;

public record IncidentTimelineSnapshot(
        UUID incidentId,
        UUID investigationId,
        UUID investigationCorrelationId,
        Instant alertReceivedAt,
        Instant investigationStartedAt,
        UUID investigationStartedBy) {}
