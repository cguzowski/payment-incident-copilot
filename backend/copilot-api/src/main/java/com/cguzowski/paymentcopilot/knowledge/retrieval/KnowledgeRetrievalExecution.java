package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.List;

record KnowledgeRetrievalExecution(
        DerivedKnowledgeQuery query,
        KnowledgeMetadataFilters filters,
        String rankingVersion,
        int rrfK,
        int candidateDepth,
        float minimumLexicalRank,
        float minimumVectorSimilarity,
        QueryEmbeddingOutcome embeddingOutcome,
        List<KnowledgeSearchCandidate> candidates,
        List<SelectedKnowledgeChunk> selected,
        KnowledgeRetrievalStatus status,
        String statusDetail) {

    KnowledgeRetrievalExecution {
        candidates = List.copyOf(candidates);
        selected = List.copyOf(selected);
    }
}
