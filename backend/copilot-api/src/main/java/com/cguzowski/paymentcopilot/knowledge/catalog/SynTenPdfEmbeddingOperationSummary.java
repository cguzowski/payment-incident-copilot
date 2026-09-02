package com.cguzowski.paymentcopilot.knowledge.catalog;

record SynTenPdfEmbeddingOperationSummary(
        String catalogFingerprint,
        SynTenPdfEmbeddingState initialState,
        int targetChunks,
        int alreadyEmbeddedChunks,
        boolean noOp) {}
