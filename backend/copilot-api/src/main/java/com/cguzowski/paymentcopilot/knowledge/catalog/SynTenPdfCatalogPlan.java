package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.util.List;
import java.util.UUID;

record SynTenPdfCatalogPlan(UUID tenantId, String catalogFingerprint, List<PdfCatalogDocumentPlan> documents) {

    SynTenPdfCatalogPlan {
        documents = List.copyOf(documents);
    }

    int chunkCount() {
        return documents.stream().mapToInt(document -> document.chunks().size()).sum();
    }

    List<SynTenPdfEmbeddingTarget> embeddingTargets() {
        return documents.stream()
                .flatMap(document -> document.chunks().stream()
                        .map(chunk -> new SynTenPdfEmbeddingTarget(
                                tenantId,
                                document.documentVersionId(),
                                document.document().source().documentKey(),
                                document.document().source().version(),
                                chunk.chunkId(),
                                chunk.ordinal(),
                                chunk.embeddingInput(),
                                chunk.embeddingInputHash(),
                                catalogFingerprint)))
                .sorted(java.util.Comparator.comparing(SynTenPdfEmbeddingTarget::documentKey)
                        .thenComparing(SynTenPdfEmbeddingTarget::documentVersion)
                        .thenComparingInt(SynTenPdfEmbeddingTarget::chunkOrdinal))
                .toList();
    }
}
