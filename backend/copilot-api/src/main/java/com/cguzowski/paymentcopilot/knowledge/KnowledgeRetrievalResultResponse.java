package com.cguzowski.paymentcopilot.knowledge;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeRetrievalResultResponse(
        UUID chunkId,
        UUID documentVersionId,
        UUID documentId,
        int selectedPosition,
        Float lexicalRank,
        Integer lexicalPosition,
        Float vectorSimilarity,
        Float vectorDistance,
        Integer vectorPosition,
        int fusedPosition,
        double fusedScore,
        KnowledgeDocumentType documentType,
        String documentTitle,
        String documentVersion,
        String appliesTo,
        String sectionPath,
        String rawContent,
        int sourceStartLine,
        int sourceEndLine,
        KnowledgeApprovalStatus approvalStatus,
        UUID approvedBy,
        Instant approvedAt,
        Instant effectiveAt) {

    static KnowledgeRetrievalResultResponse from(KnowledgeRetrievalResult result) {
        return new KnowledgeRetrievalResultResponse(
                result.chunkId(),
                result.documentVersionId(),
                result.documentId(),
                result.selectedPosition(),
                result.lexicalRank(),
                result.lexicalPosition(),
                result.vectorSimilarity(),
                result.vectorDistance(),
                result.vectorPosition(),
                result.fusedPosition(),
                result.fusedScore(),
                result.documentType(),
                result.documentTitle(),
                result.documentVersion(),
                result.appliesTo(),
                result.sectionPath(),
                result.rawContent(),
                result.sourceStartLine(),
                result.sourceEndLine(),
                result.approvalStatus(),
                result.approvedBy(),
                result.approvedAt(),
                result.effectiveAt());
    }
}
