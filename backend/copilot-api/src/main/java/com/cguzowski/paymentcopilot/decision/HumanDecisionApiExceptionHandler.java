package com.cguzowski.paymentcopilot.decision;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = HumanDecisionController.class)
class HumanDecisionApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldValidationError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldValidationError::field))
                .toList();
        return invalid(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadable() {
        return invalid(List.of(new FieldValidationError("request", "contains malformed or unsupported data")));
    }

    @ExceptionHandler(InvalidHumanDecisionRequestException.class)
    ResponseEntity<ProblemDetail> handleInvalid(InvalidHumanDecisionRequestException exception) {
        return invalid(List.of(new FieldValidationError(exception.field(), exception.getMessage())));
    }

    @ExceptionHandler(DecisionInvestigationNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setType(URI.create("urn:problem:investigation-not-found"));
        problem.setTitle("Investigation not found");
        problem.setDetail("No investigation was found for the requested tenant and investigation ID.");
        return response(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(HumanDecisionConflictException.class)
    ResponseEntity<ProblemDetail> handleConflict(HumanDecisionConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setType(URI.create("urn:problem:human-decision-conflict"));
        problem.setTitle("Human decision conflict");
        problem.setDetail(exception.getMessage());
        return response(HttpStatus.CONFLICT, problem);
    }

    private static ResponseEntity<ProblemDetail> invalid(List<FieldValidationError> errors) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("urn:problem:invalid-human-decision"));
        problem.setTitle("Invalid human decision");
        problem.setDetail("The human decision request contains invalid fields.");
        problem.setProperty("errors", errors);
        return response(HttpStatus.BAD_REQUEST, problem);
    }

    private static ResponseEntity<ProblemDetail> response(HttpStatus status, ProblemDetail problem) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private record FieldValidationError(String field, String message) {}
}
