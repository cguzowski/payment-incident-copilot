package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.Instant;
import java.util.List;

interface SynTenPdfEmbeddingPersistence {
    SynTenPdfEmbeddingOperationSummary persist(List<PreparedSynTenPdfEmbedding> preparedEmbeddings, Instant embeddedAt)
            throws IllegalStateException;
}
