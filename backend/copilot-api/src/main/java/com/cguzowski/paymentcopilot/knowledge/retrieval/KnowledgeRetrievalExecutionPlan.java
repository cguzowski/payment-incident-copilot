package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.UUID;

record KnowledgeRetrievalExecutionPlan(
        UUID tenantId,
        DerivedKnowledgeQuery query,
        KnowledgeMetadataFilters filters,
        String rankingVersion,
        int rrfK,
        int candidateDepth,
        float minimumLexicalRank,
        float minimumVectorSimilarity) {}
