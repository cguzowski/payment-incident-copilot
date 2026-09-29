package com.cguzowski.syntheticincidentgenerator.generation;

import java.time.Instant;
import java.util.UUID;

public record GeneratedIncident(
        UUID incidentId, String incidentType, String queueStatus, Instant receivedAt, GeneratedAlert alert) {}
