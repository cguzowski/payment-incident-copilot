package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.List;
import java.util.UUID;

public interface KnowledgeTimelineSnapshotProvider {

    List<KnowledgeTimelineSnapshot> findTimelineSnapshots(UUID tenantId, UUID investigationId);
}
