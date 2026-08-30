package com.cguzowski.syntheticincidentgenerator.generation;

import java.time.Instant;
import java.util.UUID;

public record AlertIntakeResponse(UUID incidentId, String incidentType, String status, Instant receivedAt) {}
