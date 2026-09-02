package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.util.List;
import java.util.UUID;

record PdfCatalogDocumentPlan(
        UUID documentVersionId,
        PdfKnowledgeDocument document,
        String catalogContentHash,
        List<PdfKnowledgeChunkDraft> chunks) {

    PdfCatalogDocumentPlan {
        chunks = List.copyOf(chunks);
    }
}
