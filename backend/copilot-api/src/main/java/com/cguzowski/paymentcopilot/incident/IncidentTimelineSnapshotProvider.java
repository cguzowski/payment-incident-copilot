package com.cguzowski.paymentcopilot.incident;

import java.util.Optional;
import java.util.UUID;

public interface IncidentTimelineSnapshotProvider {

    Optional<IncidentTimelineSnapshot> findTimelineSnapshot(UUID tenantId, UUID investigationId);
}
