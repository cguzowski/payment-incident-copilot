package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SynTenPdfCatalogPersistenceService {

    private final SynTenPdfCatalogRepository repository;

    SynTenPdfCatalogPersistenceService(SynTenPdfCatalogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    PdfCatalogImportSummary importAll(List<PdfCatalogDocumentPlan> plans) {
        return repository.importAll(plans);
    }
}
