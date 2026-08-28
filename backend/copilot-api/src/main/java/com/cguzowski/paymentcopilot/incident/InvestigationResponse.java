package com.cguzowski.paymentcopilot.incident;

import java.time.Instant;
import java.util.UUID;

public record InvestigationResponse(
        UUID investigationId,
        UUID incidentId,
        IncidentStatus incidentStatus,
        UUID startedBy,
        Instant startedAt) {

    static InvestigationResponse from(InvestigationView view) {
        Investigation investigation = view.investigation();
        return new InvestigationResponse(
                investigation.id(),
                investigation.incidentId(),
                view.incidentStatus(),
                investigation.startedBy(),
                investigation.startedAt());
    }
}
