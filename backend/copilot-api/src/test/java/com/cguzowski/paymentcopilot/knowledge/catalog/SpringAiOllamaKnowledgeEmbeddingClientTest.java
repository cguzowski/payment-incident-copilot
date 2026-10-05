package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

class SpringAiOllamaKnowledgeEmbeddingClientTest {

    @Test
    void requestsAndValidatesNormalizedNomicEmbedding() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        float[] vector = normalizedVector();
        when(model.call(argThat(
                        request -> request != null && request.getInstructions().equals(List.of("embedding input")))))
                .thenReturn(response(vector));
        SpringAiOllamaKnowledgeEmbeddingClient client = new SpringAiOllamaKnowledgeEmbeddingClient(model);

        KnowledgeEmbedding embedding = client.embed("embedding input");

        verify(model)
                .call(argThat(request -> request.getInstructions().equals(List.of("embedding input"))
                        && ((org.springframework.ai.ollama.api.OllamaEmbeddingOptions) request.getOptions())
                                .getKeepAlive()
                                .equals("5m")));
        assertThat(embedding.modelId()).isEqualTo("nomic-embed-text");
        assertThat(embedding.dimensions()).isEqualTo(768);
        assertThat(embedding.normalized()).isTrue();
        assertThat(embedding.vector()).containsExactly(vector);
    }

    @Test
    void rejectsWrongDimensionNonFiniteOrUnnormalizedOutput() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        SpringAiOllamaKnowledgeEmbeddingClient client = new SpringAiOllamaKnowledgeEmbeddingClient(model);

        when(model.call(argThat(
                        request -> request != null && request.getInstructions().equals(List.of("wrong-size")))))
                .thenReturn(response(new float[1024]));
        assertThatThrownBy(() -> client.embed("wrong-size")).isInstanceOf(KnowledgeEmbeddingMalformedException.class);

        float[] nonFinite = normalizedVector();
        nonFinite[20] = Float.NaN;
        when(model.call(argThat(
                        request -> request != null && request.getInstructions().equals(List.of("non-finite")))))
                .thenReturn(response(nonFinite));
        assertThatThrownBy(() -> client.embed("non-finite")).isInstanceOf(KnowledgeEmbeddingMalformedException.class);

        float[] notNormalized = new float[768];
        notNormalized[0] = 2.0f;
        when(model.call(argThat(
                        request -> request != null && request.getInstructions().equals(List.of("not-normalized")))))
                .thenReturn(response(notNormalized));
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
        when(model.call(argThat(
                        request -> request != null && request.getInstructions().equals(List.of("embedding input")))))
                .thenThrow(new IllegalStateException(new SocketTimeoutException("synthetic timeout")));
        SpringAiOllamaKnowledgeEmbeddingClient timedOut = new SpringAiOllamaKnowledgeEmbeddingClient(model);
        assertThatThrownBy(() -> timedOut.embed("embedding input"))
                .isInstanceOf(KnowledgeEmbeddingTimedOutException.class);
    }

    private static EmbeddingResponse response(float[] vector) {
        return new EmbeddingResponse(List.of(new Embedding(vector, 0)));
    }

    private static float[] normalizedVector() {
        float[] vector = new float[768];
        vector[0] = 1.0f;
        return vector;
    }
}
