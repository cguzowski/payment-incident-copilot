package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.util.Optional;

interface PdfKnowledgeArtifactRepository {

    Optional<PdfKnowledgeArtifact> findBySha256(String sha256);
}
