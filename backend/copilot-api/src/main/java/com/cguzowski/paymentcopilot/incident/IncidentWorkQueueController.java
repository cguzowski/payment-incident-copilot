package com.cguzowski.paymentcopilot.incident;

import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
class IncidentWorkQueueController {

    private final IncidentWorkQueueService incidentWorkQueueService;
    private final SyntheticRequestContextResolver requestContext;

    IncidentWorkQueueController(
            IncidentWorkQueueService incidentWorkQueueService, SyntheticRequestContextResolver requestContext) {
        this.incidentWorkQueueService = incidentWorkQueueService;
        this.requestContext = requestContext;
    }

    @GetMapping
    List<IncidentWorkQueueItem> getQueue(HttpServletRequest request) {
        return incidentWorkQueueService.getQueue(requestContext.tenantId(request));
    }
}
