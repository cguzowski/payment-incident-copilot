package com.cguzowski.paymentcopilot.knowledge.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.knowledge.pdf-backfill", name = "enabled", havingValue = "true")
class SynTenPdfEmbeddingBackfillCommand implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SynTenPdfEmbeddingBackfillCommand.class);

    private final SynTenPdfEmbeddingService embeddingService;

    SynTenPdfEmbeddingBackfillCommand(SynTenPdfEmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        SynTenPdfEmbeddingOperationSummary summary = embeddingService.backfill();
        LOGGER.info(
                "SynTen PDF embedding backfill complete: catalogFingerprint={}, initialState={}, targetChunks={}, alreadyEmbeddedChunks={}, noOp={}",
                summary.catalogFingerprint(),
                summary.initialState(),
                summary.targetChunks(),
                summary.alreadyEmbeddedChunks(),
                summary.noOp());
    }
}
