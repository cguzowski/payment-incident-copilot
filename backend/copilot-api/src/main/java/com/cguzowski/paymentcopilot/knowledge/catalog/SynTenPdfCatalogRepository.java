package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.util.List;

interface SynTenPdfCatalogRepository {
    PdfCatalogImportSummary importAll(List<PdfCatalogDocumentPlan> plans);
}
