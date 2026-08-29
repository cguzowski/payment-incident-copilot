package com.cguzowski.paymentcopilot.requestcontext;

import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class SyntheticRequestContextExceptionHandler {

    @ExceptionHandler(InvalidSyntheticRequestContextException.class)
    ResponseEntity<ProblemDetail> handle(InvalidSyntheticRequestContextException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("urn:problem:invalid-synthetic-request-context"));
        problem.setTitle("Invalid synthetic request context");
        problem.setDetail("The synthetic demonstration request context is missing or invalid.");
        problem.setProperty("errors", List.of(new ContextValidationError(exception.field(), exception.getMessage())));
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private record ContextValidationError(String field, String message) {}
}
