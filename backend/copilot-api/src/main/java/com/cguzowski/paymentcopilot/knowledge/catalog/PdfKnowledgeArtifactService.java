package com.cguzowski.paymentcopilot.knowledge.catalog;

import org.springframework.stereotype.Service;

@Service
class PdfKnowledgeArtifactService {

    private final PdfKnowledgeArtifactRepository repository;

    PdfKnowledgeArtifactService(PdfKnowledgeArtifactRepository repository) {
        this.repository = repository;
    }

    PdfKnowledgeArtifact resolve(String sha256) {
        if (sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
            throw new InvalidPdfKnowledgeArtifactHashException();
        }
        return repository.findBySha256(sha256).orElseThrow(PdfKnowledgeArtifactNotFoundException::new);
    }
}
