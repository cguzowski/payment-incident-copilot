package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.util.UUID;

record SynTenPdfEmbeddingTarget(
        UUID tenantId,
        UUID documentVersionId,
        String documentKey,
        String documentVersion,
        UUID chunkId,
        int chunkOrdinal,
        String embeddingInput,
        String embeddingInputHash,
        String catalogFingerprint) {}
