package com.cguzowski.paymentcopilot.incident;

import java.util.UUID;

public interface ReportLifecyclePort {

    boolean transitionToAwaitingReview(UUID tenantId, UUID incidentId);
}
