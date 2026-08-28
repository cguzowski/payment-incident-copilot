package com.cguzowski.paymentcopilot.incident;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class InvestigationController {

    private final InvestigationService investigationService;

    InvestigationController(InvestigationService investigationService) {
        this.investigationService = investigationService;
    }

    @PostMapping("/api/incidents/{incidentId}/investigations")
    ResponseEntity<InvestigationResponse> start(
            @PathVariable String incidentId,
            @RequestParam(required = false) String tenantId,
            @RequestBody InvestigationStartRequest request) {
        InvestigationStartResult result = investigationService.start(
                parseRequiredUuid("tenantId", tenantId),
                parseRequiredUuid("incidentId", incidentId),
                parseRequiredUuid("operatorId", request.operatorId()));
        if (result.created()) {
            URI location = URI.create("/api/investigations/" + result.response().investigationId());
            return ResponseEntity.created(location).body(result.response());
        }
        return ResponseEntity.ok(result.response());
    }

    @GetMapping("/api/investigations/{investigationId}")
    InvestigationResponse get(
            @PathVariable String investigationId,
            @RequestParam(required = false) String tenantId) {
        return investigationService.get(
                parseRequiredUuid("tenantId", tenantId),
                parseRequiredUuid("investigationId", investigationId));
    }

    private static UUID parseRequiredUuid(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidInvestigationRequestException(field, "is required");
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidInvestigationRequestException(field, "must be a valid UUID");
        }
    }
}
