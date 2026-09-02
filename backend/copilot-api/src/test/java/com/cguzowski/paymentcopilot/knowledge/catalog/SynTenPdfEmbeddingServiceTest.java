package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class SynTenPdfEmbeddingServiceTest {

    private static final Instant EMBEDDED_AT = Instant.parse("2026-09-01T09:00:00Z");
    private static final String FINGERPRINT = SynTenPdfCatalogPlanner.ACCEPTED_CATALOG_FINGERPRINT;

    @Test
    @SuppressWarnings("unchecked")
    void preparesAll705TargetsInStableOrderBeforeOnePersistenceCall() {
        SynTenPdfEmbeddingPlanService planService = mock(SynTenPdfEmbeddingPlanService.class);
        SynTenPdfEmbeddingPersistence persistence = mock(SynTenPdfEmbeddingPersistence.class);
        KnowledgeEmbeddingClient client = mock(KnowledgeEmbeddingClient.class);
        Clock clock = mock(Clock.class);
        List<SynTenPdfEmbeddingTarget> targets = targets();
        when(planService.planBackfill()).thenReturn(snapshot(SynTenPdfEmbeddingState.ABSENT, targets));
        when(client.embed(any())).thenAnswer(invocation -> normalizedEmbedding());
        when(clock.instant()).thenReturn(EMBEDDED_AT);
        SynTenPdfEmbeddingOperationSummary expected =
                new SynTenPdfEmbeddingOperationSummary(FINGERPRINT, SynTenPdfEmbeddingState.ABSENT, 705, 0, false);
        when(persistence.persist(any(), any())).thenReturn(expected);
        SynTenPdfEmbeddingService service = new SynTenPdfEmbeddingService(
                planService,
                client,
                new SynTenPdfEmbeddingCallExecutor(Duration.ofSeconds(1), Duration.ofMinutes(5)),
                persistence,
                clock);

        SynTenPdfEmbeddingOperationSummary result = service.backfill();

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<List<PreparedSynTenPdfEmbedding>> prepared = ArgumentCaptor.forClass(List.class);
        InOrder order = inOrder(client, clock, persistence);
        for (SynTenPdfEmbeddingTarget target : targets) {
            order.verify(client).embed(target.embeddingInput());
        }
        order.verify(clock).instant();
        order.verify(persistence).persist(prepared.capture(), org.mockito.ArgumentMatchers.eq(EMBEDDED_AT));
        assertThat(prepared.getValue())
                .extracting(item -> item.target().chunkId())
                .containsExactlyElementsOf(
                        targets.stream().map(SynTenPdfEmbeddingTarget::chunkId).toList());
        assertThat(prepared.getValue()).allSatisfy(item -> {
            assertThat(item.modelId()).isEqualTo(KnowledgeEmbeddingClient.MODEL_ID);
            assertThat(item.dimensions()).isEqualTo(KnowledgeEmbeddingClient.DIMENSIONS);
            assertThat(item.normalized()).isTrue();
            assertThat(item.vector()).hasSize(KnowledgeEmbeddingClient.DIMENSIONS);
        });
    }

    @ParameterizedTest(name = "{0} failure at target {1}")
    @MethodSource("failureCases")
    void discardsTheWholePreparedSetWhenAnyRepresentativeResponseFails(
            String label, int failingIndex, Supplier<Object> failure) {
        SynTenPdfEmbeddingPlanService planService = mock(SynTenPdfEmbeddingPlanService.class);
        SynTenPdfEmbeddingPersistence persistence = mock(SynTenPdfEmbeddingPersistence.class);
        KnowledgeEmbeddingClient client = mock(KnowledgeEmbeddingClient.class);
        Clock clock = mock(Clock.class);
        List<SynTenPdfEmbeddingTarget> targets = targets();
        when(planService.planBackfill()).thenReturn(snapshot(SynTenPdfEmbeddingState.ABSENT, targets));
        AtomicInteger calls = new AtomicInteger();
        when(client.embed(any())).thenAnswer(invocation -> {
            if (calls.getAndIncrement() == failingIndex) {
                Object outcome = failure.get();
                if (outcome instanceof RuntimeException exception) {
                    throw exception;
                }
                return outcome;
            }
            return normalizedEmbedding();
        });
        SynTenPdfEmbeddingService service = new SynTenPdfEmbeddingService(
                planService,
                client,
                new SynTenPdfEmbeddingCallExecutor(Duration.ofSeconds(1), Duration.ofMinutes(5)),
                persistence,
                clock);

        assertThatThrownBy(service::backfill).isInstanceOf(RuntimeException.class);

        assertThat(calls).hasValue(failingIndex + 1);
        verify(persistence, never()).persist(any(), any());
        verify(clock, never()).instant();
    }

    @Test
    void enforcesPerCallAndWholeOperationDeadlinesWithoutRetry() {
        SynTenPdfEmbeddingTarget target = targets().getFirst();
        KnowledgeEmbeddingClient blockingClient = input -> {
            try {
                Thread.sleep(Duration.ofSeconds(5));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return normalizedEmbedding();
        };
        SynTenPdfEmbeddingCallExecutor perCall =
                new SynTenPdfEmbeddingCallExecutor(Duration.ofMillis(20), Duration.ofSeconds(1));

        assertThatThrownBy(() -> perCall.prepare(List.of(target), blockingClient))
                .isInstanceOf(KnowledgeEmbeddingTimedOutException.class);

        AtomicInteger calls = new AtomicInteger();
        KnowledgeEmbeddingClient slowClient = input -> {
            calls.incrementAndGet();
            try {
                Thread.sleep(Duration.ofMillis(20));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return normalizedEmbedding();
        };
        SynTenPdfEmbeddingCallExecutor wholeOperation =
                new SynTenPdfEmbeddingCallExecutor(Duration.ofSeconds(1), Duration.ofMillis(35));

        assertThatThrownBy(() -> wholeOperation.prepare(targets().subList(0, 3), slowClient))
                .isInstanceOf(KnowledgeEmbeddingTimedOutException.class);
        assertThat(calls.get()).isBetween(1, 2);
    }

    @Test
    void returnsAnExactCompletedSnapshotWithoutModelClockOrPersistenceCalls() {
        SynTenPdfEmbeddingPlanService planService = mock(SynTenPdfEmbeddingPlanService.class);
        SynTenPdfEmbeddingPersistence persistence = mock(SynTenPdfEmbeddingPersistence.class);
        KnowledgeEmbeddingClient client = mock(KnowledgeEmbeddingClient.class);
        Clock clock = mock(Clock.class);
        SynTenPdfEmbeddingCatalogSnapshot complete = snapshot(SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL, targets());
        when(planService.planBackfill()).thenReturn(complete);
        SynTenPdfEmbeddingService service = new SynTenPdfEmbeddingService(
                planService,
                client,
                new SynTenPdfEmbeddingCallExecutor(Duration.ofSeconds(1), Duration.ofMinutes(5)),
                persistence,
                clock);

        assertThat(service.backfill()).isEqualTo(complete.operationSummary());

        verify(client, never()).embed(any());
        verify(persistence, never()).persist(any(), any());
        verify(clock, never()).instant();
    }

    private static java.util.stream.Stream<Arguments> failureCases() {
        List<Arguments> cases = new ArrayList<>();
        for (int index : List.of(0, 352, 704)) {
            cases.add(
                    Arguments.of("unavailable", index, (Supplier<Object>) KnowledgeEmbeddingUnavailableException::new));
            cases.add(Arguments.of("timed out", index, (Supplier<Object>)
                    () -> new KnowledgeEmbeddingTimedOutException(new java.util.concurrent.TimeoutException())));
            cases.add(Arguments.of("wrong model", index, (Supplier<Object>)
                    () -> new KnowledgeEmbedding("wrong-model", 768, true, normalizedVector(768))));
            cases.add(Arguments.of("wrong declared dimensions", index, (Supplier<Object>) () ->
                    new KnowledgeEmbedding(KnowledgeEmbeddingClient.MODEL_ID, 1024, true, normalizedVector(768))));
            cases.add(Arguments.of("not normalized", index, (Supplier<Object>) () ->
                    new KnowledgeEmbedding(KnowledgeEmbeddingClient.MODEL_ID, 768, false, normalizedVector(768))));
            cases.add(Arguments.of("null response", index, (Supplier<Object>) () -> null));
            cases.add(Arguments.of("wrong vector size", index, (Supplier<Object>)
                    () -> new KnowledgeEmbedding(KnowledgeEmbeddingClient.MODEL_ID, 768, true, normalizedVector(767))));
            cases.add(Arguments.of("non-finite", index, (Supplier<Object>)
                    () -> new KnowledgeEmbedding(KnowledgeEmbeddingClient.MODEL_ID, 768, true, nonFiniteVector())));
        }
        return cases.stream();
    }

    private static SynTenPdfEmbeddingCatalogSnapshot snapshot(
            SynTenPdfEmbeddingState state, List<SynTenPdfEmbeddingTarget> targets) {
        return new SynTenPdfEmbeddingCatalogSnapshot(
                FINGERPRINT, state, targets, state == SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL ? EMBEDDED_AT : null);
    }

    private static List<SynTenPdfEmbeddingTarget> targets() {
        return IntStream.range(0, 705)
                .mapToObj(index -> new SynTenPdfEmbeddingTarget(
                        SynTenCorpusSourceRepository.SYNTEN_TENANT_ID,
                        UUID.nameUUIDFromBytes(("version-" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        "RB-%03d".formatted(index / 32 + 1),
                        "1.0.0",
                        UUID.nameUUIDFromBytes(("chunk-" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        index,
                        "embedding input " + index,
                        "%064x".formatted(index + 1),
                        FINGERPRINT))
                .toList();
    }

    private static KnowledgeEmbedding normalizedEmbedding() {
        return new KnowledgeEmbedding(
                KnowledgeEmbeddingClient.MODEL_ID,
                KnowledgeEmbeddingClient.DIMENSIONS,
                true,
                normalizedVector(KnowledgeEmbeddingClient.DIMENSIONS));
    }

    private static float[] normalizedVector(int dimensions) {
        float[] vector = new float[dimensions];
        vector[0] = 1.0f;
        return vector;
    }

    private static float[] nonFiniteVector() {
        float[] vector = normalizedVector(KnowledgeEmbeddingClient.DIMENSIONS);
        vector[1] = Float.NaN;
        return vector;
    }
}
