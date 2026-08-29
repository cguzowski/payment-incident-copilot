package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class KnowledgeIngestionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-28T10:00:00Z");

    @Test
    void embedsDualFormChunksBeforePersistingOneVersionedDocument() {
        ApprovedKnowledgeSourceRepository sources = mock(ApprovedKnowledgeSourceRepository.class);
        MarkdownKnowledgeChunker chunker = mock(MarkdownKnowledgeChunker.class);
        KnowledgeEmbeddingClient embeddingClient = mock(KnowledgeEmbeddingClient.class);
        KnowledgeIndexPersistenceService persistence = mock(KnowledgeIndexPersistenceService.class);
        ApprovedKnowledgeDocument document = document();
        KnowledgeChunkDraft draft = draft();
        when(sources.findAll()).thenReturn(List.of(document));
        when(chunker.chunk(document)).thenReturn(List.of(draft));
        when(persistence.findSourceContentHash(document.tenantId(), document.documentId(), document.version()))
                .thenReturn(Optional.empty());
        when(embeddingClient.embed(draft.embeddingInput())).thenReturn(embedding());
        when(persistence.insert(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        KnowledgeIngestionService service = new KnowledgeIngestionService(
                sources, chunker, embeddingClient, persistence, Clock.fixed(NOW, ZoneOffset.UTC));

        KnowledgeImportSummary summary = service.importApprovedSources();

        assertThat(summary).isEqualTo(new KnowledgeImportSummary(1, 0, 1));
        InOrder order = inOrder(persistence, embeddingClient);
        order.verify(persistence).findSourceContentHash(document.tenantId(), document.documentId(), document.version());
        order.verify(embeddingClient).embed(draft.embeddingInput());
        order.verify(persistence)
                .insert(org.mockito.ArgumentMatchers.argThat(indexed -> indexed.document()
                                .equals(document)
                        && indexed.sourceContentHash().equals(sourceHash(document))
                        && indexed.importedAt().equals(NOW)
                        && indexed.chunks().size() == 1
                        && indexed.chunks().getFirst().draft().equals(draft)
                        && indexed.chunks().getFirst().embedding().modelId().equals("amazon.titan-embed-text-v2:0")));
    }

    @Test
    void skipsUnchangedDocumentWithoutEmbeddingAgain() {
        ApprovedKnowledgeSourceRepository sources = mock(ApprovedKnowledgeSourceRepository.class);
        MarkdownKnowledgeChunker chunker = mock(MarkdownKnowledgeChunker.class);
        KnowledgeEmbeddingClient embeddingClient = mock(KnowledgeEmbeddingClient.class);
        KnowledgeIndexPersistenceService persistence = mock(KnowledgeIndexPersistenceService.class);
        ApprovedKnowledgeDocument document = document();
        when(sources.findAll()).thenReturn(List.of(document));
        when(persistence.findSourceContentHash(document.tenantId(), document.documentId(), document.version()))
                .thenReturn(Optional.of(sourceHash(document)));
        KnowledgeIngestionService service = new KnowledgeIngestionService(
                sources, chunker, embeddingClient, persistence, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(service.importApprovedSources()).isEqualTo(new KnowledgeImportSummary(0, 1, 0));
        verify(chunker, never()).chunk(org.mockito.ArgumentMatchers.any());
        verify(embeddingClient, never()).embed(org.mockito.ArgumentMatchers.any());
        verify(persistence, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsChangedContentUnderAnExistingVersionBeforeEmbedding() {
        ApprovedKnowledgeSourceRepository sources = mock(ApprovedKnowledgeSourceRepository.class);
        MarkdownKnowledgeChunker chunker = mock(MarkdownKnowledgeChunker.class);
        KnowledgeEmbeddingClient embeddingClient = mock(KnowledgeEmbeddingClient.class);
        KnowledgeIndexPersistenceService persistence = mock(KnowledgeIndexPersistenceService.class);
        ApprovedKnowledgeDocument document = document();
        when(sources.findAll()).thenReturn(List.of(document));
        when(persistence.findSourceContentHash(document.tenantId(), document.documentId(), document.version()))
                .thenReturn(Optional.of("f".repeat(64)));
        KnowledgeIngestionService service = new KnowledgeIngestionService(
                sources, chunker, embeddingClient, persistence, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(service::importApprovedSources)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Changed knowledge content or metadata requires a new document version.");
        verify(chunker, never()).chunk(org.mockito.ArgumentMatchers.any());
        verify(embeddingClient, never()).embed(org.mockito.ArgumentMatchers.any());
        verify(persistence, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    private static ApprovedKnowledgeDocument document() {
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
                "# Authorization Decline Runbook\n\n## Diagnosis\n\nExact source paragraph.\n");
    }

    private static KnowledgeChunkDraft draft() {
        return new KnowledgeChunkDraft(
                0,
                "Diagnosis",
                "Exact source paragraph.",
                "Document: Authorization Decline Runbook\nSection: Diagnosis\nType: RUNBOOK\n"
                        + "Applies to: Card authorization\n\nExact source paragraph.",
                "b".repeat(64),
                "c".repeat(64),
                "embedding-input/v1",
                18,
                18,
                5);
    }

    private static KnowledgeEmbedding embedding() {
        float[] vector = new float[1024];
        vector[0] = 1.0f;
        return new KnowledgeEmbedding("amazon.titan-embed-text-v2:0", 1024, true, vector);
    }

    private static String sourceHash(ApprovedKnowledgeDocument document) {
        return KnowledgeSourceHasher.hash(document);
    }
}
