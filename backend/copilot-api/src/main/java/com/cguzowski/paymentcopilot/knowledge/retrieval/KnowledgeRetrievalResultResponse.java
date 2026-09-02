package com.cguzowski.paymentcopilot.knowledge.retrieval;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeSourceFormat;
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
        String sourceName,
        KnowledgeSourceFormat sourceFormat,
        String pdfSha256,
        Integer sourceStartLine,
        Integer sourceEndLine,
        Integer sourceStartPage,
        Integer sourceEndPage,
        Integer sourceStartBlock,
        Integer sourceEndBlock,
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
                result.sourceName(),
                result.sourceFormat(),
                result.pdfSha256(),
                result.sourceStartLine(),
                result.sourceEndLine(),
                result.sourceStartPage(),
                result.sourceEndPage(),
                result.sourceStartBlock(),
                result.sourceEndBlock(),
                result.approvalStatus(),
                result.approvedBy(),
                result.approvedAt(),
                result.effectiveAt());
    }
}
