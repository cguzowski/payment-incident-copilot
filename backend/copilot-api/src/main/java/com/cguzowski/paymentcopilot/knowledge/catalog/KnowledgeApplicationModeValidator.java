package com.cguzowski.paymentcopilot.knowledge.catalog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class KnowledgeApplicationModeValidator {

    KnowledgeApplicationModeValidator(
            @Value("${app.knowledge.ingestion.enabled:false}") boolean ingestionEnabled,
            @Value("${app.knowledge.pdf-catalog.enabled:false}") boolean pdfCatalogEnabled,
            @Value("${app.knowledge.pdf-backfill.enabled:false}") boolean pdfBackfillEnabled,
            @Value("${app.knowledge.retrieval-evaluation.enabled:false}") boolean evaluationEnabled,
            @Value("${app.knowledge.embedding-smoke-test.enabled:false}") boolean embeddingSmokeTestEnabled) {
        int enabledModes = countEnabled(
                ingestionEnabled, pdfCatalogEnabled, pdfBackfillEnabled, evaluationEnabled, embeddingSmokeTestEnabled);
        if (enabledModes > 1) {
            throw new IllegalStateException("Only one explicit knowledge command mode may be enabled at a time.");
        }
    }

    private static int countEnabled(boolean... modes) {
        int count = 0;
        for (boolean enabled : modes) {
            if (enabled) {
                count++;
            }
        }
        return count;
    }
}
