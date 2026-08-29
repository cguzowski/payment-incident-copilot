package com.cguzowski.paymentcopilot.knowledge;

final class KnowledgeEmbeddingMalformedException extends RuntimeException {
    KnowledgeEmbeddingMalformedException() {
        super("Embedding output failed validation.");
    }
}
