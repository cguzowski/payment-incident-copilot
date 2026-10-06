package com.cguzowski.syntheticincidentgenerator.generation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/generations")
public class IncidentGenerationController {
    private final IncidentGenerationService generationService;

    public IncidentGenerationController(IncidentGenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping
    ResponseEntity<GeneratedIncident> generate() {
        return ResponseEntity.status(HttpStatus.CREATED).body(generationService.generate());
    }
}
