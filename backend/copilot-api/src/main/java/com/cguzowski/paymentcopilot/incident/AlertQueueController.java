package com.cguzowski.paymentcopilot.incident;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/alert-queue")
class AlertQueueController {

    private final AlertQueueService alertQueueService;

    AlertQueueController(AlertQueueService alertQueueService) {
        this.alertQueueService = alertQueueService;
    }

    @GetMapping
    List<AlertQueueSummary> getQueue(@PathVariable UUID tenantId) {
        return alertQueueService.getQueue(tenantId);
    }
}
