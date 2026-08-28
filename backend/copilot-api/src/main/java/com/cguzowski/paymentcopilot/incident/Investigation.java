package com.cguzowski.paymentcopilot.incident;

import java.time.Instant;
import java.util.UUID;

record Investigation(UUID id, UUID tenantId, UUID incidentId, UUID startedBy, Instant startedAt, UUID correlationId) {}
