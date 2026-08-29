package com.cguzowski.paymentcopilot.knowledge.catalog;

public final class KnowledgeEmbeddingUnavailableException extends RuntimeException {
    public KnowledgeEmbeddingUnavailableException() {
        super("Embedding provider is unavailable.");
    }

    public KnowledgeEmbeddingUnavailableException(Throwable cause) {
        super("Embedding provider is unavailable.", cause);
    }
}
