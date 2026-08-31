package com.cguzowski.paymentcopilot.knowledge.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.knowledge.pdf-catalog", name = "enabled", havingValue = "true")
class SynTenPdfCatalogImportCommand implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SynTenPdfCatalogImportCommand.class);

    private final SynTenPdfCatalogImportService importService;

    SynTenPdfCatalogImportCommand(SynTenPdfCatalogImportService importService) {
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        PdfCatalogImportSummary summary = importService.importCorpus();
        LOGGER.info(
                "SynTen PDF catalog import complete: importedDocuments={}, skippedDocuments={}, cataloguedChunks={}",
                summary.importedDocuments(),
                summary.skippedDocuments(),
                summary.cataloguedChunks());
    }
}
