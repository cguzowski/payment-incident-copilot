package com.cguzowski.paymentcopilot.knowledge;

import java.time.Instant;
import java.util.UUID;

record KnowledgeSearchRequest(
        UUID tenantId,
        String incidentFamily,
        Instant effectiveAt,
        String queryText,
        float[] queryEmbedding,
        int candidateDepth,
        int rrfK,
        float minimumLexicalRank,
        float minimumVectorSimilarity) {

    KnowledgeSearchRequest {
        queryEmbedding = queryEmbedding == null ? null : queryEmbedding.clone();
    }

    @Override
    public float[] queryEmbedding() {
        return queryEmbedding == null ? null : queryEmbedding.clone();
    }
}
