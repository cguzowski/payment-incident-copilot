package com.cguzowski.paymentcopilot.evidence;

import java.util.List;
import java.util.UUID;

public interface EvidenceTimelineSnapshotProvider {

    List<EvidenceTimelineSnapshot> findTimelineSnapshots(UUID tenantId, UUID investigationId);
}
