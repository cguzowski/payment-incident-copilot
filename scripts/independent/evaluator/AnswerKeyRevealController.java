package com.cguzowski.syntheticincidentgenerator.generation;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
public class AnswerKeyRevealController {
    private final AnswerKeyRevealService service;

    public AnswerKeyRevealController(AnswerKeyRevealService service) {
        this.service = service;
    }

    @PostMapping("/api/generations/{incidentId}/answer-key")
    AnswerKeyRevealResponse reveal(
            @PathVariable String incidentId,
            @RequestHeader(name = "X-Synthetic-Operator-Id", required = false) String operatorId) {
        return service.reveal(uuid(incidentId), uuid(operatorId));
    }

    private static UUID uuid(String value) {
        if (value == null || value.isBlank()) throw new InvalidAnswerKeyRevealException();
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidAnswerKeyRevealException();
        }
    }
}
