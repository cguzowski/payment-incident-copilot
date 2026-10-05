package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class SpringAiOllamaKnowledgeEmbeddingClient implements KnowledgeEmbeddingClient {

    private static final int MAXIMUM_INPUT_CHARACTERS = 50_000;
    private static final double NORMALIZATION_TOLERANCE = 0.01d;

    private final EmbeddingModel model;
    private final String keepAlive;
    private final String modelId;

    @Autowired
    SpringAiOllamaKnowledgeEmbeddingClient(
            Optional<EmbeddingModel> model,
            @Value("${spring.ai.ollama.embedding.keep-alive:5m}") String keepAlive,
            @Value("${spring.ai.ollama.embedding.model:nomic-embed-text}") String modelId) {
        this.model = model.orElse(null);
        this.keepAlive = keepAlive;
        this.modelId = modelId;
    }

    SpringAiOllamaKnowledgeEmbeddingClient(Optional<EmbeddingModel> model) {
        this(model, "5m", KnowledgeEmbeddingClient.MODEL_ID);
    }

    SpringAiOllamaKnowledgeEmbeddingClient(EmbeddingModel model) {
        this(Optional.ofNullable(model), "5m", KnowledgeEmbeddingClient.MODEL_ID);
    }

    @Override
    public KnowledgeEmbedding embed(String input) {
        if (input == null || input.isBlank() || input.length() > MAXIMUM_INPUT_CHARACTERS) {
            throw new KnowledgeEmbeddingMalformedException();
        }
        if (model == null) {
            throw new KnowledgeEmbeddingUnavailableException();
        }
        try {
            // Spring AI 2.0 drops provider-specific defaults when embed(String)
            // merges generic runtime options. An explicit request preserves residency.
            var response = model.call(new EmbeddingRequest(
                    List.of(input),
                    OllamaEmbeddingOptions.builder()
                            .model(modelId)
                            .keepAlive(keepAlive)
                            .build()));
            float[] vector = response.getResult().getOutput();
            validate(vector);
            return new KnowledgeEmbedding(
                    KnowledgeEmbeddingClient.MODEL_ID, KnowledgeEmbeddingClient.DIMENSIONS, true, vector);
        } catch (KnowledgeEmbeddingMalformedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (hasTimeoutCause(exception)) {
                throw new KnowledgeEmbeddingTimedOutException(exception);
            }
            throw new KnowledgeEmbeddingUnavailableException(exception);
        }
    }

    private static void validate(float[] vector) {
        if (vector == null || vector.length != KnowledgeEmbeddingClient.DIMENSIONS) {
            throw new KnowledgeEmbeddingMalformedException();
        }
        double squaredNorm = 0.0d;
        for (float value : vector) {
            if (!Float.isFinite(value)) {
                throw new KnowledgeEmbeddingMalformedException();
            }
            squaredNorm += (double) value * value;
        }
        if (Math.abs(squaredNorm - 1.0d) > NORMALIZATION_TOLERANCE) {
            throw new KnowledgeEmbeddingMalformedException();
        }
    }

    private static boolean hasTimeoutCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
