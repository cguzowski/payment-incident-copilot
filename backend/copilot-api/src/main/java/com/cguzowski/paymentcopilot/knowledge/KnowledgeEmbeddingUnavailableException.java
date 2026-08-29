package com.cguzowski.paymentcopilot.knowledge;

final class KnowledgeEmbeddingUnavailableException extends RuntimeException {
    KnowledgeEmbeddingUnavailableException() {
        super("Embedding provider is unavailable.");
    }

    KnowledgeEmbeddingUnavailableException(Throwable cause) {
        super("Embedding provider is unavailable.", cause);
    }
}
