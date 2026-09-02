package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class SynTenPdfCatalogImportService {

    private final SynTenPdfCatalogPlanner planner;
    private final SynTenPdfCatalogPersistenceService persistence;
    private final Clock clock;

    SynTenPdfCatalogImportService(
            SynTenPdfCatalogPlanner planner, SynTenPdfCatalogPersistenceService persistence, Clock clock) {
        this.planner = planner;
        this.persistence = persistence;
        this.clock = clock;
    }

    public PdfCatalogImportSummary importCorpus() {
        Instant importedAt = Instant.now(clock);
        return persistence.importAll(planner.plan(), importedAt);
    }
}
