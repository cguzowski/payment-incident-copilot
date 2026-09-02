package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SynTenPdfCatalogPlannerTest {

    private static final Path CORPUS_ROOT =
            Path.of("..", "..", "SynTen Inc", "corpus").toAbsolutePath().normalize();

    @Test
    void plansTheExactImmutableRealCorpusTwice() {
        SynTenCorpusSourceRepository sources = new SynTenCorpusSourceRepository(
                CORPUS_ROOT, JsonMapper.builder().build());
        SynTenPdfCatalogPlanner planner = new SynTenPdfCatalogPlanner(
                sources,
                new PdfBoxKnowledgeDocumentParser(),
                new PdfKnowledgeChunker(new ApproximateTokenEstimator(), 400, 600, 50, 80));

        SynTenPdfCatalogPlan first = planner.plan();
        SynTenPdfCatalogPlan second = planner.plan();

        assertThat(second.catalogFingerprint()).isEqualTo(first.catalogFingerprint());
        assertThat(second.documents())
                .extracting(PdfCatalogDocumentPlan::documentVersionId)
                .containsExactlyElementsOf(first.documents().stream()
                        .map(PdfCatalogDocumentPlan::documentVersionId)
                        .toList());
        assertThat(second.documents().stream().flatMap(plan -> plan.chunks().stream()))
                .containsExactlyElementsOf(first.documents().stream()
                        .flatMap(plan -> plan.chunks().stream())
                        .toList());
        assertThat(first.tenantId()).isEqualTo(SynTenCorpusSourceRepository.SYNTEN_TENANT_ID);
        assertThat(first.documents()).hasSize(30);
        assertThat(first.chunkCount()).isEqualTo(705);
        assertThat(first.catalogFingerprint())
                .isEqualTo("734461e767e08a59b83169fdf75d208d20c0366bebecd8825e2458c5f1b3d427");
        assertThat(first.documents())
                .extracting(plan -> plan.document().source().documentKey())
                .containsExactlyElementsOf(sources.findAll().stream()
                        .map(SynTenPdfSourceDocument::documentKey)
                        .toList());
        assertThat(first.documents()).allSatisfy(plan -> {
            SynTenPdfSourceDocument source = plan.document().source();
            assertThat(source.tenantId()).isEqualTo(first.tenantId());
            assertThat(plan.documentVersionId()).isNotNull();
            assertThat(plan.catalogContentHash()).matches("[0-9a-f]{64}");
            assertThat(source.sourceSha256()).matches("[0-9a-f]{64}");
            assertThat(source.pdfSha256()).matches("[0-9a-f]{64}");
            assertThat(plan.document().extractionStrategyVersion())
                    .isEqualTo(PdfBoxKnowledgeDocumentParser.EXTRACTION_STRATEGY_VERSION);
        });

        List<PdfKnowledgeChunkDraft> chunks = first.documents().stream()
                .flatMap(plan -> plan.chunks().stream())
                .toList();
        assertThat(chunks).extracting(PdfKnowledgeChunkDraft::chunkId).doesNotHaveDuplicates();
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.rawContentHash()).matches("[0-9a-f]{64}");
            assertThat(chunk.embeddingInputHash()).matches("[0-9a-f]{64}");
            assertThat(chunk.embeddingInputTemplateVersion()).isEqualTo("embedding-input/v1");
            assertThat(chunk.chunkingStrategyVersion()).isEqualTo(PdfKnowledgeChunker.CHUNKING_STRATEGY_VERSION);
            assertThat(chunk.pageNumber()).isBetween(1, 15);
            assertThat(chunk.startBlock()).isPositive();
            assertThat(chunk.endBlock()).isGreaterThanOrEqualTo(chunk.startBlock());
        });
        assertThat(first.documents().stream().map(PdfCatalogDocumentPlan::documentVersionId))
                .doesNotHaveDuplicates();
    }
}
