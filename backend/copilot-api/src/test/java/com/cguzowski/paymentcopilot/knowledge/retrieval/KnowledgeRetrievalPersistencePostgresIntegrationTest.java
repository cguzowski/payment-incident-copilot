package com.cguzowski.paymentcopilot.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
        insertIndexedDocument();
    }

    @Test
    void persistsCompleteRetrievalSnapshotAndEveryRetryNewestFirst() {
        KnowledgeRetrievalContext context = retrievalContext();

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
        KnowledgeRetrievalAttempt retry = started(retryId, context, Instant.parse("2026-08-28T10:01:00Z"));
        persistence.insertStarted(retry);

        List<KnowledgeRetrievalAttempt> history = persistence.findAll(TENANT_ID, INVESTIGATION_ID);
        assertThat(history).extracting(KnowledgeRetrievalAttempt::retrievalId).containsExactly(retryId, RETRIEVAL_ID);
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
        assertThat(jdbcClient
                        .sql("SELECT status FROM incident WHERE id = :id")
                        .param("id", INCIDENT_ID)
                        .query(String.class)
                        .single())
                .isEqualTo("INVESTIGATING");
    }

    private static KnowledgeRetrievalAttempt started(
            UUID retrievalId, KnowledgeRetrievalContext context, Instant requestedAt) {
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

    private static KnowledgeRetrievalContext retrievalContext() {
        return new KnowledgeRetrievalContext(
                TENANT_ID,
                INVESTIGATION_ID,
                CORRELATION_ID,
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Authorization declines elevated",
                "Synthetic authorization declines exceeded the observation threshold.",
                new KnowledgeEvidenceReference(
                        EVIDENCE_UNAVAILABLE_ID,
                        "UNAVAILABLE",
                        EVIDENCE_AVAILABLE_ID,
                        "authorization-gateway",
                        List.of(new KnowledgeErrorCount("GATEWAY_TIMEOUT", 12))));
    }

    private void insertIncidentAndInvestigation() {
        jdbcClient
                .sql("""
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
        jdbcClient
                .sql("""
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
        jdbcClient
                .sql("""
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
        jdbcClient
                .sql("""
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

    private void insertIndexedDocument() {
        KnowledgeSearchCandidate candidate = candidate();
        jdbcClient
                .sql("""
                        INSERT INTO knowledge_document_version (
                            id, tenant_id, document_id, document_type, title,
                            document_version, incident_family, applies_to,
                            approval_status, approved_by, approved_at, effective_at,
                            source_name, source_content_hash, imported_at
                        ) VALUES (
                            :id, :tenantId, :documentId, 'RUNBOOK', :title,
                            :documentVersion, :incidentFamily, :appliesTo,
                            'APPROVED', :approvedBy, :approvedAt, :effectiveAt,
                            'fixture.md', :sourceHash, :importedAt
                        )
                        """)
                .param("id", candidate.documentVersionId())
                .param("tenantId", TENANT_ID)
                .param("documentId", candidate.documentId())
                .param("title", candidate.documentTitle())
                .param("documentVersion", candidate.documentVersion())
                .param("incidentFamily", candidate.incidentFamily())
                .param("appliesTo", candidate.appliesTo())
                .param("approvedBy", candidate.approvedBy())
                .param("approvedAt", utc(candidate.approvedAt()))
                .param("effectiveAt", utc(candidate.effectiveAt()))
                .param("sourceHash", "a".repeat(64))
                .param("importedAt", utc(Instant.parse("2026-08-28T09:50:00Z")))
                .update();
        jdbcClient
                .sql("""
                        INSERT INTO knowledge_chunk (
                            id, tenant_id, document_version_id, chunk_ordinal,
                            section_path, raw_content, embedding_input,
                            raw_content_hash, embedding_input_hash,
                            embedding_input_template_version, chunking_strategy_version,
                            source_start_line, source_end_line, estimated_tokens,
                            embedding_model_id, embedding_dimensions,
                            embedding_normalized, embedded_at, embedding
                        ) VALUES (
                            :id, :tenantId, :documentVersionId, 0,
                            :sectionPath, :rawContent, :embeddingInput,
                            :rawHash, :embeddingHash,
                            'embedding-input/v1', 'markdown-sections/v1',
                            :sourceStartLine, :sourceEndLine, 10,
                            'amazon.titan-embed-text-v2:0', 1024,
                            TRUE, :embeddedAt, CAST(:embedding AS vector)
                        )
                        """)
                .param("id", candidate.chunkId())
                .param("tenantId", TENANT_ID)
                .param("documentVersionId", candidate.documentVersionId())
                .param("sectionPath", candidate.sectionPath())
                .param("rawContent", candidate.rawContent())
                .param(
                        "embeddingInput",
                        "Document: Authorization Decline Runbook\nSection: Diagnosis\nType: RUNBOOK\n"
                                + "Applies to: Card authorization\n\n" + candidate.rawContent())
                .param("rawHash", "b".repeat(64))
                .param("embeddingHash", "c".repeat(64))
                .param("sourceStartLine", candidate.sourceStartLine())
                .param("sourceEndLine", candidate.sourceEndLine())
                .param("embeddedAt", utc(Instant.parse("2026-08-28T09:50:00Z")))
                .param("embedding", vectorLiteral(unitVector()))
                .update();
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

    private static String vectorLiteral(float[] vector) {
        StringBuilder value = new StringBuilder("[");
        for (int index = 0; index < vector.length; index++) {
            if (index > 0) {
                value.append(',');
            }
            value.append(Float.toString(vector[index]));
        }
        return value.append(']').toString();
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
