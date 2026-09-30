package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.Instant;
import java.util.UUID;

record SynTenPdfSourceDocument(
        String documentKey,
        UUID documentId,
        UUID tenantId,
        KnowledgeDocumentType type,
        String title,
        String version,
        String incidentFamily,
        String appliesTo,
        KnowledgeApprovalStatus approvalStatus,
        UUID approvedBy,
        Instant approvedAt,
        Instant effectiveAt,
        String classification,
        String replacement,
        String sourceName,
        String pdfName,
        String sourceSha256,
        String pdfSha256,
        int manifestPageCount,
        byte[] sourceBytes,
        byte[] pdfBytes,
        java.util.List<String> relatedDocumentKeys) {

    SynTenPdfSourceDocument(
            String documentKey,
            UUID documentId,
            UUID tenantId,
            KnowledgeDocumentType type,
            String title,
            String version,
            String incidentFamily,
            String appliesTo,
            KnowledgeApprovalStatus approvalStatus,
            UUID approvedBy,
            Instant approvedAt,
            Instant effectiveAt,
            String classification,
            String replacement,
            String sourceName,
            String pdfName,
            String sourceSha256,
            String pdfSha256,
            int manifestPageCount,
            byte[] sourceBytes,
            byte[] pdfBytes) {
        this(
                documentKey,
                documentId,
                tenantId,
                type,
                title,
                version,
                incidentFamily,
                appliesTo,
                approvalStatus,
                approvedBy,
                approvedAt,
                effectiveAt,
                classification,
                replacement,
                sourceName,
                pdfName,
                sourceSha256,
                pdfSha256,
                manifestPageCount,
                sourceBytes,
                pdfBytes,
                java.util.List.of());
    }

    SynTenPdfSourceDocument {
        relatedDocumentKeys = java.util.List.copyOf(relatedDocumentKeys);
        sourceBytes = sourceBytes.clone();
        pdfBytes = pdfBytes.clone();
    }

    @Override
    public byte[] sourceBytes() {
        return sourceBytes.clone();
    }

    @Override
    public byte[] pdfBytes() {
        return pdfBytes.clone();
    }
}
