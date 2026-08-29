package com.cguzowski.paymentcopilot.incident;

import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
class IncidentDetailController {

    private final IncidentDetailService incidentDetailService;
    private final SyntheticRequestContextResolver requestContext;

    IncidentDetailController(
            IncidentDetailService incidentDetailService, SyntheticRequestContextResolver requestContext) {
        this.incidentDetailService = incidentDetailService;
        this.requestContext = requestContext;
    }

    @GetMapping("/{incidentId}")
    IncidentDetailResponse getDetail(HttpServletRequest request, @PathVariable String incidentId) {
        return incidentDetailService.getDetail(
                requestContext.tenantId(request), parseRequiredUuid("incidentId", incidentId));
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
