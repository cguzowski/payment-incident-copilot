package com.cguzowski.syntheticincidentgenerator.generation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/generations")
public class IncidentGenerationController {

    private final IncidentGenerationService service;

    public IncidentGenerationController(IncidentGenerationService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<GeneratedIncident> generate() {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.generate());
    }
}
