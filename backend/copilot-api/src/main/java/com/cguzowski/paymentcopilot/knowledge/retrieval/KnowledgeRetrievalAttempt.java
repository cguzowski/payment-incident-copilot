package com.cguzowski.paymentcopilot.knowledge.retrieval;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingClient;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record KnowledgeRetrievalAttempt(
        UUID retrievalId,
        UUID tenantId,
        UUID investigationId,
        UUID investigationCorrelationId,
        UUID requestedBy,
        KnowledgeRetrievalStatus status,
        Instant requestedAt,
        Instant completedAt,
        String queryText,
        String queryTemplateVersion,
        List<UUID> contributingEvidenceIds,
        String embeddingModelId,
        int embeddingDimensions,
        KnowledgeMetadataFilters metadataFilters,
        String rankingVersion,
        int rrfK,
        int candidateDepth,
        float minimumLexicalRank,
        float minimumVectorSimilarity,
        String statusDetail,
        List<KnowledgeRetrievalResult> results) {

    KnowledgeRetrievalAttempt {
        contributingEvidenceIds = List.copyOf(contributingEvidenceIds);
        results = List.copyOf(results);
    }

    static KnowledgeRetrievalAttempt started(
            UUID retrievalId,
            KnowledgeRetrievalContext context,
            UUID requestedBy,
            Instant requestedAt,
            DerivedKnowledgeQuery query,
            KnowledgeMetadataFilters filters,
            String rankingVersion,
            int rrfK,
            int candidateDepth,
            float minimumLexicalRank,
            float minimumVectorSimilarity) {
        return new KnowledgeRetrievalAttempt(
                retrievalId,
                context.tenantId(),
                context.investigationId(),
                context.investigationCorrelationId(),
                requestedBy,
                KnowledgeRetrievalStatus.STARTED,
                requestedAt,
                null,
                query.text(),
                query.templateVersion(),
                query.contributingEvidenceIds(),
                KnowledgeEmbeddingClient.MODEL_ID,
                KnowledgeEmbeddingClient.DIMENSIONS,
                filters,
                rankingVersion,
                rrfK,
                candidateDepth,
                minimumLexicalRank,
                minimumVectorSimilarity,
                null,
                List.of());
    }

    KnowledgeRetrievalAttempt complete(
            KnowledgeRetrievalStatus terminalStatus,
            Instant completedAt,
            String statusDetail,
            List<SelectedKnowledgeChunk> selected) {
        if (status != KnowledgeRetrievalStatus.STARTED || !terminalStatus.isTerminal()) {
            throw new IllegalStateException("Only a started knowledge retrieval can be completed.");
        }
        return new KnowledgeRetrievalAttempt(
                retrievalId,
                tenantId,
                investigationId,
                investigationCorrelationId,
                requestedBy,
                terminalStatus,
                requestedAt,
                completedAt,
                queryText,
                queryTemplateVersion,
                contributingEvidenceIds,
                embeddingModelId,
                embeddingDimensions,
                metadataFilters,
                rankingVersion,
                rrfK,
                candidateDepth,
                minimumLexicalRank,
                minimumVectorSimilarity,
                statusDetail,
                selected.stream().map(KnowledgeRetrievalResult::from).toList());
    }
}
