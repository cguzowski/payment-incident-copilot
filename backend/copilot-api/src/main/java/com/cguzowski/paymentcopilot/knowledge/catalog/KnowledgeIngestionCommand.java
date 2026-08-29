package com.cguzowski.paymentcopilot.knowledge.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.knowledge.ingestion", name = "enabled", havingValue = "true")
class KnowledgeIngestionCommand implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeIngestionCommand.class);

    private final KnowledgeIngestionService ingestionService;

    KnowledgeIngestionCommand(KnowledgeIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        KnowledgeImportSummary summary = ingestionService.importApprovedSources();
        LOGGER.info(
                "Approved knowledge ingestion complete: importedDocuments={}, skippedDocuments={}, embeddedChunks={}",
                summary.importedDocuments(),
                summary.skippedDocuments(),
                summary.embeddedChunks());
    }
}
