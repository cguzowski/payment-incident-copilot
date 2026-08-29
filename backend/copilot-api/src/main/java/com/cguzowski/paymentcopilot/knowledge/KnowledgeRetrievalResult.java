package com.cguzowski.paymentcopilot.knowledge;

import java.time.Instant;
import java.util.UUID;

record KnowledgeRetrievalResult(
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

    static KnowledgeRetrievalResult from(SelectedKnowledgeChunk selected) {
        KnowledgeSearchCandidate candidate = selected.candidate();
        Float distance = candidate.vectorSimilarity() == null ? null : 1.0f - candidate.vectorSimilarity();
        return new KnowledgeRetrievalResult(
                candidate.chunkId(),
                candidate.documentVersionId(),
                candidate.documentId(),
                selected.selectedPosition(),
                candidate.lexicalRank(),
                candidate.lexicalPosition(),
                candidate.vectorSimilarity(),
                distance,
                candidate.vectorPosition(),
                selected.fusedPosition(),
                candidate.fusedScore(),
                candidate.documentType(),
                candidate.documentTitle(),
                candidate.documentVersion(),
                candidate.appliesTo(),
                candidate.sectionPath(),
                candidate.rawContent(),
                candidate.sourceStartLine(),
                candidate.sourceEndLine(),
                candidate.approvalStatus(),
                candidate.approvedBy(),
                candidate.approvedAt(),
                candidate.effectiveAt());
    }
}
