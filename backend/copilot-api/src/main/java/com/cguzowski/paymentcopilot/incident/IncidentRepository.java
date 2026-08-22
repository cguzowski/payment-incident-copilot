package com.cguzowski.paymentcopilot.incident;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface IncidentRepository {

    boolean insertIfAbsent(Incident incident);

    Optional<Incident> findByTenantIdAndExternalAlertId(UUID tenantId, String externalAlertId);

    List<Incident> findQueueByTenantId(UUID tenantId);
}
