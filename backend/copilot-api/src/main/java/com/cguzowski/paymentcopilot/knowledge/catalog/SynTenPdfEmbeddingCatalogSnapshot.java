package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.Instant;
import java.util.List;

record SynTenPdfEmbeddingCatalogSnapshot(
        String catalogFingerprint,
        SynTenPdfEmbeddingState state,
        List<SynTenPdfEmbeddingTarget> targets,
        Instant embeddedAt) {

    SynTenPdfEmbeddingCatalogSnapshot {
        targets = List.copyOf(targets);
    }

    SynTenPdfEmbeddingOperationSummary operationSummary() {
        boolean noOp = state == SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL;
        return new SynTenPdfEmbeddingOperationSummary(
                catalogFingerprint, state, targets.size(), noOp ? targets.size() : 0, noOp);
    }
}
