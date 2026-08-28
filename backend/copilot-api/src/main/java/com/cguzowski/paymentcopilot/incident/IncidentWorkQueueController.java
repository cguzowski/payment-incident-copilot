package com.cguzowski.paymentcopilot.incident;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/incidents")
class IncidentWorkQueueController {

    private final IncidentWorkQueueService incidentWorkQueueService;

    IncidentWorkQueueController(IncidentWorkQueueService incidentWorkQueueService) {
        this.incidentWorkQueueService = incidentWorkQueueService;
    }

    @GetMapping
    List<IncidentWorkQueueItem> getQueue(@PathVariable String tenantId) {
        return incidentWorkQueueService.getQueue(parseTenantId(tenantId));
    }

    private static UUID parseTenantId(String tenantId) {
        try {
            return UUID.fromString(tenantId.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidIncidentRequestException("tenantId", "must be a valid UUID");
        }
    }
}
