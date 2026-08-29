package com.cguzowski.paymentcopilot.report;

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
class ReportController {

    private final ReportGenerationService service;
    private final SyntheticRequestContextResolver requestContext;

    ReportController(ReportGenerationService service, SyntheticRequestContextResolver requestContext) {
        this.service = service;
        this.requestContext = requestContext;
    }

    @PostMapping("/api/investigations/{investigationId}/reports")
    ResponseEntity<ReportGenerationResponse> generate(
            HttpServletRequest request,
            @PathVariable String investigationId,
            @RequestBody(required = false) String requestBody) {
        if (requestBody != null) {
            throw new InvalidReportRequestException("request", "must not include a body");
        }
        UUID parsedInvestigationId = parseRequiredUuid(investigationId);
        UUID tenantId = requestContext.tenantId(request);
        UUID operatorId = requestContext.operatorId(request);
        ReportGenerationResponse response = service.generate(tenantId, parsedInvestigationId, operatorId);
        URI location = URI.create("/api/investigations/" + parsedInvestigationId + "/reports");
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/investigations/{investigationId}/reports")
    List<ReportGenerationResponse> history(HttpServletRequest request, @PathVariable String investigationId) {
        return service.history(requestContext.tenantId(request), parseRequiredUuid(investigationId));
    }

    private static UUID parseRequiredUuid(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidReportRequestException("investigationId", "is required");
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidReportRequestException("investigationId", "must be a valid UUID");
        }
    }
}
