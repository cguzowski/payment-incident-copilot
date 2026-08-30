package com.cguzowski.paymentcopilot.audit;

import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AuditTimelineController {

    private final AuditTimelineService service;
    private final SyntheticRequestContextResolver requestContext;

    AuditTimelineController(AuditTimelineService service, SyntheticRequestContextResolver requestContext) {
        this.service = service;
        this.requestContext = requestContext;
    }

    @GetMapping("/api/investigations/{investigationId}/timeline")
    List<AuditTimelineEvent> timeline(HttpServletRequest request, @PathVariable String investigationId) {
        return service.timeline(requestContext.tenantId(request), parseInvestigationId(investigationId));
    }

    private static UUID parseInvestigationId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidAuditTimelineRequestException();
        }
    }
}
