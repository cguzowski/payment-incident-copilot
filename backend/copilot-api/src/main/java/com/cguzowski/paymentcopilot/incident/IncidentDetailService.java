package com.cguzowski.paymentcopilot.incident;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class IncidentDetailService {

    private final IncidentRepository incidentRepository;

    IncidentDetailService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    IncidentDetailResponse getDetail(UUID tenantId, UUID incidentId) {
        return incidentRepository.findViewByTenantIdAndIncidentId(tenantId, incidentId)
                .map(IncidentDetailResponse::from)
                .orElseThrow(IncidentNotFoundException::new);
    }
}
