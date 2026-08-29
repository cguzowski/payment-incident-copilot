package com.cguzowski.paymentcopilot.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MarkdownKnowledgeChunkerTest {

    @Test
    void preservesRawContentAndBuildsVersionedEmbeddingInput() {
        ApprovedKnowledgeDocument document = document("""
                # Authorization Decline Runbook

                ## Gateway Failures

                ### Diagnosis

                Inspect timeout and connection-reset observations exactly as recorded.
                """);
        MarkdownKnowledgeChunker chunker = new MarkdownKnowledgeChunker(text -> text.split("\\s+").length, 40, 60, 5);

        List<KnowledgeChunkDraft> chunks = chunker.chunk(document);

        assertThat(chunks).hasSize(1);
        KnowledgeChunkDraft chunk = chunks.getFirst();
        assertThat(chunk.ordinal()).isZero();
        assertThat(chunk.sectionPath()).isEqualTo("Gateway Failures > Diagnosis");
        assertThat(chunk.rawContent())
                .isEqualTo("### Diagnosis\n\nInspect timeout and connection-reset observations exactly as recorded.");
        assertThat(chunk.embeddingInput())
                .isEqualTo("Document: Authorization Decline Runbook\n"
                        + "Section: Gateway Failures > Diagnosis\n"
                        + "Type: RUNBOOK\n"
                        + "Applies to: Card authorization\n\n"
                        + "### Diagnosis\n\n"
                        + "Inspect timeout and connection-reset observations exactly as recorded.");
        assertThat(chunk.embeddingInputTemplateVersion()).isEqualTo("embedding-input/v1");
        assertThat(chunk.sourceStartLine()).isEqualTo(18);
        assertThat(chunk.sourceEndLine()).isEqualTo(20);
        assertThat(chunk.rawContentHash()).hasSize(64);
        assertThat(chunk.embeddingInputHash()).hasSize(64).isNotEqualTo(chunk.rawContentHash());
    }

    @Test
    void splitsAtTargetWithOverlapOnlyInsideTheSameSection() {
        ApprovedKnowledgeDocument document = document("""
                # Authorization Decline Runbook

                ## Gateway Failures

                one two three four five

                six seven eight nine ten

                eleven twelve thirteen fourteen fifteen

                ## Account Status

                alpha beta gamma delta epsilon
                """);
        MarkdownKnowledgeChunker chunker = new MarkdownKnowledgeChunker(text -> text.split("\\s+").length, 10, 12, 2);

        List<KnowledgeChunkDraft> chunks = chunker.chunk(document);

        assertThat(chunks).extracting(KnowledgeChunkDraft::sectionPath)
                .containsExactly(
                        "Gateway Failures",
                        "Gateway Failures",
                        "Gateway Failures",
                        "Account Status");
        assertThat(chunks).extracting(KnowledgeChunkDraft::rawContent)
                .containsExactly(
                        "## Gateway Failures\n\none two three four five",
                        "four five\n\nsix seven eight nine ten",
                        "nine ten\n\neleven twelve thirteen fourteen fifteen",
                        "## Account Status\n\nalpha beta gamma delta epsilon");
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.estimatedTokens()).isLessThanOrEqualTo(12));
        assertThat(chunks.get(3).rawContent()).doesNotContain("four", "ten", "fifteen");
    }

    @Test
    void keepsAnAtomicMarkdownBlockIntactUntilTheHardMaximum() {
        ApprovedKnowledgeDocument document = document("one two three four five six");
        MarkdownKnowledgeChunker chunker = new MarkdownKnowledgeChunker(text -> text.split("\\s+").length, 4, 8, 2);

        List<KnowledgeChunkDraft> chunks = chunker.chunk(document);

        assertThat(chunks).singleElement().satisfies(chunk -> {
            assertThat(chunk.rawContent()).isEqualTo("one two three four five six");
            assertThat(chunk.estimatedTokens()).isEqualTo(6);
        });
    }

    private static ApprovedKnowledgeDocument document(String body) {
        return new ApprovedKnowledgeDocument(
                UUID.fromString("66a84fed-3d77-4e7e-9a1b-e25ff37e2280"),
                UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61"),
                KnowledgeDocumentType.RUNBOOK,
                "Authorization Decline Runbook",
                "1.0.0",
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Card authorization",
                KnowledgeApprovalStatus.APPROVED,
                UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"),
                Instant.parse("2026-08-20T10:00:00Z"),
                Instant.parse("2026-08-21T00:00:00Z"),
                "authorization-decline-runbook.md",
                14,
                body);
    }
}
