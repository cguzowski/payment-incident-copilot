package com.cguzowski.paymentcopilot.knowledge;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
final class SpringAiTitanKnowledgeEmbeddingClient implements KnowledgeEmbeddingClient {

    static final String MODEL_ID = "amazon.titan-embed-text-v2:0";
    static final int DIMENSIONS = 1024;
    private static final int MAXIMUM_INPUT_CHARACTERS = 50_000;
    private static final double NORMALIZATION_TOLERANCE = 0.01d;

    private final EmbeddingModel model;

    @Autowired
    SpringAiTitanKnowledgeEmbeddingClient(Optional<EmbeddingModel> model) {
        this.model = model.orElse(null);
    }

    SpringAiTitanKnowledgeEmbeddingClient(EmbeddingModel model) {
        this.model = model;
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
            float[] vector = model.embed(input);
            validate(vector);
            return new KnowledgeEmbedding(MODEL_ID, DIMENSIONS, true, vector);
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
        if (vector == null || vector.length != DIMENSIONS) {
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
