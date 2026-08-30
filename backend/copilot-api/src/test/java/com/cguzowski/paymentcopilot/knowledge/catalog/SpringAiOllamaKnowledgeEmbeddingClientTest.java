package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

class SpringAiOllamaKnowledgeEmbeddingClientTest {

    @Test
    void requestsAndValidatesNormalizedNomicEmbedding() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        float[] vector = normalizedVector();
        when(model.embed("embedding input")).thenReturn(vector);
        SpringAiOllamaKnowledgeEmbeddingClient client = new SpringAiOllamaKnowledgeEmbeddingClient(model);

        KnowledgeEmbedding embedding = client.embed("embedding input");

        verify(model).embed("embedding input");
        assertThat(embedding.modelId()).isEqualTo("nomic-embed-text");
        assertThat(embedding.dimensions()).isEqualTo(768);
        assertThat(embedding.normalized()).isTrue();
        assertThat(embedding.vector()).containsExactly(vector);
    }

    @Test
    void rejectsWrongDimensionNonFiniteOrUnnormalizedOutput() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        SpringAiOllamaKnowledgeEmbeddingClient client = new SpringAiOllamaKnowledgeEmbeddingClient(model);

        when(model.embed("wrong-size")).thenReturn(new float[1024]);
        assertThatThrownBy(() -> client.embed("wrong-size")).isInstanceOf(KnowledgeEmbeddingMalformedException.class);

        float[] nonFinite = normalizedVector();
        nonFinite[20] = Float.NaN;
        when(model.embed("non-finite")).thenReturn(nonFinite);
        assertThatThrownBy(() -> client.embed("non-finite")).isInstanceOf(KnowledgeEmbeddingMalformedException.class);

        float[] notNormalized = new float[768];
        notNormalized[0] = 2.0f;
        when(model.embed("not-normalized")).thenReturn(notNormalized);
        assertThatThrownBy(() -> client.embed("not-normalized"))
                .isInstanceOf(KnowledgeEmbeddingMalformedException.class);
    }

    @Test
    void mapsMissingProviderAndTimeoutWithoutCallingARealModel() {
        SpringAiOllamaKnowledgeEmbeddingClient unavailable =
                new SpringAiOllamaKnowledgeEmbeddingClient(Optional.empty());
        assertThatThrownBy(() -> unavailable.embed("embedding input"))
                .isInstanceOf(KnowledgeEmbeddingUnavailableException.class);

        EmbeddingModel model = mock(EmbeddingModel.class);
        when(model.embed("embedding input"))
                .thenThrow(new IllegalStateException(new SocketTimeoutException("synthetic timeout")));
        SpringAiOllamaKnowledgeEmbeddingClient timedOut = new SpringAiOllamaKnowledgeEmbeddingClient(model);
        assertThatThrownBy(() -> timedOut.embed("embedding input"))
                .isInstanceOf(KnowledgeEmbeddingTimedOutException.class);
    }

    private static float[] normalizedVector() {
        float[] vector = new float[768];
        vector[0] = 1.0f;
        return vector;
    }
}
