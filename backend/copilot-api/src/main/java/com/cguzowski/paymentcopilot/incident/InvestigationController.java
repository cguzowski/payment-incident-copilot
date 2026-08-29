package com.cguzowski.paymentcopilot.incident;

import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class InvestigationController {

    private final InvestigationService investigationService;
    private final SyntheticRequestContextResolver requestContext;

    InvestigationController(InvestigationService investigationService, SyntheticRequestContextResolver requestContext) {
        this.investigationService = investigationService;
        this.requestContext = requestContext;
    }

    @PostMapping("/api/incidents/{incidentId}/investigations")
    ResponseEntity<InvestigationResponse> start(
            HttpServletRequest request,
            @PathVariable String incidentId,
            @RequestBody(required = false) String requestBody) {
        if (requestBody != null) {
            throw new InvalidInvestigationRequestException("request", "must not include a body");
        }
        InvestigationStartResult result = investigationService.start(
                requestContext.tenantId(request),
                parseRequiredUuid("incidentId", incidentId),
                requestContext.operatorId(request));
        if (result.created()) {
            URI location = URI.create("/api/investigations/" + result.response().investigationId());
            return ResponseEntity.created(location).body(result.response());
        }
        return ResponseEntity.ok(result.response());
    }

    @GetMapping("/api/investigations/{investigationId}")
    InvestigationResponse get(HttpServletRequest request, @PathVariable String investigationId) {
        return investigationService.get(
                requestContext.tenantId(request), parseRequiredUuid("investigationId", investigationId));
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
