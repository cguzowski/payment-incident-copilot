package com.cguzowski.paymentcopilot.knowledge;

import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class KnowledgeApiExceptionHandler {

    @ExceptionHandler(InvalidKnowledgeRetrievalRequestException.class)
    ResponseEntity<ProblemDetail> handleInvalidRequest(InvalidKnowledgeRetrievalRequestException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("urn:problem:invalid-investigation-request"));
        problem.setTitle("Invalid investigation request");
        problem.setDetail("The investigation request contains invalid fields.");
        problem.setProperty("errors", List.of(new FieldValidationError(
                exception.field(), exception.getMessage())));
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(KnowledgeInvestigationNotFoundException.class)
    ResponseEntity<ProblemDetail> handleInvestigationNotFound() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setType(URI.create("urn:problem:investigation-not-found"));
        problem.setTitle("Investigation not found");
        problem.setDetail("No investigation was found for the requested tenant and investigation ID.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private record FieldValidationError(String field, String message) {
    }
}
