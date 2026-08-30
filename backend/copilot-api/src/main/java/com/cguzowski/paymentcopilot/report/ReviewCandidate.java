package com.cguzowski.paymentcopilot.report;

import java.util.UUID;

public record ReviewCandidate(
        UUID tenantId, UUID investigationId, UUID incidentId, UUID investigationCorrelationId, UUID reportAttemptId) {}
