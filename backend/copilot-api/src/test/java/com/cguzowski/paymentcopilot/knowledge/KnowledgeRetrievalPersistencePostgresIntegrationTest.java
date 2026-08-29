package com.cguzowski.paymentcopilot.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import com.cguzowski.paymentcopilot.incident.EvidenceCollectionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class KnowledgeRetrievalPersistencePostgresIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("076a18a3-d54f-486a-b3ec-189e1048fd28");
    private static final UUID INCIDENT_ID = UUID.fromString("ce22cb8d-10d6-4d6d-9a56-f644ae84573d");
    private static final UUID INVESTIGATION_ID = UUID.fromString("7f50162a-8dc5-45b0-9c88-dc2f77135e0f");
    private static final UUID CORRELATION_ID = UUID.fromString("133767cf-8ec8-487d-a09d-19d5efcece07");
    private static final UUID RETRIEVAL_ID = UUID.fromString("a74f88ed-e295-4caf-9404-a22f733d86ec");
    private static final UUID EVIDENCE_AVAILABLE_ID = UUID.fromString("d3a12cd9-ef1d-4328-b63d-2ecea6558e2d");
    private static final UUID EVIDENCE_UNAVAILABLE_ID = UUID.fromString("5b8e57e4-e194-4f51-81dc-4d2e6e47103a");
    private static final Instant REQUESTED_AT = Instant.parse("2026-08-28T10:00:00Z");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg17")
            .withDatabaseName("payment_copilot")
            .withUsername("payment_copilot")
            .withPassword("test_only_password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private KnowledgeRetrievalPersistenceService persistence;

    @Autowired
    private KnowledgeIndexPersistenceService indexPersistence;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM knowledge_retrieval_result").update();
        jdbcClient.sql("DELETE FROM knowledge_retrieval_attempt").update();
        jdbcClient.sql("DELETE FROM knowledge_chunk").update();
        jdbcClient.sql("DELETE FROM knowledge_document_version").update();
        jdbcClient.sql("DELETE FROM evidence_collection_attempt").update();
        jdbcClient.sql("DELETE FROM investigation").update();
        jdbcClient.sql("DELETE FROM incident").update();
        insertIncidentAndInvestigation();
        insertEvidenceAttempts();
        indexPersistence.insert(indexedDocument());
    }

    @Test
    void persistsCompleteRetrievalSnapshotAndEveryRetryNewestFirst() {
        KnowledgeRetrievalContext context = persistence.findContext(TENANT_ID, INVESTIGATION_ID).orElseThrow();
        assertThat(context.evidence().latestAttemptId()).isEqualTo(EVIDENCE_UNAVAILABLE_ID);
        assertThat(context.evidence().latestStatus()).isEqualTo(EvidenceCollectionStatus.UNAVAILABLE);
        assertThat(context.evidence().applicableAttemptId()).isEqualTo(EVIDENCE_AVAILABLE_ID);
        assertThat(context.evidence().applicableContent().errors().getFirst().errorCode())
                .isEqualTo("GATEWAY_TIMEOUT");
        assertThat(persistence.findContext(OTHER_TENANT_ID, INVESTIGATION_ID)).isEmpty();

        KnowledgeRetrievalAttempt started = started(RETRIEVAL_ID, context, REQUESTED_AT);
        persistence.insertStarted(started);
        KnowledgeRetrievalAttempt completed = started.complete(
                KnowledgeRetrievalStatus.AVAILABLE,
                Instant.parse("2026-08-28T10:00:02Z"),
                null,
                List.of(new SelectedKnowledgeChunk(candidate(), 1, 1)));

        assertThat(persistence.complete(completed)).isTrue();
        assertThat(persistence.complete(completed)).isFalse();

        UUID retryId = UUID.fromString("d84b2fb0-3436-4c61-afdf-a673535fc6cc");
        KnowledgeRetrievalAttempt retry = started(
                retryId,
                context,
                Instant.parse("2026-08-28T10:01:00Z"));
        persistence.insertStarted(retry);

        List<KnowledgeRetrievalAttempt> history = persistence.findAll(TENANT_ID, INVESTIGATION_ID);
        assertThat(history).extracting(KnowledgeRetrievalAttempt::retrievalId)
                .containsExactly(retryId, RETRIEVAL_ID);
        assertThat(history.getFirst().status()).isEqualTo(KnowledgeRetrievalStatus.STARTED);
        assertThat(history.getLast().status()).isEqualTo(KnowledgeRetrievalStatus.AVAILABLE);
        assertThat(history.getLast().results()).hasSize(1);
        KnowledgeRetrievalResult result = history.getLast().results().getFirst();
        assertThat(result.documentId()).isEqualTo(candidate().documentId());
        assertThat(result.documentVersionId()).isEqualTo(candidate().documentVersionId());
        assertThat(result.chunkId()).isEqualTo(candidate().chunkId());
        assertThat(result.rawContent()).isEqualTo("Inspect GATEWAY_TIMEOUT observations.");
        assertThat(result.vectorDistance()).isZero();
        assertThat(result.fusedPosition()).isEqualTo(1);
        assertThat(result.selectedPosition()).isEqualTo(1);
        assertThat(jdbcClient.sql("SELECT status FROM incident WHERE id = :id")
                        .param("id", INCIDENT_ID)
                        .query(String.class)
                        .single())
                .isEqualTo("INVESTIGATING");
    }

    private static KnowledgeRetrievalAttempt started(
            UUID retrievalId,
            KnowledgeRetrievalContext context,
            Instant requestedAt) {
        DerivedKnowledgeQuery query = new KnowledgeRetrievalQueryBuilder().build(context);
        return KnowledgeRetrievalAttempt.started(
                retrievalId,
                context,
                requestedAt,
                query,
                new KnowledgeMetadataFilters(
                        "AUTHORIZATION_DECLINE_RATE_SPIKE",
                        List.of(KnowledgeDocumentType.RUNBOOK, KnowledgeDocumentType.POLICY),
                        KnowledgeApprovalStatus.APPROVED,
                        requestedAt),
                KnowledgeRetrievalService.RANKING_VERSION,
                60,
                20,
                0.0f,
                0.55f);
    }

    private void insertIncidentAndInvestigation() {
        jdbcClient.sql("""
                        INSERT INTO incident (
                            id, tenant_id, external_alert_id, incident_type, severity,
                            status, summary, description, occurred_at, received_at
                        ) VALUES (
                            :id, :tenantId, 'synthetic-alert-knowledge-001',
                            'AUTHORIZATION_DECLINE_RATE_SPIKE', 'HIGH', 'INVESTIGATING',
                            'Authorization declines elevated',
                            'Synthetic authorization declines exceeded the observation threshold.',
                            TIMESTAMPTZ '2026-08-28 09:55:00Z',
                            TIMESTAMPTZ '2026-08-28 09:56:00Z'
                        )
                        """)
                .param("id", INCIDENT_ID)
                .param("tenantId", TENANT_ID)
                .update();
        jdbcClient.sql("""
                        INSERT INTO investigation (
                            id, tenant_id, incident_id, started_by, started_at, correlation_id
                        ) VALUES (
                            :id, :tenantId, :incidentId, :startedBy,
                            TIMESTAMPTZ '2026-08-28 09:57:00Z', :correlationId
                        )
                        """)
                .param("id", INVESTIGATION_ID)
                .param("tenantId", TENANT_ID)
                .param("incidentId", INCIDENT_ID)
                .param("startedBy", UUID.fromString("6904706f-d9e6-4543-a1bf-fc4b729e4c05"))
                .param("correlationId", CORRELATION_ID)
                .update();
    }

    private void insertEvidenceAttempts() {
        jdbcClient.sql("""
                        INSERT INTO evidence_collection_attempt (
                            id, tenant_id, investigation_id, tool_call_id,
                            investigation_correlation_id, source_system, source_tool,
                            scenario_reference, status, requested_at, retrieved_at,
                            completed_at, content_schema_version, content
                        ) VALUES (
                            :id, :tenantId, :investigationId, :toolCallId,
                            :correlationId, 'synthetic-observability', 'getRecentServiceErrors',
                            'synthetic-alert-knowledge-001', 'AVAILABLE',
                            TIMESTAMPTZ '2026-08-28 09:58:00Z',
                            TIMESTAMPTZ '2026-08-28 09:58:01Z',
                            TIMESTAMPTZ '2026-08-28 09:58:02Z', 'service-errors/v1',
                            CAST(:content AS JSONB)
                        )
                        """)
                .param("id", EVIDENCE_AVAILABLE_ID)
                .param("tenantId", TENANT_ID)
                .param("investigationId", INVESTIGATION_ID)
                .param("toolCallId", UUID.fromString("7a04902c-dc0a-4477-878b-698caadf37aa"))
                .param("correlationId", CORRELATION_ID)
                .param("content", """
                        {"serviceName":"authorization-gateway","observedFrom":"2026-08-28T09:55:00Z","observedTo":"2026-08-28T09:58:00Z","errors":[{"sourceEventId":"evt-1","observedAt":"2026-08-28T09:57:00Z","errorCode":"GATEWAY_TIMEOUT","count":12}]}""")
                .update();
        jdbcClient.sql("""
                        INSERT INTO evidence_collection_attempt (
                            id, tenant_id, investigation_id, tool_call_id,
                            investigation_correlation_id, source_system, source_tool,
                            scenario_reference, status, requested_at, completed_at,
                            content_schema_version, status_detail
                        ) VALUES (
                            :id, :tenantId, :investigationId, :toolCallId,
                            :correlationId, 'synthetic-observability', 'getRecentServiceErrors',
                            'synthetic-alert-knowledge-001', 'UNAVAILABLE',
                            TIMESTAMPTZ '2026-08-28 09:59:00Z',
                            TIMESTAMPTZ '2026-08-28 09:59:02Z', 'service-errors/v1',
                            'Synthetic source unavailable.'
                        )
                        """)
                .param("id", EVIDENCE_UNAVAILABLE_ID)
                .param("tenantId", TENANT_ID)
                .param("investigationId", INVESTIGATION_ID)
                .param("toolCallId", UUID.fromString("38e1fc2e-ab72-43f4-9177-d43753af2c43"))
                .param("correlationId", CORRELATION_ID)
                .update();
    }

    private static IndexedKnowledgeDocument indexedDocument() {
        Instant indexedAt = Instant.parse("2026-08-28T09:50:00Z");
        ApprovedKnowledgeDocument document = new ApprovedKnowledgeDocument(
                candidate().documentId(),
                TENANT_ID,
                KnowledgeDocumentType.RUNBOOK,
                "Authorization Decline Runbook",
                "1.0.0",
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Card authorization",
                KnowledgeApprovalStatus.APPROVED,
                candidate().approvedBy(),
                candidate().approvedAt(),
                candidate().effectiveAt(),
                "fixture.md",
                1,
                "# Authorization Decline Runbook\n\n## Diagnosis\n\nInspect GATEWAY_TIMEOUT observations.\n");
        KnowledgeChunkDraft draft = new KnowledgeChunkDraft(
                0,
                "Diagnosis",
                candidate().rawContent(),
                "Document: Authorization Decline Runbook\nSection: Diagnosis\nType: RUNBOOK\nApplies to: Card authorization\n\n"
                        + candidate().rawContent(),
                "b".repeat(64),
                "c".repeat(64),
                "embedding-input/v1",
                5,
                5,
                10);
        IndexedKnowledgeChunk chunk = new IndexedKnowledgeChunk(
                candidate().chunkId(),
                draft,
                new KnowledgeEmbedding(
                        SpringAiTitanKnowledgeEmbeddingClient.MODEL_ID,
                        1024,
                        true,
                        unitVector()),
                indexedAt);
        return new IndexedKnowledgeDocument(
                candidate().documentVersionId(),
                document,
                "a".repeat(64),
                indexedAt,
                List.of(chunk));
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
        float[] vector = new float[1024];
        vector[0] = 1.0f;
        return vector;
    }
}
