package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import tools.jackson.databind.json.JsonMapper;

@SuppressWarnings("unchecked")
class SynTenPdfCatalogImportServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-31T18:00:00Z");

    @Test
    void validatesTheWholeCorpusBeforeOneAtomicWrite() {
        SynTenCorpusSourceRepository sources = mock(SynTenCorpusSourceRepository.class);
        PdfBoxKnowledgeDocumentParser parser = mock(PdfBoxKnowledgeDocumentParser.class);
        PdfKnowledgeChunker chunker = mock(PdfKnowledgeChunker.class);
        SynTenPdfCatalogPersistenceService persistence = mock(SynTenPdfCatalogPersistenceService.class);
        SynTenPdfSourceDocument firstSource = source("RB-001", "66a84fed-3d77-4e7e-9a1b-e25ff37e2280");
        SynTenPdfSourceDocument secondSource = source("PL-001", "f3e31211-e2ad-4a8e-b504-671f9be1a160");
        PdfKnowledgeDocument firstDocument = document(firstSource);
        PdfKnowledgeDocument secondDocument = document(secondSource);
        PdfKnowledgeChunkDraft firstChunk = chunk(firstDocument, 0);
        PdfKnowledgeChunkDraft secondChunk = chunk(secondDocument, 0);
        when(sources.findAll()).thenReturn(List.of(firstSource, secondSource));
        when(parser.parse(firstSource)).thenReturn(firstDocument);
        when(parser.parse(secondSource)).thenReturn(secondDocument);
        when(chunker.chunk(firstDocument)).thenReturn(List.of(firstChunk));
        when(chunker.chunk(secondDocument)).thenReturn(List.of(secondChunk));
        when(persistence.importAll(any())).thenReturn(new PdfCatalogImportSummary(2, 0, 2));
        SynTenPdfCatalogImportService service = new SynTenPdfCatalogImportService(
                sources, parser, chunker, persistence, Clock.fixed(NOW, ZoneOffset.UTC));

        PdfCatalogImportSummary summary = service.importCorpus();

        assertThat(summary).isEqualTo(new PdfCatalogImportSummary(2, 0, 2));
        ArgumentCaptor<List<PdfCatalogDocumentPlan>> plans = ArgumentCaptor.forClass(List.class);
        verify(persistence).importAll(plans.capture());
        assertThat(plans.getValue()).hasSize(2).allSatisfy(plan -> {
            assertThat(plan.documentVersionId()).isNotNull();
            assertThat(plan.catalogContentHash()).matches("[0-9a-f]{64}");
            assertThat(plan.importedAt()).isEqualTo(NOW);
            assertThat(plan.chunks()).hasSize(1);
        });
        InOrder order = inOrder(parser, chunker, persistence);
        order.verify(parser).parse(firstSource);
        order.verify(chunker).chunk(firstDocument);
        order.verify(parser).parse(secondSource);
        order.verify(chunker).chunk(secondDocument);
        order.verify(persistence).importAll(any());
    }

    @Test
    void doesNotWriteWhenAnyDocumentFailsValidation() {
        SynTenCorpusSourceRepository sources = mock(SynTenCorpusSourceRepository.class);
        PdfBoxKnowledgeDocumentParser parser = mock(PdfBoxKnowledgeDocumentParser.class);
        PdfKnowledgeChunker chunker = mock(PdfKnowledgeChunker.class);
        SynTenPdfCatalogPersistenceService persistence = mock(SynTenPdfCatalogPersistenceService.class);
        SynTenPdfSourceDocument firstSource = source("RB-001", "66a84fed-3d77-4e7e-9a1b-e25ff37e2280");
        SynTenPdfSourceDocument secondSource = source("PL-001", "f3e31211-e2ad-4a8e-b504-671f9be1a160");
        PdfKnowledgeDocument firstDocument = document(firstSource);
        when(sources.findAll()).thenReturn(List.of(firstSource, secondSource));
        when(parser.parse(firstSource)).thenReturn(firstDocument);
        when(chunker.chunk(firstDocument)).thenReturn(List.of(chunk(firstDocument, 0)));
        when(parser.parse(secondSource)).thenThrow(new IllegalArgumentException("invalid second PDF"));
        SynTenPdfCatalogImportService service = new SynTenPdfCatalogImportService(
                sources, parser, chunker, persistence, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(service::importCorpus)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid second PDF");
        verify(persistence, never()).importAll(any());
    }

    @Test
    void producesTheSameDocumentPlanForTheSameInputs() {
        SynTenCorpusSourceRepository sources = mock(SynTenCorpusSourceRepository.class);
        PdfBoxKnowledgeDocumentParser parser = mock(PdfBoxKnowledgeDocumentParser.class);
        PdfKnowledgeChunker chunker = mock(PdfKnowledgeChunker.class);
        SynTenPdfCatalogPersistenceService persistence = mock(SynTenPdfCatalogPersistenceService.class);
        SynTenPdfSourceDocument source = source("RB-001", "66a84fed-3d77-4e7e-9a1b-e25ff37e2280");
        PdfKnowledgeDocument document = document(source);
        when(sources.findAll()).thenReturn(List.of(source));
        when(parser.parse(source)).thenReturn(document);
        when(chunker.chunk(document)).thenReturn(List.of(chunk(document, 0)));
        when(persistence.importAll(any())).thenReturn(new PdfCatalogImportSummary(1, 0, 1));
        SynTenPdfCatalogImportService service = new SynTenPdfCatalogImportService(
                sources, parser, chunker, persistence, Clock.fixed(NOW, ZoneOffset.UTC));

        service.importCorpus();
        service.importCorpus();

        ArgumentCaptor<List<PdfCatalogDocumentPlan>> plans = ArgumentCaptor.forClass(List.class);
        verify(persistence, org.mockito.Mockito.times(2)).importAll(plans.capture());
        assertThat(plans.getAllValues().get(0)).isEqualTo(plans.getAllValues().get(1));
    }

    @Test
    void plansEveryRealCorpusPdfWithPageBoundedChunksBeforePersistence() {
        SynTenCorpusSourceRepository sources = new SynTenCorpusSourceRepository(
                Path.of("..", "..", "SynTen Inc", "corpus").toAbsolutePath().normalize(),
                JsonMapper.builder().build());
        PdfBoxKnowledgeDocumentParser parser = new PdfBoxKnowledgeDocumentParser();
        PdfKnowledgeChunker chunker = new PdfKnowledgeChunker(new ApproximateTokenEstimator(), 400, 600, 50, 80);
        SynTenPdfCatalogPersistenceService persistence = mock(SynTenPdfCatalogPersistenceService.class);
        when(persistence.importAll(any())).thenAnswer(invocation -> {
            List<PdfCatalogDocumentPlan> plans = invocation.getArgument(0);
            int chunks = plans.stream().mapToInt(plan -> plan.chunks().size()).sum();
            return new PdfCatalogImportSummary(plans.size(), 0, chunks);
        });
        SynTenPdfCatalogImportService service = new SynTenPdfCatalogImportService(
                sources, parser, chunker, persistence, Clock.fixed(NOW, ZoneOffset.UTC));

        PdfCatalogImportSummary summary = service.importCorpus();

        ArgumentCaptor<List<PdfCatalogDocumentPlan>> plans = ArgumentCaptor.forClass(List.class);
        verify(persistence).importAll(plans.capture());
        assertThat(summary.importedDocuments()).isEqualTo(30);
        assertThat(summary.cataloguedChunks()).isGreaterThan(30);
        assertThat(plans.getValue()).hasSize(30);
        assertThat(plans.getValue())
                .filteredOn(plan -> plan.document().source().approvalStatus() == KnowledgeApprovalStatus.APPROVED)
                .hasSize(27);
        assertThat(plans.getValue())
                .filteredOn(plan -> plan.document().source().approvalStatus() == KnowledgeApprovalStatus.SUPERSEDED)
                .hasSize(3);
        assertThat(plans.getValue())
                .flatExtracting(PdfCatalogDocumentPlan::chunks)
                .allSatisfy(chunk -> {
                    assertThat(chunk.estimatedTokens()).isLessThanOrEqualTo(600);
                    assertThat(chunk.pageNumber()).isPositive();
                    assertThat(chunk.startBlock()).isPositive();
                    assertThat(chunk.endBlock()).isGreaterThanOrEqualTo(chunk.startBlock());
                });
        assertThat(plans.getValue().stream()
                        .flatMap(plan -> plan.chunks().stream())
                        .map(PdfKnowledgeChunkDraft::chunkId))
                .doesNotHaveDuplicates();
        assertThat(plans.getValue().stream()
                        .map(plan -> plan.document().source().pdfName())
                        .collect(java.util.stream.Collectors.toSet()))
                .hasSize(30)
                .allMatch(name -> name.endsWith(".pdf"));
    }

    private static SynTenPdfSourceDocument source(String key, String documentId) {
        KnowledgeDocumentType type =
                key.startsWith("RB") ? KnowledgeDocumentType.RUNBOOK : KnowledgeDocumentType.POLICY;
        return new SynTenPdfSourceDocument(
                key,
                UUID.fromString(documentId),
                SynTenCorpusSourceRepository.SYNTEN_TENANT_ID,
                type,
                key + " title",
                "1.0.0",
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Card authorization",
                KnowledgeApprovalStatus.APPROVED,
                UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"),
                Instant.parse("2026-08-30T08:00:00Z"),
                Instant.parse("2026-08-30T09:00:00Z"),
                "Internal - Synthetic Demo",
                null,
                key.toLowerCase() + ".md",
                key.toLowerCase() + ".pdf",
                "b".repeat(64),
                "a".repeat(64),
                1,
                new byte[] {1},
                new byte[] {2});
    }

    private static PdfKnowledgeDocument document(SynTenPdfSourceDocument source) {
        return new PdfKnowledgeDocument(
                source,
                source.pdfSha256(),
                PdfBoxKnowledgeDocumentParser.EXTRACTION_STRATEGY_VERSION,
                List.of(new PdfKnowledgePage(1, List.of(new PdfTextBlock(1, "Exact source content.")))));
    }

    private static PdfKnowledgeChunkDraft chunk(PdfKnowledgeDocument document, int ordinal) {
        return new PdfKnowledgeChunker(text -> text.split("\\s+").length, 20, 30, 3, 4)
                .chunk(document)
                .get(ordinal);
    }
}
