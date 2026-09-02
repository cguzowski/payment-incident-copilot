package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SynTenPdfCatalogPersistenceService {

    private final SynTenPdfCatalogRepository repository;

    SynTenPdfCatalogPersistenceService(SynTenPdfCatalogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    PdfCatalogImportSummary importAll(SynTenPdfCatalogPlan plan, Instant importedAt) {
        return repository.importAll(plan, importedAt);
    }
}
