package com.cguzowski.paymentcopilot.incident;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
class IncidentDetailController {

    private final IncidentDetailService incidentDetailService;

    IncidentDetailController(IncidentDetailService incidentDetailService) {
        this.incidentDetailService = incidentDetailService;
    }

    @GetMapping("/{incidentId}")
    IncidentDetailResponse getDetail(
            @PathVariable String incidentId,
            @RequestParam(required = false) String tenantId) {
        return incidentDetailService.getDetail(
                parseRequiredUuid("tenantId", tenantId),
                parseRequiredUuid("incidentId", incidentId));
    }

    private static UUID parseRequiredUuid(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidIncidentRequestException(field, "is required");
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidIncidentRequestException(field, "must be a valid UUID");
        }
    }
}
