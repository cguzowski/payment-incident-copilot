package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SynTenPdfEmbeddingStateMachineTest {

    private static final Instant EMBEDDED_AT = Instant.parse("2026-08-31T20:00:00Z");

    @Test
    void classifiesEveryClosedCatalogEmbeddingState() {
        assertThat(SynTenPdfEmbeddingStateMachine.classify(List.of(absent(), absent())))
                .isEqualTo(SynTenPdfEmbeddingState.ABSENT);
        assertThat(SynTenPdfEmbeddingStateMachine.classify(List.of(current(), current())))
                .isEqualTo(SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL);
        assertThat(SynTenPdfEmbeddingStateMachine.classify(List.of(absent(), current())))
                .isEqualTo(SynTenPdfEmbeddingState.MIXED);
        assertThat(SynTenPdfEmbeddingStateMachine.classify(List.of(absent(), incomplete())))
                .isEqualTo(SynTenPdfEmbeddingState.INCOMPLETE);
        assertThat(SynTenPdfEmbeddingStateMachine.classify(List.of(absent(), conflicting())))
                .isEqualTo(SynTenPdfEmbeddingState.CONFLICTING);
    }

    @Test
    void treatsDifferentCompletionTimestampsAsConflicting() {
        SynTenPdfEmbeddingMetadata later = new SynTenPdfEmbeddingMetadata(
                KnowledgeEmbeddingClient.MODEL_ID,
                KnowledgeEmbeddingClient.DIMENSIONS,
                true,
                EMBEDDED_AT.plusSeconds(1),
                KnowledgeEmbeddingClient.DIMENSIONS,
                1.0d);

        assertThat(SynTenPdfEmbeddingStateMachine.classify(List.of(current(), later)))
                .isEqualTo(SynTenPdfEmbeddingState.CONFLICTING);
    }

    private static SynTenPdfEmbeddingMetadata absent() {
        return new SynTenPdfEmbeddingMetadata(null, null, null, null, null, null);
    }

    private static SynTenPdfEmbeddingMetadata current() {
        return new SynTenPdfEmbeddingMetadata(
                KnowledgeEmbeddingClient.MODEL_ID,
                KnowledgeEmbeddingClient.DIMENSIONS,
                true,
                EMBEDDED_AT,
                KnowledgeEmbeddingClient.DIMENSIONS,
                1.0d);
    }

    private static SynTenPdfEmbeddingMetadata incomplete() {
        return new SynTenPdfEmbeddingMetadata(KnowledgeEmbeddingClient.MODEL_ID, null, null, null, null, null);
    }

    private static SynTenPdfEmbeddingMetadata conflicting() {
        return new SynTenPdfEmbeddingMetadata("amazon.titan-embed-text-v2:0", 1024, true, EMBEDDED_AT, 1024, 1.0d);
    }
}
