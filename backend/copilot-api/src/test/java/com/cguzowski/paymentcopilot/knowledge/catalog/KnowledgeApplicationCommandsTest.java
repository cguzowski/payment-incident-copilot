package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class KnowledgeApplicationCommandsTest {

    @Test
    void explicitIngestionCommandImportsApprovedSources() throws Exception {
        KnowledgeIngestionService ingestionService = mock(KnowledgeIngestionService.class);
        when(ingestionService.importApprovedSources()).thenReturn(new KnowledgeImportSummary(2, 0, 9));

        new KnowledgeIngestionCommand(ingestionService).run(null);

        verify(ingestionService).importApprovedSources();
    }

    @Test
    void explicitPdfCatalogCommandImportsTheConfiguredCorpus() throws Exception {
        SynTenPdfCatalogImportService importService = mock(SynTenPdfCatalogImportService.class);
        when(importService.importCorpus()).thenReturn(new PdfCatalogImportSummary(30, 0, 180));

        new SynTenPdfCatalogImportCommand(importService).run(null);

        verify(importService).importCorpus();
    }

    @Test
    void embeddingSmokeCommandValidatesTheConfiguredModelContract() throws Exception {
        KnowledgeEmbeddingClient client = mock(KnowledgeEmbeddingClient.class);
        when(client.embed(KnowledgeEmbeddingSmokeTestCommand.SMOKE_INPUT)).thenReturn(normalizedEmbedding());

        new KnowledgeEmbeddingSmokeTestCommand(client).run(null);

        verify(client).embed(KnowledgeEmbeddingSmokeTestCommand.SMOKE_INPUT);
    }

    @Test
    void embeddingSmokeCommandFailsSafelyForAContractMismatch() {
        KnowledgeEmbeddingClient client = mock(KnowledgeEmbeddingClient.class);
        when(client.embed(KnowledgeEmbeddingSmokeTestCommand.SMOKE_INPUT))
                .thenReturn(new KnowledgeEmbedding("unexpected-model", 1, false, new float[] {1.0f}));

        assertThatThrownBy(() -> new KnowledgeEmbeddingSmokeTestCommand(client).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Embedding smoke test failed its model contract.");
    }

    private static KnowledgeEmbedding normalizedEmbedding() {
        float[] vector = new float[KnowledgeEmbeddingClient.DIMENSIONS];
        vector[0] = 1.0f;
        return new KnowledgeEmbedding(
                KnowledgeEmbeddingClient.MODEL_ID, KnowledgeEmbeddingClient.DIMENSIONS, true, vector);
    }
}
