package com.cguzowski.syntheticincidentgenerator.comparison;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class ComparisonController {
    private final ComparisonService service;

    ComparisonController(ComparisonService service) {
        this.service = service;
    }

    @PostMapping("/api/generations/{incidentId}/comparison")
    ComparisonResponse compare(
            @PathVariable String incidentId,
            @RequestHeader(name = "X-Synthetic-Operator-Id", required = false) String operatorId,
            @RequestBody(required = false) String body) {
        if (body != null) throw new InvalidComparisonRequest();
        return service.compare(uuid(incidentId), uuid(operatorId));
    }

    private static UUID uuid(String value) {
        if (value == null || value.isBlank()) throw new InvalidComparisonRequest();
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidComparisonRequest();
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    private static final class InvalidComparisonRequest extends RuntimeException {}
}
