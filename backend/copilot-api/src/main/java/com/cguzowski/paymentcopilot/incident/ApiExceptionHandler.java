package com.cguzowski.paymentcopilot.incident;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleInvalidAlert(MethodArgumentNotValidException exception) {
        List<FieldValidationError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldValidationError::field))
                .toList();

        return invalidAlert(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableRequest(HttpServletRequest request) {
        if (request.getRequestURI().endsWith("/investigations")) {
            return handleInvalidInvestigationRequest(new InvalidInvestigationRequestException(
                    "request", "contains malformed or unsupported data"));
        }
        return invalidAlert(List.of(new FieldValidationError("request", "contains malformed or unsupported data")));
    }

    @ExceptionHandler(InvalidIncidentRequestException.class)
    ResponseEntity<ProblemDetail> handleInvalidIncidentRequest(InvalidIncidentRequestException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("urn:problem:invalid-incident-request"));
        problem.setTitle("Invalid incident request");
        problem.setDetail("The incident detail request contains invalid fields.");
        problem.setProperty("errors", List.of(new FieldValidationError(exception.field(), exception.getMessage())));

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(IncidentNotFoundException.class)
    ResponseEntity<ProblemDetail> handleIncidentNotFound() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setType(URI.create("urn:problem:incident-not-found"));
        problem.setTitle("Incident not found");
        problem.setDetail("No incident was found for the requested tenant and incident ID.");

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(InvalidInvestigationRequestException.class)
    ResponseEntity<ProblemDetail> handleInvalidInvestigationRequest(
            InvalidInvestigationRequestException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("urn:problem:invalid-investigation-request"));
        problem.setTitle("Invalid investigation request");
        problem.setDetail("The investigation request contains invalid fields.");
        problem.setProperty("errors", List.of(new FieldValidationError(exception.field(), exception.getMessage())));

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(InvestigationNotFoundException.class)
    ResponseEntity<ProblemDetail> handleInvestigationNotFound() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setType(URI.create("urn:problem:investigation-not-found"));
        problem.setTitle("Investigation not found");
        problem.setDetail("No investigation was found for the requested tenant and investigation ID.");

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(InvestigationConflictException.class)
    ResponseEntity<ProblemDetail> handleInvestigationConflict() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setType(URI.create("urn:problem:investigation-state-conflict"));
        problem.setTitle("Investigation state conflict");
        problem.setDetail("The incident cannot start or resume an investigation in its current state.");

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private ResponseEntity<ProblemDetail> invalidAlert(List<FieldValidationError> errors) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("urn:problem:invalid-alert"));
        problem.setTitle("Invalid alert");
        problem.setDetail("The alert request contains invalid fields.");
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private record FieldValidationError(String field, String message) {
    }
}
