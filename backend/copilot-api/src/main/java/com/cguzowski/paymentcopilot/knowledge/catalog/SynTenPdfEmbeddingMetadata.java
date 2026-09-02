package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.Instant;

record SynTenPdfEmbeddingMetadata(
        String modelId,
        Integer dimensions,
        Boolean normalized,
        Instant embeddedAt,
        Integer vectorDimensions,
        Double vectorNorm) {

    boolean isAbsent() {
        return modelId == null
                && dimensions == null
                && normalized == null
                && embeddedAt == null
                && vectorDimensions == null
                && vectorNorm == null;
    }

    boolean isComplete() {
        return modelId != null
                && dimensions != null
                && normalized != null
                && embeddedAt != null
                && vectorDimensions != null
                && vectorNorm != null;
    }

    boolean matchesCurrentModel() {
        return isComplete()
                && KnowledgeEmbeddingClient.MODEL_ID.equals(modelId)
                && dimensions == KnowledgeEmbeddingClient.DIMENSIONS
                && Boolean.TRUE.equals(normalized)
                && vectorDimensions == KnowledgeEmbeddingClient.DIMENSIONS
                && Double.isFinite(vectorNorm)
                && Math.abs(vectorNorm - 1.0d) <= 0.01d;
    }
}
