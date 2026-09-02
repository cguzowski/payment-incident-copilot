package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class SynTenPdfEmbeddingService {

    private final SynTenPdfEmbeddingPlanService planService;
    private final KnowledgeEmbeddingClient embeddingClient;
    private final SynTenPdfEmbeddingCallExecutor callExecutor;
    private final SynTenPdfEmbeddingPersistence persistence;
    private final Clock clock;

    SynTenPdfEmbeddingService(
            SynTenPdfEmbeddingPlanService planService,
            KnowledgeEmbeddingClient embeddingClient,
            SynTenPdfEmbeddingCallExecutor callExecutor,
            SynTenPdfEmbeddingPersistence persistence,
            Clock clock) {
        this.planService = planService;
        this.embeddingClient = embeddingClient;
        this.callExecutor = callExecutor;
        this.persistence = persistence;
        this.clock = clock;
    }

    SynTenPdfEmbeddingOperationSummary backfill() {
        SynTenPdfEmbeddingCatalogSnapshot snapshot = planService.planBackfill();
        if (snapshot.state() == SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL) {
            return snapshot.operationSummary();
        }
        List<PreparedSynTenPdfEmbedding> prepared = callExecutor.prepare(snapshot.targets(), embeddingClient);
        Instant embeddedAt = clock.instant();
        return persistence.persist(prepared, embeddedAt);
    }
}
