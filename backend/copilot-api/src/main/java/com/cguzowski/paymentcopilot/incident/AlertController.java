package com.cguzowski.paymentcopilot.incident;

import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
class AlertController {

    private final AlertIngestionService alertIngestionService;
    private final SyntheticRequestContextResolver requestContext;

    AlertController(AlertIngestionService alertIngestionService, SyntheticRequestContextResolver requestContext) {
        this.alertIngestionService = alertIngestionService;
        this.requestContext = requestContext;
    }

    @PostMapping
    ResponseEntity<AlertResponse> ingest(HttpServletRequest httpRequest, @Valid @RequestBody AlertRequest request) {
        AlertIngestionResult result =
                alertIngestionService.ingest(request.toCommand(requestContext.tenantId(httpRequest)));
        AlertResponse response = AlertResponse.from(result.incident());
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
