package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class SynTenPdfEmbeddingCallExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SynTenPdfEmbeddingCallExecutor.class);
    private static final double NORMALIZATION_TOLERANCE = 0.01d;

    private final long perCallTimeoutNanos;
    private final long operationTimeoutNanos;

    SynTenPdfEmbeddingCallExecutor(
            @Value("${app.knowledge.pdf-backfill.per-call-timeout:30s}") Duration perCallTimeout,
            @Value("${app.knowledge.pdf-backfill.operation-timeout:8h}") Duration operationTimeout) {
        perCallTimeoutNanos = positiveNanos(perCallTimeout, "per-call timeout");
        operationTimeoutNanos = positiveNanos(operationTimeout, "operation timeout");
    }

    List<PreparedSynTenPdfEmbedding> prepare(List<SynTenPdfEmbeddingTarget> targets, KnowledgeEmbeddingClient client) {
        long startedAt = System.nanoTime();
        List<PreparedSynTenPdfEmbedding> prepared = new ArrayList<>(targets.size());
        for (int index = 0; index < targets.size(); index++) {
            SynTenPdfEmbeddingTarget target = targets.get(index);
            long remainingNanos = operationTimeoutNanos - (System.nanoTime() - startedAt);
            if (remainingNanos <= 0) {
                throw new KnowledgeEmbeddingTimedOutException(
                        new TimeoutException("SynTen PDF embedding operation deadline elapsed."));
            }
            if (index == 0 || index + 1 == targets.size() || (index + 1) % 100 == 0) {
                LOGGER.info(
                        "Preparing SynTen PDF embedding: chunkId={}, documentVersionId={}, ordinal={}, completed={}, total={}",
                        target.chunkId(),
                        target.documentVersionId(),
                        target.chunkOrdinal(),
                        index,
                        targets.size());
            }
            KnowledgeEmbedding embedding =
                    invoke(client, target.embeddingInput(), Math.min(perCallTimeoutNanos, remainingNanos));
            validate(embedding);
            prepared.add(new PreparedSynTenPdfEmbedding(
                    target, embedding.modelId(), embedding.dimensions(), embedding.normalized(), embedding.vector()));
        }
        return List.copyOf(prepared);
    }

    private static KnowledgeEmbedding invoke(KnowledgeEmbeddingClient client, String input, long timeoutNanos) {
        FutureTask<KnowledgeEmbedding> invocation = new FutureTask<>(() -> client.embed(input));
        Thread.ofVirtual().name("synten-pdf-embedding-call-", 0).start(invocation);
        try {
            return invocation.get(timeoutNanos, TimeUnit.NANOSECONDS);
        } catch (TimeoutException exception) {
            invocation.cancel(true);
            throw new KnowledgeEmbeddingTimedOutException(exception);
        } catch (InterruptedException exception) {
            invocation.cancel(true);
            Thread.currentThread().interrupt();
            throw new KnowledgeEmbeddingUnavailableException(exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new KnowledgeEmbeddingUnavailableException(exception);
        }
    }

    private static void validate(KnowledgeEmbedding embedding) {
        if (embedding == null
                || !KnowledgeEmbeddingClient.MODEL_ID.equals(embedding.modelId())
                || embedding.dimensions() != KnowledgeEmbeddingClient.DIMENSIONS
                || !embedding.normalized()) {
            throw new KnowledgeEmbeddingMalformedException();
        }
        float[] vector = embedding.vector();
        if (vector.length != KnowledgeEmbeddingClient.DIMENSIONS) {
            throw new KnowledgeEmbeddingMalformedException();
        }
        double squaredNorm = 0.0d;
        for (float value : vector) {
            if (!Float.isFinite(value)) {
                throw new KnowledgeEmbeddingMalformedException();
            }
            squaredNorm += (double) value * value;
        }
        if (Math.abs(Math.sqrt(squaredNorm) - 1.0d) > NORMALIZATION_TOLERANCE) {
            throw new KnowledgeEmbeddingMalformedException();
        }
    }

    private static long positiveNanos(Duration duration, String name) {
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("The SynTen PDF embedding " + name + " must be positive.");
        }
        try {
            return duration.toNanos();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("The SynTen PDF embedding " + name + " is too large.", exception);
        }
    }
}
