package com.cguzowski.paymentcopilot.knowledge.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PdfKnowledgeArtifactController.class)
class PdfKnowledgeArtifactExceptionHandler {

    @ExceptionHandler(InvalidPdfKnowledgeArtifactHashException.class)
    ResponseEntity<ProblemDetail> invalidHash() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid PDF SHA-256.");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(PdfKnowledgeArtifactNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "PDF artifact not found.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
}

final class InvalidPdfKnowledgeArtifactHashException extends RuntimeException {}

final class PdfKnowledgeArtifactNotFoundException extends RuntimeException {}
