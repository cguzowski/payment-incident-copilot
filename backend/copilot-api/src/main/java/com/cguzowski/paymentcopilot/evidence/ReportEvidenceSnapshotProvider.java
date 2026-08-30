package com.cguzowski.paymentcopilot.evidence;

import java.util.Optional;
import java.util.UUID;

public interface ReportEvidenceSnapshotProvider {

    Optional<ReportEvidenceSnapshot> findForReport(UUID tenantId, UUID investigationId);
}
