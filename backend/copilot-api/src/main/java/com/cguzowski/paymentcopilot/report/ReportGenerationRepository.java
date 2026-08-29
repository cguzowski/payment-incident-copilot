package com.cguzowski.paymentcopilot.report;

import java.util.List;
import java.util.UUID;

interface ReportGenerationRepository {

    boolean insertStarted(ReportGenerationAttempt attempt);

    boolean completeFailure(ReportGenerationAttempt attempt);

    boolean completeAvailable(ReportGenerationAttempt attempt);

    List<ReportGenerationAttempt> findAll(UUID tenantId, UUID investigationId);
}
