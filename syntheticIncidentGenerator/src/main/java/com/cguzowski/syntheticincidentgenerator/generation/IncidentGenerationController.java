package com.cguzowski.syntheticincidentgenerator.generation;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/generations")
public class IncidentGenerationController {

    private final IncidentGenerationService generationService;
    private final AnswerKeyRevealService revealService;

    public IncidentGenerationController(
            IncidentGenerationService generationService, AnswerKeyRevealService revealService) {
        this.generationService = generationService;
        this.revealService = revealService;
    }

    @PostMapping
    ResponseEntity<GeneratedIncident> generate() {
        return ResponseEntity.status(HttpStatus.CREATED).body(generationService.generate());
    }

    @PostMapping("/{incidentId}/answer-key")
    AnswerKeyRevealResponse reveal(
            @PathVariable String incidentId,
            @RequestHeader(name = "X-Synthetic-Operator-Id", required = false) String operatorId) {
        return revealService.reveal(requiredUuid(incidentId), requiredUuid(operatorId));
    }

    private static UUID requiredUuid(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidAnswerKeyRevealException();
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidAnswerKeyRevealException();
        }
    }
}
