package com.cguzowski.paymentcopilot.knowledge;

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
class KnowledgeRetrievalController {

    private final KnowledgeRetrievalService service;

    KnowledgeRetrievalController(KnowledgeRetrievalService service) {
        this.service = service;
    }

    @PostMapping("/api/investigations/{investigationId}/knowledge-retrievals")
    ResponseEntity<KnowledgeRetrievalResponse> retrieve(
            @PathVariable String investigationId,
            @RequestParam(required = false) String tenantId,
            @RequestBody(required = false) String requestBody) {
        if (requestBody != null) {
            throw new InvalidKnowledgeRetrievalRequestException("request", "must not include a body");
        }
        UUID parsedInvestigationId = parseRequiredUuid("investigationId", investigationId);
        KnowledgeRetrievalResponse response = service.retrieve(
                parseRequiredUuid("tenantId", tenantId),
                parsedInvestigationId);
        URI location = URI.create("/api/investigations/"
                + parsedInvestigationId
                + "/knowledge-retrievals");
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/investigations/{investigationId}/knowledge-retrievals")
    List<KnowledgeRetrievalResponse> history(
            @PathVariable String investigationId,
            @RequestParam(required = false) String tenantId) {
        return service.history(
                parseRequiredUuid("tenantId", tenantId),
                parseRequiredUuid("investigationId", investigationId));
    }

    private static UUID parseRequiredUuid(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidKnowledgeRetrievalRequestException(field, "is required");
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidKnowledgeRetrievalRequestException(field, "must be a valid UUID");
        }
    }
}
