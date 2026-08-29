package com.cguzowski.paymentcopilot.knowledge.catalog;

public final class KnowledgeEmbeddingMalformedException extends RuntimeException {
    public KnowledgeEmbeddingMalformedException() {
        super("Embedding output failed validation.");
    }
}
