package com.cguzowski.paymentcopilot.knowledge.catalog;

public record KnowledgeEmbedding(String modelId, int dimensions, boolean normalized, float[] vector) {

    public KnowledgeEmbedding {
        vector = vector.clone();
    }

    @Override
    public float[] vector() {
        return vector.clone();
    }
}
