package com.cguzowski.paymentcopilot.incident;

import java.util.UUID;

public interface DecisionLifecyclePort {

    boolean transitionFromAwaitingReview(UUID tenantId, UUID incidentId, IncidentStatus terminalStatus);
}
