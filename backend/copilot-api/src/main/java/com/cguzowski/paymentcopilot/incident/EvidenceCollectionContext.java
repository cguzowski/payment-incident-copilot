package com.cguzowski.paymentcopilot.incident;

import java.util.UUID;

public record EvidenceCollectionContext(
        UUID tenantId, UUID investigationId, UUID correlationId, String scenarioReference) {}
