package com.cguzowski.paymentcopilot.knowledge.catalog;

interface SynTenPdfCatalogSnapshotRepository {
    SynTenPdfEmbeddingCatalogSnapshot readEmbeddingSnapshot(SynTenPdfCatalogPlan expectedPlan)
            throws IllegalStateException;
}
