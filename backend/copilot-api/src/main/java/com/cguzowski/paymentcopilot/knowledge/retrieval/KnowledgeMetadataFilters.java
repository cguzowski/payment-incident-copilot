package com.cguzowski.paymentcopilot.knowledge.retrieval;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import java.time.Instant;
import java.util.List;

public record KnowledgeMetadataFilters(
        String incidentFamily,
        List<KnowledgeDocumentType> documentTypes,
        KnowledgeApprovalStatus approvalStatus,
        Instant effectiveAt) {

    public KnowledgeMetadataFilters {
        documentTypes = List.copyOf(documentTypes);
    }
}
