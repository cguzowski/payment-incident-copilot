package com.cguzowski.paymentcopilot.decision;

import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
class HumanDecisionController {

    private final HumanDecisionService service;
    private final SyntheticRequestContextResolver requestContext;

    HumanDecisionController(HumanDecisionService service, SyntheticRequestContextResolver requestContext) {
        this.service = service;
        this.requestContext = requestContext;
    }

    @PostMapping("/api/investigations/{investigationId}/decisions")
    ResponseEntity<HumanDecisionResponse> record(
            HttpServletRequest httpRequest,
            @PathVariable String investigationId,
            @Valid @RequestBody HumanDecisionRequest request) {
        UUID parsedInvestigationId = parseRequiredUuid(investigationId);
        HumanDecisionRecordResult result = service.record(
                requestContext.tenantId(httpRequest),
                parsedInvestigationId,
                requestContext.operatorId(httpRequest),
                request.outcome(),
                request.reason());
        HumanDecisionResponse response = HumanDecisionResponse.from(result.decision());
        if (!result.created()) {
            return ResponseEntity.ok(response);
        }
        URI location = URI.create("/api/investigations/" + parsedInvestigationId + "/decisions");
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/investigations/{investigationId}/decisions")
    List<HumanDecisionResponse> history(HttpServletRequest request, @PathVariable String investigationId) {
        return service.history(requestContext.tenantId(request), parseRequiredUuid(investigationId)).stream()
                .map(HumanDecisionResponse::from)
                .toList();
    }

    private static UUID parseRequiredUuid(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidHumanDecisionRequestException("investigationId", "is required");
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidHumanDecisionRequestException("investigationId", "must be a valid UUID");
        }
    }
}
