package com.cguzowski.paymentcopilot.knowledge.catalog;

public interface KnowledgeEmbeddingClient {

    String MODEL_ID = "nomic-embed-text";
    int DIMENSIONS = 768;

    KnowledgeEmbedding embed(String input);
}
