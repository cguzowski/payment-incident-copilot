package com.cguzowski.paymentcopilot.knowledge;

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
