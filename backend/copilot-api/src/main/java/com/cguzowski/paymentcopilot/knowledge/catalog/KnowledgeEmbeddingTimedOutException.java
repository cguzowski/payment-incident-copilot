package com.cguzowski.paymentcopilot.knowledge.catalog;

public final class KnowledgeEmbeddingTimedOutException extends RuntimeException {
    public KnowledgeEmbeddingTimedOutException(Throwable cause) {
        super("Embedding provider request timed out.", cause);
    }
}
