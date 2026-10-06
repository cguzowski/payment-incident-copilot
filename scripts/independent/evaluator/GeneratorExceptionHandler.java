package com.cguzowski.syntheticincidentgenerator.generation;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GeneratorExceptionHandler {

    @ExceptionHandler(InvalidAnswerKeyRevealException.class)
    ProblemDetail invalidAnswerKeyReveal() {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "A valid incident ID and synthetic operator ID are required.");
        detail.setType(URI.create("urn:problem:invalid-answer-key-reveal"));
        detail.setTitle("Invalid answer-key reveal request");
        return detail;
    }

    @ExceptionHandler(AnswerKeyNotReadyException.class)
    ProblemDetail answerKeyNotReady() {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The answer key remains sealed until the incident has an approved or rejected human decision.");
        detail.setType(URI.create("urn:problem:answer-key-not-ready"));
        detail.setTitle("Answer key not ready");
        return detail;
    }

    @ExceptionHandler(AnswerKeyUnavailableException.class)
    ProblemDetail answerKeyUnavailable() {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, "The answer-key reveal could not be safely authorized.");
        detail.setType(URI.create("urn:problem:answer-key-unavailable"));
        detail.setTitle("Answer key unavailable");
        return detail;
    }
}
