package com.cguzowski.paymentcopilot.knowledge;

final class KnowledgeEmbeddingTimedOutException extends RuntimeException {
    KnowledgeEmbeddingTimedOutException(Throwable cause) {
        super("Embedding provider request timed out.", cause);
    }
}
