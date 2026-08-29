package com.cguzowski.paymentcopilot.knowledge.retrieval;

import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class KnowledgeRetrievalController {

    private final KnowledgeRetrievalService service;
    private final SyntheticRequestContextResolver requestContext;

    KnowledgeRetrievalController(KnowledgeRetrievalService service, SyntheticRequestContextResolver requestContext) {
        this.service = service;
        this.requestContext = requestContext;
    }

    @PostMapping("/api/investigations/{investigationId}/knowledge-retrievals")
    ResponseEntity<KnowledgeRetrievalResponse> retrieve(
            HttpServletRequest request,
            @PathVariable String investigationId,
            @RequestBody(required = false) String requestBody) {
        if (requestBody != null) {
            throw new InvalidKnowledgeRetrievalRequestException("request", "must not include a body");
        }
        UUID parsedInvestigationId = parseRequiredUuid("investigationId", investigationId);
        KnowledgeRetrievalResponse response = service.retrieve(requestContext.tenantId(request), parsedInvestigationId);
        URI location = URI.create("/api/investigations/" + parsedInvestigationId + "/knowledge-retrievals");
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/investigations/{investigationId}/knowledge-retrievals")
    List<KnowledgeRetrievalResponse> history(HttpServletRequest request, @PathVariable String investigationId) {
        return service.history(requestContext.tenantId(request), parseRequiredUuid("investigationId", investigationId));
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
