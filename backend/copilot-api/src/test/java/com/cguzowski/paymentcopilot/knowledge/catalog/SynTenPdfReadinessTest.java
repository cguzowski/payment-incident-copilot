package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class SynTenPdfReadinessTest {
    @Test
    void missingCatalogGivesActionablePreparationInstruction() {
        var plans = mock(SynTenPdfEmbeddingPlanService.class);
        when(plans.planBackfill()).thenThrow(new IllegalStateException("missing catalog"));
        assertThatThrownBy(() -> new SynTenPdfReadinessCommand(plans).run(null))
                .hasMessageContaining("PrepareKnowledge");
    }

    @Test
    void absentEmbeddingsCannotClaimReadiness() {
        var plans = mock(SynTenPdfEmbeddingPlanService.class);
        when(plans.planBackfill())
                .thenReturn(
                        new SynTenPdfEmbeddingCatalogSnapshot("hash", SynTenPdfEmbeddingState.ABSENT, List.of(), null));
        assertThatThrownBy(() -> new SynTenPdfReadinessCommand(plans).run(null))
                .hasMessageContaining("PrepareKnowledge");
    }

    @Test
    void completeEmbeddingsCanClaimReadinessWithoutProviderCalls() {
        var plans = mock(SynTenPdfEmbeddingPlanService.class);
        when(plans.planBackfill())
                .thenReturn(new SynTenPdfEmbeddingCatalogSnapshot(
                        "hash", SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL, List.of(), null));
        new SynTenPdfReadinessCommand(plans).run(null);
    }
}
