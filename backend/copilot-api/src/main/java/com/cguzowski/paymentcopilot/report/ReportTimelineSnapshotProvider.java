package com.cguzowski.paymentcopilot.report;

import java.util.List;
import java.util.UUID;

public interface ReportTimelineSnapshotProvider {

    List<ReportTimelineSnapshot> findTimelineSnapshots(UUID tenantId, UUID investigationId);
}
