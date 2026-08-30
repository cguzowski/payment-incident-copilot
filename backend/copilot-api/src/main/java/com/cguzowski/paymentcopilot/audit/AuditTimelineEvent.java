package com.cguzowski.paymentcopilot.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditTimelineEvent(
        UUID sourceId,
        AuditTimelineEventType eventType,
        Instant occurredAt,
        Instant completedAt,
        AuditActorKind actorKind,
        UUID actorId,
        String status,
        UUID investigationCorrelationId,
        String resultingIncidentStatus,
        UUID relatedSourceId,
        UUID toolCallId,
        String modelId,
        String promptVersion,
        String schemaVersion,
        String disposition,
        String reason,
        String description) {}
