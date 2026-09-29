package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PdfKnowledgeArtifactController {

    private final PdfKnowledgeArtifactService service;

    PdfKnowledgeArtifactController(PdfKnowledgeArtifactService service) {
        this.service = service;
    }

    @GetMapping("/api/knowledge/pdf-artifacts/{sha256}")
    ResponseEntity<byte[]> get(@PathVariable String sha256) {
        PdfKnowledgeArtifact artifact = service.resolve(sha256);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(artifact.fileName())
                                .build()
                                .toString())
                .cacheControl(
                        CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(artifact.content());
    }
}
