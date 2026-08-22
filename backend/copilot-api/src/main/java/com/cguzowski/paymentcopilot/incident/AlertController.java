package com.cguzowski.paymentcopilot.incident;

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

    AlertController(AlertIngestionService alertIngestionService) {
        this.alertIngestionService = alertIngestionService;
    }

    @PostMapping
    ResponseEntity<AlertResponse> ingest(@Valid @RequestBody AlertRequest request) {
        AlertIngestionResult result = alertIngestionService.ingest(request.toCommand());
        AlertResponse response = AlertResponse.from(result.incident());
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
