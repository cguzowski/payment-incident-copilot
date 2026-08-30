package com.cguzowski.paymentcopilot.report;

import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ReportApiExceptionHandler {

    @ExceptionHandler(InvalidReportRequestException.class)
    ResponseEntity<ProblemDetail> handleInvalidRequest(InvalidReportRequestException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("urn:problem:invalid-report-request"));
        problem.setTitle("Invalid report request");
        problem.setDetail("The report request contains invalid fields.");
        problem.setProperty("errors", List.of(new FieldValidationError(exception.field(), exception.getMessage())));
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(ReportInvestigationNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setType(URI.create("urn:problem:investigation-not-found"));
        problem.setTitle("Investigation not found");
        problem.setDetail("No investigation was found for the requested tenant and investigation ID.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(ReportGenerationConflictException.class)
    ResponseEntity<ProblemDetail> handleConflict(ReportGenerationConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setType(URI.create("urn:problem:report-generation-conflict"));
        problem.setTitle("Report generation conflict");
        problem.setDetail(exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private record FieldValidationError(String field, String message) {}
}
