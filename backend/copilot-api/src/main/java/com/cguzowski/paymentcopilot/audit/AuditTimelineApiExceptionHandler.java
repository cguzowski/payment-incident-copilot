package com.cguzowski.paymentcopilot.audit;

import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuditTimelineController.class)
class AuditTimelineApiExceptionHandler {

    @ExceptionHandler(InvalidAuditTimelineRequestException.class)
    ResponseEntity<ProblemDetail> handleInvalidTimeline(InvalidAuditTimelineRequestException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("urn:problem:invalid-audit-timeline-request"));
        problem.setTitle("Invalid audit timeline request");
        problem.setDetail("The audit timeline request contains invalid fields.");
        problem.setProperty("errors", List.of(new TimelineFieldError("investigationId", exception.getMessage())));
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(AuditTimelineNotFoundException.class)
    ResponseEntity<ProblemDetail> handleTimelineNotFound() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setType(URI.create("urn:problem:investigation-not-found"));
        problem.setTitle("Investigation not found");
        problem.setDetail("No investigation was found for the requested tenant and investigation ID.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private record TimelineFieldError(String field, String message) {}
}
