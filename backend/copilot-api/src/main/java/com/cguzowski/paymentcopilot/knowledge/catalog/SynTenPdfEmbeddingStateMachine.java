package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class SynTenPdfEmbeddingStateMachine {

    private SynTenPdfEmbeddingStateMachine() {}

    static SynTenPdfEmbeddingState classify(List<SynTenPdfEmbeddingMetadata> metadata) {
        if (metadata.isEmpty()) {
            throw new IllegalArgumentException("SynTen PDF embedding metadata is empty.");
        }
        if (metadata.stream().anyMatch(item -> !item.isAbsent() && !item.isComplete())) {
            return SynTenPdfEmbeddingState.INCOMPLETE;
        }

        List<SynTenPdfEmbeddingMetadata> complete =
                metadata.stream().filter(SynTenPdfEmbeddingMetadata::isComplete).toList();
        if (complete.stream().anyMatch(item -> !item.matchesCurrentModel())) {
            return SynTenPdfEmbeddingState.CONFLICTING;
        }
        if (complete.isEmpty()) {
            return SynTenPdfEmbeddingState.ABSENT;
        }
        if (complete.size() != metadata.size()) {
            return SynTenPdfEmbeddingState.MIXED;
        }

        Set<Instant> timestamps = new HashSet<>();
        complete.forEach(item -> timestamps.add(item.embeddedAt()));
        return timestamps.size() == 1
                ? SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL
                : SynTenPdfEmbeddingState.CONFLICTING;
    }
}
