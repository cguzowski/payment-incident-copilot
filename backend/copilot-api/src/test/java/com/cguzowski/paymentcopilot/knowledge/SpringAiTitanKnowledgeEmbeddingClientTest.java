package com.cguzowski.paymentcopilot.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

class SpringAiTitanKnowledgeEmbeddingClientTest {

    @Test
    void requestsAndValidatesNormalizedTitanV2Embedding() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        float[] vector = normalizedVector();
        when(model.embed("embedding input")).thenReturn(vector);
        SpringAiTitanKnowledgeEmbeddingClient client = new SpringAiTitanKnowledgeEmbeddingClient(model);

        KnowledgeEmbedding embedding = client.embed("embedding input");

        verify(model).embed("embedding input");
        assertThat(embedding.modelId()).isEqualTo("amazon.titan-embed-text-v2:0");
        assertThat(embedding.dimensions()).isEqualTo(1024);
        assertThat(embedding.normalized()).isTrue();
        assertThat(embedding.vector()).containsExactly(vector);
    }

    @Test
    void rejectsWrongDimensionNonFiniteOrUnnormalizedOutput() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        SpringAiTitanKnowledgeEmbeddingClient client = new SpringAiTitanKnowledgeEmbeddingClient(model);

        when(model.embed("wrong-size")).thenReturn(new float[512]);
        assertThatThrownBy(() -> client.embed("wrong-size"))
                .isInstanceOf(KnowledgeEmbeddingMalformedException.class);

        float[] nonFinite = normalizedVector();
        nonFinite[20] = Float.NaN;
        when(model.embed("non-finite")).thenReturn(nonFinite);
        assertThatThrownBy(() -> client.embed("non-finite"))
                .isInstanceOf(KnowledgeEmbeddingMalformedException.class);

        float[] notNormalized = new float[1024];
        notNormalized[0] = 2.0f;
        when(model.embed("not-normalized")).thenReturn(notNormalized);
        assertThatThrownBy(() -> client.embed("not-normalized"))
                .isInstanceOf(KnowledgeEmbeddingMalformedException.class);
    }

    private static float[] normalizedVector() {
        float[] vector = new float[1024];
        vector[0] = 1.0f;
        return vector;
    }
}
