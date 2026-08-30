package com.cguzowski.paymentcopilot.decision;

import java.util.List;
import java.util.UUID;

public interface DecisionTimelineSnapshotProvider {

    List<DecisionTimelineSnapshot> findTimelineSnapshots(UUID tenantId, UUID investigationId);
}
