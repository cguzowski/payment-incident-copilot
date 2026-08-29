package com.cguzowski.paymentcopilot.knowledge.retrieval;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import java.time.Instant;
import java.util.UUID;

record KnowledgeSearchCandidate(
        UUID tenantId,
        UUID chunkId,
        UUID documentVersionId,
        UUID documentId,
        KnowledgeDocumentType documentType,
        String documentTitle,
        String documentVersion,
        String incidentFamily,
        String appliesTo,
        String sectionPath,
        String rawContent,
        int sourceStartLine,
        int sourceEndLine,
        KnowledgeApprovalStatus approvalStatus,
        UUID approvedBy,
        Instant approvedAt,
        Instant effectiveAt,
        Float lexicalRank,
        Integer lexicalPosition,
        Float vectorSimilarity,
        Integer vectorPosition,
        double fusedScore) {}
