package com.cguzowski.paymentcopilot.knowledge.catalog;

record PreparedSynTenPdfEmbedding(
        SynTenPdfEmbeddingTarget target, String modelId, int dimensions, boolean normalized, float[] vector) {

    PreparedSynTenPdfEmbedding {
        vector = vector.clone();
    }

    @Override
    public float[] vector() {
        return vector.clone();
    }
}
