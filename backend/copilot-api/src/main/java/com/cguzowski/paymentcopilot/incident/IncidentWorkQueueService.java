package com.cguzowski.paymentcopilot.incident;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class IncidentWorkQueueService {

    private final IncidentRepository incidentRepository;

    IncidentWorkQueueService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    List<IncidentWorkQueueItem> getQueue(UUID tenantId, IncidentQueueView view) {
        List<IncidentWorkQueueEntry> entries =
                switch (view) {
                    case ACTIVE -> incidentRepository.findActiveByTenantId(tenantId);
                    case COMPLETED -> incidentRepository.findCompletedByTenantId(tenantId);
                };
        return entries.stream().map(IncidentWorkQueueItem::from).toList();
    }
}
