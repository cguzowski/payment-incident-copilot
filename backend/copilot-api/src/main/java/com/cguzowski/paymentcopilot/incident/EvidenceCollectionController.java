package com.cguzowski.paymentcopilot.incident;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class EvidenceCollectionController {

    private final EvidenceCollectionService service;

    EvidenceCollectionController(EvidenceCollectionService service) {
        this.service = service;
    }

    @PostMapping("/api/investigations/{investigationId}/evidence-collections")
    ResponseEntity<EvidenceCollectionResponse> collect(
            @PathVariable String investigationId,
            @RequestParam(required = false) String tenantId,
            @RequestBody(required = false) String requestBody) {
        if (requestBody != null) {
            throw new InvalidInvestigationRequestException("request", "must not include a body");
        }
        UUID parsedInvestigationId = parseRequiredUuid("investigationId", investigationId);
        EvidenceCollectionResponse response = service.collect(
                parseRequiredUuid("tenantId", tenantId),
                parsedInvestigationId);
        URI location = URI.create("/api/investigations/"
                + parsedInvestigationId
                + "/evidence-collections");
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/investigations/{investigationId}/evidence-collections")
    List<EvidenceCollectionResponse> history(
            @PathVariable String investigationId,
            @RequestParam(required = false) String tenantId) {
        return service.history(
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
