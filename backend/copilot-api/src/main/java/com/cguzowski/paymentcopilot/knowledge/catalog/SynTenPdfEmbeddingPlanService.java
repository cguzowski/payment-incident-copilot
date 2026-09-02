package com.cguzowski.paymentcopilot.knowledge.catalog;

import org.springframework.stereotype.Service;

@Service
class SynTenPdfEmbeddingPlanService {

    private final SynTenPdfCatalogPlanner planner;
    private final SynTenPdfCatalogSnapshotRepository repository;

    SynTenPdfEmbeddingPlanService(SynTenPdfCatalogPlanner planner, SynTenPdfCatalogSnapshotRepository repository) {
        this.planner = planner;
        this.repository = repository;
    }

    SynTenPdfEmbeddingCatalogSnapshot planBackfill() {
        SynTenPdfCatalogPlan catalogPlan = planner.plan();
        SynTenPdfEmbeddingCatalogSnapshot snapshot = repository.readEmbeddingSnapshot(catalogPlan);
        if (!catalogPlan.catalogFingerprint().equals(snapshot.catalogFingerprint())) {
            throw new IllegalStateException("SynTen PDF embedding snapshot has a conflicting catalog fingerprint.");
        }
        if (snapshot.targets().size() != catalogPlan.chunkCount()) {
            throw new IllegalStateException("SynTen PDF embedding snapshot has a conflicting target count.");
        }
        if (snapshot.state() != SynTenPdfEmbeddingState.ABSENT
                && snapshot.state() != SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL) {
            throw new IllegalStateException(
                    "SynTen PDF embedding catalog state is not backfillable: " + snapshot.state() + ".");
        }
        return snapshot;
    }
}
