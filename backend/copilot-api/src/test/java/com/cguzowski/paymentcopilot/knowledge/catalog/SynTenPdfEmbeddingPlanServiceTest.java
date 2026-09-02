package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SynTenPdfEmbeddingPlanServiceTest {

    private static final UUID TENANT_ID = SynTenCorpusSourceRepository.SYNTEN_TENANT_ID;
    private static final String FINGERPRINT = "a".repeat(64);
    private static final Instant EMBEDDED_AT = Instant.parse("2026-08-31T20:00:00Z");

    @Test
    void allowsAnExactAbsentCatalogForLaterModelPreparation() {
        SynTenPdfCatalogPlanner planner = mock(SynTenPdfCatalogPlanner.class);
        SynTenPdfCatalogSnapshotRepository repository = mock(SynTenPdfCatalogSnapshotRepository.class);
        SynTenPdfCatalogPlan catalogPlan = catalogPlan();
        SynTenPdfEmbeddingCatalogSnapshot snapshot = snapshot(SynTenPdfEmbeddingState.ABSENT, null);
        when(planner.plan()).thenReturn(catalogPlan);
        when(repository.readEmbeddingSnapshot(catalogPlan)).thenReturn(snapshot);

        SynTenPdfEmbeddingCatalogSnapshot result =
                new SynTenPdfEmbeddingPlanService(planner, repository).planBackfill();

        assertThat(result).isSameAs(snapshot);
        assertThat(result.operationSummary())
                .isEqualTo(new SynTenPdfEmbeddingOperationSummary(
                        FINGERPRINT, SynTenPdfEmbeddingState.ABSENT, 1, 0, false));
        verify(repository).readEmbeddingSnapshot(catalogPlan);
    }

    @Test
    void allowsAnExactCompletedCatalogOnlyAsANoOp() {
        SynTenPdfCatalogPlanner planner = mock(SynTenPdfCatalogPlanner.class);
        SynTenPdfCatalogSnapshotRepository repository = mock(SynTenPdfCatalogSnapshotRepository.class);
        SynTenPdfCatalogPlan catalogPlan = catalogPlan();
        SynTenPdfEmbeddingCatalogSnapshot snapshot = snapshot(SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL, EMBEDDED_AT);
        when(planner.plan()).thenReturn(catalogPlan);
        when(repository.readEmbeddingSnapshot(catalogPlan)).thenReturn(snapshot);

        SynTenPdfEmbeddingCatalogSnapshot result =
                new SynTenPdfEmbeddingPlanService(planner, repository).planBackfill();

        assertThat(result.operationSummary())
                .isEqualTo(new SynTenPdfEmbeddingOperationSummary(
                        FINGERPRINT, SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL, 1, 1, true));
    }

    @Test
    void rejectsMixedIncompleteAndConflictingCoverageBeforeLaterWork() {
        for (SynTenPdfEmbeddingState state : List.of(
                SynTenPdfEmbeddingState.MIXED,
                SynTenPdfEmbeddingState.INCOMPLETE,
                SynTenPdfEmbeddingState.CONFLICTING)) {
            SynTenPdfCatalogPlanner planner = mock(SynTenPdfCatalogPlanner.class);
            SynTenPdfCatalogSnapshotRepository repository = mock(SynTenPdfCatalogSnapshotRepository.class);
            SynTenPdfCatalogPlan catalogPlan = catalogPlan();
            when(planner.plan()).thenReturn(catalogPlan);
            when(repository.readEmbeddingSnapshot(catalogPlan)).thenReturn(snapshot(state, null));

            assertThatThrownBy(() -> new SynTenPdfEmbeddingPlanService(planner, repository).planBackfill())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(state.name());
        }
    }

    @Test
    void immutableRecordsDefensivelyCopyCollectionsAndVectors() {
        SynTenPdfEmbeddingTarget target = target();
        List<SynTenPdfEmbeddingTarget> mutableTargets = new ArrayList<>(List.of(target));
        SynTenPdfEmbeddingCatalogSnapshot snapshot = new SynTenPdfEmbeddingCatalogSnapshot(
                FINGERPRINT, SynTenPdfEmbeddingState.ABSENT, mutableTargets, null);
        mutableTargets.clear();

        float[] mutableVector = new float[] {1.0f, 0.0f};
        PreparedSynTenPdfEmbedding prepared =
                new PreparedSynTenPdfEmbedding(target, KnowledgeEmbeddingClient.MODEL_ID, 2, true, mutableVector);
        mutableVector[0] = 0.0f;
        float[] returnedVector = prepared.vector();
        returnedVector[0] = 0.0f;

        assertThat(snapshot.targets()).containsExactly(target);
        assertThat(prepared.vector()).containsExactly(1.0f, 0.0f);
    }

    private static SynTenPdfCatalogPlan catalogPlan() {
        PdfCatalogDocumentPlan document = mock(PdfCatalogDocumentPlan.class);
        when(document.chunks()).thenReturn(List.of(mock(PdfKnowledgeChunkDraft.class)));
        return new SynTenPdfCatalogPlan(TENANT_ID, FINGERPRINT, List.of(document));
    }

    private static SynTenPdfEmbeddingCatalogSnapshot snapshot(SynTenPdfEmbeddingState state, Instant embeddedAt) {
        return new SynTenPdfEmbeddingCatalogSnapshot(FINGERPRINT, state, List.of(target()), embeddedAt);
    }

    private static SynTenPdfEmbeddingTarget target() {
        return new SynTenPdfEmbeddingTarget(
                TENANT_ID,
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                "RB-001",
                "1.0.0",
                UUID.fromString("22222222-2222-4222-8222-222222222222"),
                0,
                "embedding input",
                "b".repeat(64),
                FINGERPRINT);
    }
}
