package com.cguzowski.syntheticincidentgenerator.generation;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GeneratorExceptionHandler {

    @ExceptionHandler(AlertIntakeException.class)
    ProblemDetail alertIntakeUnavailable() {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, "The synthetic alert was not accepted by the copilot API.");
        detail.setType(URI.create("urn:problem:copilot-alert-intake-unavailable"));
        detail.setTitle("Copilot alert intake unavailable");
        return detail;
    }
}
