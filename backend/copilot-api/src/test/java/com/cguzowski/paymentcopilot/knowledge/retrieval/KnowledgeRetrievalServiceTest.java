package com.cguzowski.paymentcopilot.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbedding;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingClient;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingMalformedException;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingTimedOutException;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingUnavailableException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class KnowledgeRetrievalServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("076a18a3-d54f-486a-b3ec-189e1048fd28");
    private static final UUID OPERATOR_ID = UUID.fromString("6904706f-d9e6-4543-a1bf-fc4b729e4c05");
    private static final UUID INVESTIGATION_ID = UUID.fromString("7f50162a-8dc5-45b0-9c88-dc2f77135e0f");
    private static final UUID CORRELATION_ID = UUID.fromString("133767cf-8ec8-487d-a09d-19d5efcece07");
    private static final UUID RETRIEVAL_ID = UUID.fromString("a74f88ed-e295-4caf-9404-a22f733d86ec");
    private static final Instant NOW = Instant.parse("2026-08-28T10:00:00Z");
    private static final KnowledgeRetrievalContext CONTEXT = new KnowledgeRetrievalContext(
            TENANT_ID,
            INVESTIGATION_ID,
            CORRELATION_ID,
            "AUTHORIZATION_DECLINE_RATE_SPIKE",
            "Authorization declines elevated",
            "Synthetic authorization declines exceeded the observation threshold.",
            null);

    @Test
    void persistsStartedBeforeEmbeddingAndCompletesAvailableSnapshot() {
        Fixture fixture = fixture();
        float[] vector = unitVector();
        when(fixture.embeddingClient.embed(any()))
                .thenReturn(new KnowledgeEmbedding(
                        KnowledgeEmbeddingClient.MODEL_ID, KnowledgeEmbeddingClient.DIMENSIONS, true, vector));
        when(fixture.searchRepository.search(any())).thenReturn(List.of(candidate()));
        when(fixture.persistence.complete(any())).thenReturn(true);

        KnowledgeRetrievalResponse response = fixture.service.retrieve(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID);

        InOrder order = inOrder(
                fixture.contextAssembler, fixture.persistence, fixture.embeddingClient, fixture.searchRepository);
        order.verify(fixture.contextAssembler).find(TENANT_ID, INVESTIGATION_ID);
        order.verify(fixture.persistence).insertStarted(any());
        order.verify(fixture.embeddingClient).embed(any());
        order.verify(fixture.searchRepository).search(any());
        order.verify(fixture.persistence).complete(any());

        ArgumentCaptor<KnowledgeRetrievalAttempt> started = ArgumentCaptor.forClass(KnowledgeRetrievalAttempt.class);
        verify(fixture.persistence).insertStarted(started.capture());
        assertThat(started.getValue().status()).isEqualTo(KnowledgeRetrievalStatus.STARTED);
        assertThat(started.getValue().requestedBy()).isEqualTo(OPERATOR_ID);
        assertThat(started.getValue().completedAt()).isNull();
        assertThat(started.getValue().results()).isEmpty();

        ArgumentCaptor<KnowledgeSearchRequest> search = ArgumentCaptor.forClass(KnowledgeSearchRequest.class);
        verify(fixture.searchRepository).search(search.capture());
        assertThat(search.getValue().queryEmbedding()).containsExactly(vector);
        assertThat(search.getValue().candidateDepth()).isEqualTo(20);
        assertThat(search.getValue().rrfK()).isEqualTo(60);
        assertThat(search.getValue().minimumVectorSimilarity()).isEqualTo(0.55f);

        ArgumentCaptor<KnowledgeRetrievalAttempt> completed = ArgumentCaptor.forClass(KnowledgeRetrievalAttempt.class);
        verify(fixture.persistence).complete(completed.capture());
        assertThat(completed.getValue().status()).isEqualTo(KnowledgeRetrievalStatus.AVAILABLE);
        assertThat(completed.getValue().results()).hasSize(1);
        assertThat(completed.getValue().results().getFirst().rawContent())
                .isEqualTo(candidate().rawContent());
        assertThat(response.status()).isEqualTo(KnowledgeRetrievalStatus.AVAILABLE);
        assertThat(response.results()).hasSize(1);
    }

    @Test
    void recordsPartialWhenEmbeddingIsUnavailableButLexicalCandidatesMatch() {
        Fixture fixture = fixture();
        when(fixture.embeddingClient.embed(any())).thenThrow(new KnowledgeEmbeddingUnavailableException());
        when(fixture.searchRepository.search(any())).thenReturn(List.of(candidate()));
        when(fixture.persistence.complete(any())).thenReturn(true);

        KnowledgeRetrievalResponse response = fixture.service.retrieve(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID);

        ArgumentCaptor<KnowledgeSearchRequest> search = ArgumentCaptor.forClass(KnowledgeSearchRequest.class);
        verify(fixture.searchRepository).search(search.capture());
        assertThat(search.getValue().queryEmbedding()).isNull();
        assertThat(response.status()).isEqualTo(KnowledgeRetrievalStatus.PARTIAL);
        assertThat(response.statusDetail()).isEqualTo("Query embedding was unavailable; lexical retrieval was used.");
        assertThat(response.results()).hasSize(1);
    }

    @Test
    void preservesEmbeddingFailureStatusWhenLexicalFallbackHasNoMatch() {
        for (EmbeddingFailure failure : List.of(
                new EmbeddingFailure(
                        new KnowledgeEmbeddingUnavailableException(), KnowledgeRetrievalStatus.UNAVAILABLE),
                new EmbeddingFailure(
                        new KnowledgeEmbeddingTimedOutException(new java.util.concurrent.TimeoutException()),
                        KnowledgeRetrievalStatus.TIMED_OUT),
                new EmbeddingFailure(new KnowledgeEmbeddingMalformedException(), KnowledgeRetrievalStatus.MALFORMED))) {
            Fixture fixture = fixture();
            when(fixture.embeddingClient.embed(any())).thenThrow(failure.exception());
            when(fixture.searchRepository.search(any())).thenReturn(List.of());
            when(fixture.persistence.complete(any())).thenReturn(true);

            KnowledgeRetrievalResponse response = fixture.service.retrieve(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID);

            assertThat(response.status()).isEqualTo(failure.expectedStatus());
            assertThat(response.results()).isEmpty();
        }
    }

    @Test
    void recordsNoMatchWhenBothModalitiesReturnNoEligibleCandidate() {
        Fixture fixture = fixture();
        when(fixture.embeddingClient.embed(any()))
                .thenReturn(new KnowledgeEmbedding(
                        KnowledgeEmbeddingClient.MODEL_ID, KnowledgeEmbeddingClient.DIMENSIONS, true, unitVector()));
        when(fixture.searchRepository.search(any())).thenReturn(List.of());
        when(fixture.persistence.complete(any())).thenReturn(true);

        KnowledgeRetrievalResponse response = fixture.service.retrieve(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID);

        assertThat(response.status()).isEqualTo(KnowledgeRetrievalStatus.NO_MATCH);
        assertThat(response.results()).isEmpty();
    }

    @Test
    void doesNotEmbedOrPersistForCrossTenantInvestigation() {
        Fixture fixture = fixture(OTHER_TENANT_ID, Optional.empty());

        assertThatThrownBy(() -> fixture.service.retrieve(OTHER_TENANT_ID, INVESTIGATION_ID, OPERATOR_ID))
                .isInstanceOf(KnowledgeInvestigationNotFoundException.class);
        verify(fixture.embeddingClient, never()).embed(any());
        verify(fixture.persistence, never()).insertStarted(any());
    }

    @Test
    void leavesStartedAttemptVisibleWhenUnexpectedSearchFailureInterruptsRetrieval() {
        Fixture fixture = fixture();
        when(fixture.embeddingClient.embed(any()))
                .thenReturn(new KnowledgeEmbedding(
                        KnowledgeEmbeddingClient.MODEL_ID, KnowledgeEmbeddingClient.DIMENSIONS, true, unitVector()));
        when(fixture.searchRepository.search(any())).thenThrow(new IllegalStateException("database interrupted"));

        assertThatThrownBy(() -> fixture.service.retrieve(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID))
                .isInstanceOf(IllegalStateException.class);
        verify(fixture.persistence).insertStarted(any());
        verify(fixture.persistence, never()).complete(any());
    }

    private static Fixture fixture() {
        return fixture(TENANT_ID, Optional.of(CONTEXT));
    }

    private static Fixture fixture(UUID lookupTenantId, Optional<KnowledgeRetrievalContext> context) {
        KnowledgeRetrievalContextAssembler contextAssembler = mock(KnowledgeRetrievalContextAssembler.class);
        KnowledgeRetrievalPersistenceService persistence = mock(KnowledgeRetrievalPersistenceService.class);
        KnowledgeEmbeddingClient embeddingClient = mock(KnowledgeEmbeddingClient.class);
        KnowledgeSearchRepository searchRepository = mock(KnowledgeSearchRepository.class);
        KnowledgeRetrievalIdentifierGenerator identifiers = mock(KnowledgeRetrievalIdentifierGenerator.class);
        when(contextAssembler.find(lookupTenantId, INVESTIGATION_ID)).thenReturn(context);
        when(identifiers.next()).thenReturn(RETRIEVAL_ID);
        KnowledgeRetrievalExecutor executor = new KnowledgeRetrievalExecutor(
                embeddingClient,
                searchRepository,
                new KnowledgeContextSelector(4, 3),
                new KnowledgeRetrievalQueryBuilder());
        KnowledgeRetrievalService service = new KnowledgeRetrievalService(
                contextAssembler, persistence, executor, identifiers, Clock.fixed(NOW, ZoneOffset.UTC));
        return new Fixture(service, contextAssembler, persistence, embeddingClient, searchRepository);
    }

    private static KnowledgeSearchCandidate candidate() {
        return new KnowledgeSearchCandidate(
                TENANT_ID,
                UUID.fromString("21111111-1111-4111-8111-111111111111"),
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                UUID.fromString("31111111-1111-4111-8111-111111111111"),
                KnowledgeDocumentType.RUNBOOK,
                "Authorization Decline Runbook",
                "1.0.0",
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Card authorization",
                "Gateway Failures > Diagnosis",
                "Inspect GATEWAY_TIMEOUT observations.",
                20,
                22,
                KnowledgeApprovalStatus.APPROVED,
                UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"),
                Instant.parse("2026-08-20T10:00:00Z"),
                Instant.parse("2026-08-21T00:00:00Z"),
                0.5f,
                1,
                1.0f,
                1,
                2.0 / 61.0);
    }

    private static float[] unitVector() {
        float[] vector = new float[KnowledgeEmbeddingClient.DIMENSIONS];
        vector[0] = 1.0f;
        return vector;
    }

    private record Fixture(
            KnowledgeRetrievalService service,
            KnowledgeRetrievalContextAssembler contextAssembler,
            KnowledgeRetrievalPersistenceService persistence,
            KnowledgeEmbeddingClient embeddingClient,
            KnowledgeSearchRepository searchRepository) {}

    private record EmbeddingFailure(RuntimeException exception, KnowledgeRetrievalStatus expectedStatus) {}
}
