package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record KnowledgeRetrievalResponse(
        UUID retrievalId,
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
        List<KnowledgeRetrievalResultResponse> results) {

    public KnowledgeRetrievalResponse {
        contributingEvidenceIds = List.copyOf(contributingEvidenceIds);
        results = List.copyOf(results);
    }

    static KnowledgeRetrievalResponse from(KnowledgeRetrievalAttempt attempt) {
        return new KnowledgeRetrievalResponse(
                attempt.retrievalId(),
                attempt.status(),
                attempt.requestedAt(),
                attempt.completedAt(),
                attempt.queryText(),
                attempt.queryTemplateVersion(),
                attempt.contributingEvidenceIds(),
                attempt.embeddingModelId(),
                attempt.embeddingDimensions(),
                attempt.metadataFilters(),
                attempt.rankingVersion(),
                attempt.rrfK(),
                attempt.candidateDepth(),
                attempt.minimumLexicalRank(),
                attempt.minimumVectorSimilarity(),
                attempt.statusDetail(),
                attempt.results().stream()
                        .map(KnowledgeRetrievalResultResponse::from)
                        .toList());
    }
}
