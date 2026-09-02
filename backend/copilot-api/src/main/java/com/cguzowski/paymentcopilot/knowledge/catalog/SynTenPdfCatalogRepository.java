package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.Instant;

interface SynTenPdfCatalogRepository {
    PdfCatalogImportSummary importAll(SynTenPdfCatalogPlan plan, Instant importedAt) throws IllegalArgumentException;
}
