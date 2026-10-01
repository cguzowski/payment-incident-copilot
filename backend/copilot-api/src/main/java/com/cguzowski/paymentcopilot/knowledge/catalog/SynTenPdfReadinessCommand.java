package com.cguzowski.paymentcopilot.knowledge.catalog;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Read-only explicit check: validates exact source, persisted chunks and embedding tuples. */
@Component
@ConditionalOnProperty(prefix = "app.knowledge.pdf-readiness", name = "enabled", havingValue = "true")
class SynTenPdfReadinessCommand implements ApplicationRunner {
    private final SynTenPdfEmbeddingPlanService plans;

    SynTenPdfReadinessCommand(SynTenPdfEmbeddingPlanService plans) {
        this.plans = plans;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        SynTenPdfEmbeddingCatalogSnapshot snapshot;
        try {
            snapshot = plans.planBackfill();
        } catch (IllegalStateException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "PDF knowledge is not ready. Run start-local.bat -PrepareKnowledge. " + exception.getMessage(),
                    exception);
        }
        if (snapshot.state() != SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL) {
            throw new IllegalStateException("PDF knowledge is not ready. Run start-local.bat -PrepareKnowledge.");
        }
        org.slf4j.LoggerFactory.getLogger(getClass())
                .info(
                        "PDF knowledge ready: fingerprint={}, chunks={}",
                        snapshot.catalogFingerprint(),
                        snapshot.targets().size());
    }
}
