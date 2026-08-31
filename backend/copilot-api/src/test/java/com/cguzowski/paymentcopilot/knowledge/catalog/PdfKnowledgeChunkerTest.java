package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PdfKnowledgeChunkerTest {

    @Test
    void neverCrossesPagesAndCarriesSectionContext() {
        PdfKnowledgeDocument document = document(
                page(1, "1. Purpose and scope", "intro one two", "2. Diagnostic procedure", "alpha beta gamma delta"),
                page(2, "epsilon zeta eta", "theta iota kappa", "3. Escalation package", "owner evidence"));
        PdfKnowledgeChunker chunker = new PdfKnowledgeChunker(words(), 8, 10, 2, 3);

        List<PdfKnowledgeChunkDraft> chunks = chunker.chunk(document);

        assertThat(chunks).extracting(PdfKnowledgeChunkDraft::ordinal).containsExactly(0, 1, 2, 3);
        assertThat(chunks).extracting(PdfKnowledgeChunkDraft::pageNumber).containsExactly(1, 1, 2, 2);
        assertThat(chunks)
                .extracting(PdfKnowledgeChunkDraft::sectionPath)
                .containsExactly(
                        "Purpose and scope", "Diagnostic procedure", "Diagnostic procedure", "Escalation package");
        assertThat(chunks.get(2).rawContent()).startsWith("epsilon zeta eta").doesNotContain("alpha");
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.startBlock()).isPositive();
            assertThat(chunk.endBlock()).isGreaterThanOrEqualTo(chunk.startBlock());
            assertThat(chunk.estimatedTokens()).isLessThanOrEqualTo(10);
            assertThat(chunk.embeddingInput()).endsWith(chunk.rawContent());
            assertThat(chunk.rawContentHash()).matches("[0-9a-f]{64}");
            assertThat(chunk.embeddingInputHash()).matches("[0-9a-f]{64}");
            assertThat(chunk.chunkingStrategyVersion()).isEqualTo("pdf-page-sections/v1");
        });
    }

    @Test
    void preservesTableLineOrderUsesSamePageOverlapAndMergesShortTail() {
        PdfKnowledgeDocument document = document(page(
                1,
                "4. Signal interpretation",
                "Signal Meaning Caution",
                "CODE_A first meaning review",
                "CODE_B second meaning review",
                "tail words"));
        PdfKnowledgeChunker chunker = new PdfKnowledgeChunker(words(), 9, 12, 3, 4);

        List<PdfKnowledgeChunkDraft> chunks = chunker.chunk(document);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.getFirst().rawContent())
                .containsSubsequence("Signal Meaning Caution", "CODE_A first meaning review");
        assertThat(chunks.get(1).rawContent())
                .containsSubsequence("CODE_A first meaning review", "CODE_B second meaning review", "tail words");
        assertThat(chunks.get(1).estimatedTokens()).isBetween(4, 12);
        assertThat(chunks.getFirst().pageNumber()).isEqualTo(chunks.get(1).pageNumber());
    }

    @Test
    void splitsAnOversizedBlockOnWordsAndKeepsItsBlockLocator() {
        PdfKnowledgeDocument document = document(page(1, "1. Diagnosis", "one two three four five six seven eight"));
        PdfKnowledgeChunker chunker = new PdfKnowledgeChunker(words(), 4, 5, 1, 2);

        List<PdfKnowledgeChunkDraft> chunks = chunker.chunk(document);

        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2).allSatisfy(chunk -> {
            assertThat(chunk.estimatedTokens()).isLessThanOrEqualTo(5);
            assertThat(chunk.startBlock()).isBetween(1, 2);
            assertThat(chunk.endBlock()).isBetween(chunk.startBlock(), 2);
        });
    }

    @Test
    void producesStableIdsAndChangesThemWhenThePdfHashChanges() {
        PdfKnowledgeDocument document = document(page(1, "1. Diagnosis", "exact stable content"));
        PdfKnowledgeChunker chunker = new PdfKnowledgeChunker(words(), 20, 30, 3, 4);

        List<PdfKnowledgeChunkDraft> first = chunker.chunk(document);
        List<PdfKnowledgeChunkDraft> second = chunker.chunk(document);
        PdfKnowledgeDocument changedHash = new PdfKnowledgeDocument(
                document.source(), "f".repeat(64), document.extractionStrategyVersion(), document.pages());

        assertThat(first).isEqualTo(second);
        assertThat(chunker.chunk(changedHash).getFirst().chunkId())
                .isNotEqualTo(first.getFirst().chunkId());
    }

    private static TokenEstimator words() {
        return text -> text.isBlank() ? 0 : text.split("\\s+").length;
    }

    private static PdfKnowledgeDocument document(PdfKnowledgePage... pages) {
        return new PdfKnowledgeDocument(source(), "a".repeat(64), "pdfbox-text-pages/v1", List.of(pages));
    }

    private static PdfKnowledgePage page(int number, String... blocks) {
        return new PdfKnowledgePage(
                number,
                java.util.stream.IntStream.range(0, blocks.length)
                        .mapToObj(index -> new PdfTextBlock(index + 1, blocks[index]))
                        .toList());
    }

    private static SynTenPdfSourceDocument source() {
        return new SynTenPdfSourceDocument(
                "RB-001",
                UUID.fromString("66a84fed-3d77-4e7e-9a1b-e25ff37e2280"),
                SynTenCorpusSourceRepository.SYNTEN_TENANT_ID,
                KnowledgeDocumentType.RUNBOOK,
                "Authorization Decline Incident Triage Runbook",
                "2.0.0",
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Card authorization incident triage",
                KnowledgeApprovalStatus.APPROVED,
                UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"),
                Instant.parse("2026-08-30T08:00:00Z"),
                Instant.parse("2026-08-30T09:00:00Z"),
                "Internal - Synthetic Demo",
                null,
                "rb-001.md",
                "rb-001.pdf",
                "b".repeat(64),
                "a".repeat(64),
                2,
                new byte[] {1},
                new byte[] {2});
    }
}
