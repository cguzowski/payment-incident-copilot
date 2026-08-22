package com.cguzowski.paymentcopilot.incident;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AlertQueueService {

    private final IncidentRepository incidentRepository;

    AlertQueueService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    List<AlertQueueSummary> getQueue(UUID tenantId) {
        return incidentRepository.findQueueByTenantId(tenantId).stream()
                .map(AlertQueueSummary::from)
                .toList();
    }
}
