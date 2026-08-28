package com.cguzowski.paymentcopilot.incident;

import java.util.Optional;
import java.util.UUID;

interface InvestigationRepository {

    Optional<IncidentStatus> lockIncidentStatus(UUID tenantId, UUID incidentId);

    void insert(Investigation investigation);

    boolean transitionIncidentToInvestigating(UUID tenantId, UUID incidentId);

    Optional<InvestigationView> findByTenantIdAndIncidentId(UUID tenantId, UUID incidentId);

    Optional<InvestigationView> findByTenantIdAndInvestigationId(UUID tenantId, UUID investigationId);
}
