package com.cguzowski.paymentcopilot.evidence;

import com.cguzowski.paymentcopilot.incident.InvalidInvestigationRequestException;
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
class EvidenceCollectionController {

    private final EvidenceCollectionService service;
    private final SyntheticRequestContextResolver requestContext;

    EvidenceCollectionController(EvidenceCollectionService service, SyntheticRequestContextResolver requestContext) {
        this.service = service;
        this.requestContext = requestContext;
    }

    @PostMapping("/api/investigations/{investigationId}/evidence-collections")
    ResponseEntity<EvidenceCollectionResponse> collect(
            HttpServletRequest request,
            @PathVariable String investigationId,
            @RequestBody(required = false) String requestBody) {
        if (requestBody != null) {
            throw new InvalidInvestigationRequestException("request", "must not include a body");
        }
        UUID parsedInvestigationId = parseRequiredUuid("investigationId", investigationId);
        EvidenceCollectionResponse response = service.collect(requestContext.tenantId(request), parsedInvestigationId);
        URI location = URI.create("/api/investigations/" + parsedInvestigationId + "/evidence-collections");
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/investigations/{investigationId}/evidence-collections")
    List<EvidenceCollectionResponse> history(HttpServletRequest request, @PathVariable String investigationId) {
        return service.history(requestContext.tenantId(request), parseRequiredUuid("investigationId", investigationId));
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
