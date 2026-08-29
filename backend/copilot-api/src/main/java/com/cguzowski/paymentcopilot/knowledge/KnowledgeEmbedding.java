package com.cguzowski.paymentcopilot.knowledge;

record KnowledgeEmbedding(String modelId, int dimensions, boolean normalized, float[] vector) {

    KnowledgeEmbedding {
        vector = vector.clone();
    }

    @Override
    public float[] vector() {
        return vector.clone();
    }
}
