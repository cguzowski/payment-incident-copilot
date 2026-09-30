package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.UUID;

record KnowledgeRankingEvidence(int exactSignalMatches, KnowledgeRelationshipEvidence relationship) {}

record KnowledgeRelationshipEvidence(
        UUID anchorVersionId,
        String anchorDocumentKey,
        String anchorSourceSha256,
        UUID intermediateVersionId,
        String intermediateDocumentKey,
        String intermediateSourceSha256,
        String targetDocumentKey,
        int anchorPosition,
        int hops,
        String metadataVersion) {}
