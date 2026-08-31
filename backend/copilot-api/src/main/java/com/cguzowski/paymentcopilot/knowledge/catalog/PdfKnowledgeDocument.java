package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.util.List;

record PdfKnowledgeDocument(
        SynTenPdfSourceDocument source,
        String pdfSha256,
        String extractionStrategyVersion,
        List<PdfKnowledgePage> pages) {

    PdfKnowledgeDocument {
        pages = List.copyOf(pages);
    }
}
