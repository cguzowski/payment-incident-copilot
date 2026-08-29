package com.cguzowski.paymentcopilot.incident;

import java.util.Optional;
import java.util.UUID;

public interface ReportInvestigationSnapshotProvider {

    Optional<ReportInvestigationSnapshot> findForReport(UUID tenantId, UUID investigationId);
}
