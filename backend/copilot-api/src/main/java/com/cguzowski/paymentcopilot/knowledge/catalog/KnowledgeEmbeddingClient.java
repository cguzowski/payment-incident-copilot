package com.cguzowski.paymentcopilot.knowledge.catalog;

public interface KnowledgeEmbeddingClient {

    String MODEL_ID = "amazon.titan-embed-text-v2:0";
    int DIMENSIONS = 1024;

    KnowledgeEmbedding embed(String input);
}
