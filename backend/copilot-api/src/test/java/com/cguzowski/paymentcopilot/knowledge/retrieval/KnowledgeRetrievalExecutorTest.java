package com.cguzowski.paymentcopilot.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbedding;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingClient;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingMalformedException;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingTimedOutException;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingUnavailableException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KnowledgeRetrievalExecutorTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final Instant EFFECTIVE_AT = Instant.parse("2026-09-01T12:00:00Z");
    private static final KnowledgeRetrievalContext CONTEXT = new KnowledgeRetrievalContext(
            TENANT_ID,
            UUID.fromString("7f50162a-8dc5-45b0-9c88-dc2f77135e0f"),
            UUID.fromString("133767cf-8ec8-487d-a09d-19d5efcece07"),
            "AUTHORIZATION_DECLINE_RATE_SPIKE",
            "Authorization declines elevated",
            "Synthetic authorization declines exceeded the observation threshold.",
            new KnowledgeEvidenceReference(
                    UUID.fromString("11111111-1111-4111-8111-111111111111"),
                    "AVAILABLE",
                    UUID.fromString("11111111-1111-4111-8111-111111111111"),
                    "payment-authorization",
                    List.of(new KnowledgeErrorCount("GATEWAY_TIMEOUT", 47))));

    @Test
    void returnsTheDerivedQueryEmbeddingOutcomeAllCandidatesAndProductionSelection() {
        Fixture fixture = fixture();
        float[] vector = unitVector();
        when(fixture.embeddingClient.embed(any()))
                .thenReturn(new KnowledgeEmbedding(
                        KnowledgeEmbeddingClient.MODEL_ID, KnowledgeEmbeddingClient.DIMENSIONS, true, vector));
        List<KnowledgeSearchCandidate> candidates = candidates();
        when(fixture.searchRepository.search(any())).thenReturn(candidates);

        KnowledgeRetrievalExecution execution = fixture.executor.execute(CONTEXT, EFFECTIVE_AT);

        assertThat(execution.query().templateVersion()).isEqualTo("knowledge-query/v2");
        assertThat(execution.query().text())
                .contains(
                        "Synthetic authorization declines exceeded the observation threshold.",
                        "Observed service: payment-authorization",
                        "GATEWAY_TIMEOUT count 47")
                .doesNotContain("Incident type:");
        verify(fixture.embeddingClient).embed(execution.query().text());
        ArgumentCaptor<KnowledgeSearchRequest> request = ArgumentCaptor.forClass(KnowledgeSearchRequest.class);
        verify(fixture.searchRepository).search(request.capture());
        assertThat(request.getValue().tenantId()).isEqualTo(TENANT_ID);
        assertThat(request.getValue().effectiveAt()).isEqualTo(EFFECTIVE_AT);
        assertThat(request.getValue().candidateDepth()).isEqualTo(20);
        assertThat(request.getValue().rrfK()).isEqualTo(60);
        assertThat(request.getValue().minimumLexicalRank()).isEqualTo(0.0f);
        assertThat(request.getValue().minimumVectorSimilarity()).isEqualTo(0.55f);
        assertThat(request.getValue().queryEmbedding()).containsExactly(vector);
        assertThat(execution.embeddingOutcome().status()).isEqualTo(QueryEmbeddingStatus.AVAILABLE);
        assertThat(execution.candidates()).containsExactlyElementsOf(candidates);
        assertThat(execution.selected()).hasSize(7);
        assertThat(execution.selected())
                .extracting(selected -> selected.candidate().documentType())
                .containsExactly(
                        KnowledgeDocumentType.RUNBOOK,
                        KnowledgeDocumentType.POLICY,
                        KnowledgeDocumentType.RUNBOOK,
                        KnowledgeDocumentType.POLICY,
                        KnowledgeDocumentType.RUNBOOK,
                        KnowledgeDocumentType.POLICY,
                        KnowledgeDocumentType.RUNBOOK);
        assertThat(execution.status()).isEqualTo(KnowledgeRetrievalStatus.AVAILABLE);
        assertThat(execution.statusDetail()).isNull();
        assertThat(execution.rankingVersion()).isEqualTo("postgres-hybrid-rrf/v2");
    }

    @Test
    void mapsEveryEmbeddingFailureThroughTheSameLexicalFallbackPath() {
        for (Failure failure : List.of(
                new Failure(new KnowledgeEmbeddingUnavailableException(), QueryEmbeddingStatus.UNAVAILABLE),
                new Failure(
                        new KnowledgeEmbeddingTimedOutException(new TimeoutException()),
                        QueryEmbeddingStatus.TIMED_OUT),
                new Failure(new KnowledgeEmbeddingMalformedException(), QueryEmbeddingStatus.MALFORMED))) {
            Fixture fixture = fixture();
            when(fixture.embeddingClient.embed(any())).thenThrow(failure.exception());
            when(fixture.searchRepository.search(any()))
                    .thenReturn(List.of(candidate(0, KnowledgeDocumentType.RUNBOOK)));

            KnowledgeRetrievalExecution execution = fixture.executor.execute(CONTEXT, EFFECTIVE_AT);

            assertThat(execution.embeddingOutcome().status()).isEqualTo(failure.status());
            assertThat(execution.status()).isEqualTo(KnowledgeRetrievalStatus.PARTIAL);
            assertThat(execution.selected()).hasSize(1);
            ArgumentCaptor<KnowledgeSearchRequest> request = ArgumentCaptor.forClass(KnowledgeSearchRequest.class);
            verify(fixture.searchRepository).search(request.capture());
            assertThat(request.getValue().queryEmbedding()).isNull();
            assertThat(request.getValue().embeddingModelId()).isEqualTo(KnowledgeEmbeddingClient.MODEL_ID);
            assertThat(request.getValue().embeddingDimensions()).isEqualTo(KnowledgeEmbeddingClient.DIMENSIONS);
        }
    }

    private static Fixture fixture() {
        KnowledgeEmbeddingClient embeddingClient = mock(KnowledgeEmbeddingClient.class);
        KnowledgeSearchRepository searchRepository = mock(KnowledgeSearchRepository.class);
        return new Fixture(
                new KnowledgeRetrievalExecutor(
                        embeddingClient,
                        searchRepository,
                        new KnowledgeContextSelector(4, 3),
                        new KnowledgeRetrievalQueryBuilder()),
                embeddingClient,
                searchRepository);
    }

    private static List<KnowledgeSearchCandidate> candidates() {
        List<KnowledgeSearchCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            KnowledgeDocumentType type = index % 2 == 0 ? KnowledgeDocumentType.RUNBOOK : KnowledgeDocumentType.POLICY;
            candidates.add(candidate(index, type));
        }
        return candidates;
    }

    private static KnowledgeSearchCandidate candidate(int index, KnowledgeDocumentType type) {
        return new KnowledgeSearchCandidate(
                TENANT_ID,
                new UUID(1L, index + 1L),
                new UUID(2L, index + 1L),
                new UUID(3L, index + 1L),
                type,
                "Document " + index,
                "1.0.0",
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Card authorization",
                "Diagnosis",
                "Synthetic source " + index,
                20,
                21,
                KnowledgeApprovalStatus.APPROVED,
                UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"),
                Instant.parse("2026-08-30T08:00:00Z"),
                Instant.parse("2026-08-30T09:00:00Z"),
                1.0f - index / 100.0f,
                index + 1,
                1.0f - index / 100.0f,
                index + 1,
                2.0d / (61 + index));
    }

    private static float[] unitVector() {
        float[] vector = new float[KnowledgeEmbeddingClient.DIMENSIONS];
        vector[0] = 1.0f;
        return vector;
    }

    private record Fixture(
            KnowledgeRetrievalExecutor executor,
            KnowledgeEmbeddingClient embeddingClient,
            KnowledgeSearchRepository searchRepository) {}

    private record Failure(RuntimeException exception, QueryEmbeddingStatus status) {}
}
