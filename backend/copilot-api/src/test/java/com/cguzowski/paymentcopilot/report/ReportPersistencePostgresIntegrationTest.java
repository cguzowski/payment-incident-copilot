package com.cguzowski.paymentcopilot.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cguzowski.paymentcopilot.evidence.ReportEvidenceObservation;
import com.cguzowski.paymentcopilot.evidence.ReportEvidenceSnapshot;
import com.cguzowski.paymentcopilot.incident.ReportInvestigationSnapshot;
import com.cguzowski.paymentcopilot.knowledge.retrieval.ReportKnowledgeSnapshot;
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
class ReportPersistencePostgresIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("076a18a3-d54f-486a-b3ec-189e1048fd28");
    private static final UUID INCIDENT_ID = UUID.fromString("ce22cb8d-10d6-4d6d-9a56-f644ae84573d");
    private static final UUID INVESTIGATION_ID = UUID.fromString("7f50162a-8dc5-45b0-9c88-dc2f77135e0f");
    private static final UUID CORRELATION_ID = UUID.fromString("133767cf-8ec8-487d-a09d-19d5efcece07");
    private static final UUID OPERATOR_ID = UUID.fromString("6904706f-d9e6-4543-a1bf-fc4b729e4c05");
    private static final UUID EVIDENCE_ID = UUID.fromString("d3a12cd9-ef1d-4328-b63d-2ecea6558e2d");
    private static final UUID RETRIEVAL_ID = UUID.fromString("a74f88ed-e295-4caf-9404-a22f733d86ec");

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
    private ReportGenerationPersistenceService persistence;

    @BeforeEach
    void setUp() {
        jdbcClient.sql("DELETE FROM report_claim_knowledge_reference").update();
        jdbcClient.sql("DELETE FROM report_claim_evidence_reference").update();
        jdbcClient.sql("DELETE FROM report_claim").update();
        jdbcClient.sql("DELETE FROM report_generation_attempt").update();
        jdbcClient.sql("DELETE FROM knowledge_retrieval_result").update();
        jdbcClient.sql("DELETE FROM knowledge_retrieval_attempt").update();
        jdbcClient.sql("DELETE FROM knowledge_chunk").update();
        jdbcClient.sql("DELETE FROM knowledge_document_version").update();
        jdbcClient.sql("DELETE FROM evidence_collection_attempt").update();
        jdbcClient.sql("DELETE FROM investigation").update();
        jdbcClient.sql("DELETE FROM incident").update();
        insertPrerequisites();
    }

    @Test
    void atomicallyPersistsValidatedReportClaimsAndMovesIncidentToAwaitingReview() {
        ReportGenerationAttempt started =
                started(UUID.fromString("28165339-8e37-49c7-9859-493277b34da2"), Instant.parse("2026-08-29T10:00:00Z"));
        assertThat(persistence.start(started)).isTrue();

        ReportDocument document = insufficientReport();
        ReportGenerationAttempt completed = started.completeAvailable(
                Instant.parse("2026-08-29T10:00:02Z"), new ReportModelResponse("discarded", "request-1"), document);

        assertThat(persistence.completeAvailable(completed)).isTrue();
        assertThat(persistence.findAll(TENANT_ID, INVESTIGATION_ID)).containsExactly(completed);
        assertThat(persistence.findAll(OTHER_TENANT_ID, INVESTIGATION_ID)).isEmpty();
        assertThat(jdbcClient
                        .sql("SELECT status FROM incident WHERE tenant_id = :tenantId AND id = :incidentId")
                        .param("tenantId", TENANT_ID)
                        .param("incidentId", INCIDENT_ID)
                        .query(String.class)
                        .single())
                .isEqualTo("AWAITING_REVIEW");
        assertThat(jdbcClient
                        .sql("SELECT COUNT(*) FROM report_claim WHERE attempt_id = :attemptId")
                        .param("attemptId", started.attemptId())
                        .query(Integer.class)
                        .single())
                .isEqualTo(3);
        assertThat(jdbcClient
                        .sql("SELECT COUNT(*) FROM report_claim_evidence_reference WHERE attempt_id = :attemptId")
                        .param("attemptId", started.attemptId())
                        .query(Integer.class)
                        .single())
                .isEqualTo(3);
        assertThat(jdbcClient
                        .sql("SELECT report_content::text FROM report_generation_attempt WHERE id = :attemptId")
                        .param("attemptId", started.attemptId())
                        .query(String.class)
                        .single())
                .doesNotContain("discarded");
        assertThat(persistence.start(started(UUID.randomUUID(), Instant.parse("2026-08-29T10:01:00Z"))))
                .isFalse();
    }

    @Test
    void preservesTerminalFailureAndAllowsAppendOnlyRetry() {
        ReportGenerationAttempt first =
                started(UUID.fromString("28165339-8e37-49c7-9859-493277b34da2"), Instant.parse("2026-08-29T10:00:00Z"));
        assertThat(persistence.start(first)).isTrue();
        ReportGenerationAttempt failed = first.completeFailure(
                ReportGenerationStatus.TIMED_OUT,
                Instant.parse("2026-08-29T10:00:02Z"),
                null,
                "The report model timed out.");
        assertThat(persistence.completeFailure(failed)).isTrue();
        ReportGenerationAttempt retry =
                started(UUID.fromString("3c1031d5-93b8-42d0-b338-b4bb2448c339"), Instant.parse("2026-08-29T10:01:00Z"));

        assertThat(persistence.start(retry)).isTrue();
        assertThat(persistence.findAll(TENANT_ID, INVESTIGATION_ID)).containsExactly(retry, failed);
        assertThat(jdbcClient
                        .sql("SELECT status FROM incident WHERE id = :incidentId")
                        .param("incidentId", INCIDENT_ID)
                        .query(String.class)
                        .single())
                .isEqualTo("INVESTIGATING");
    }

    @Test
    void terminalizesAnInterruptedAttemptBeforeAppendingRetry() {
        ReportGenerationAttempt interrupted = started(UUID.randomUUID(), Instant.parse("2026-08-29T09:40:00Z"));
        assertThat(persistence.start(interrupted)).isTrue();
        ReportGenerationAttempt retry = started(UUID.randomUUID(), Instant.parse("2026-08-29T10:00:01Z"));

        assertThat(persistence.start(retry)).isTrue();

        List<ReportGenerationAttempt> history = persistence.findAll(TENANT_ID, INVESTIGATION_ID);
        assertThat(history)
                .extracting(ReportGenerationAttempt::attemptId)
                .containsExactly(retry.attemptId(), interrupted.attemptId());
        assertThat(history.getLast().status()).isEqualTo(ReportGenerationStatus.UNAVAILABLE);
        assertThat(history.getLast().statusDetail()).isEqualTo("Generation was interrupted before completion.");
    }

    @Test
    void rollsBackAvailableReportWhenLifecycleTransitionCannotCommit() {
        ReportGenerationAttempt started = started(UUID.randomUUID(), Instant.parse("2026-08-29T10:00:00Z"));
        assertThat(persistence.start(started)).isTrue();
        jdbcClient
                .sql("UPDATE incident SET status = 'AWAITING_REVIEW' WHERE id = :incidentId")
                .param("incidentId", INCIDENT_ID)
                .update();
        ReportGenerationAttempt completed = started.completeAvailable(
                Instant.parse("2026-08-29T10:00:02Z"),
                new ReportModelResponse("discarded", null),
                insufficientReport());

        assertThatThrownBy(() -> persistence.completeAvailable(completed)).isInstanceOf(IllegalStateException.class);

        assertThat(jdbcClient
                        .sql("SELECT status FROM report_generation_attempt WHERE id = :attemptId")
                        .param("attemptId", started.attemptId())
                        .query(String.class)
                        .single())
                .isEqualTo("STARTED");
        assertThat(jdbcClient
                        .sql("SELECT COUNT(*) FROM report_claim WHERE attempt_id = :attemptId")
                        .param("attemptId", started.attemptId())
                        .query(Integer.class)
                        .single())
                .isZero();
    }

    private static ReportGenerationAttempt started(UUID attemptId, Instant requestedAt) {
        return ReportGenerationAttempt.started(
                attemptId,
                OPERATOR_ID,
                requestedAt,
                context(),
                "global.amazon.nova-2-lite-v1:0",
                new ReportPrompt("not persisted", "report-prompt/v1", "a".repeat(64), "report-v1", "b".repeat(64)));
    }

    private static ReportGenerationContext context() {
        return new ReportGenerationContext(
                new ReportInvestigationSnapshot(
                        TENANT_ID,
                        INVESTIGATION_ID,
                        INCIDENT_ID,
                        CORRELATION_ID,
                        "INVESTIGATING",
                        "AUTHORIZATION_DECLINE_RATE_SPIKE",
                        "Authorization declines elevated",
                        "Synthetic incident."),
                new ReportEvidenceSnapshot(
                        EVIDENCE_ID,
                        "AVAILABLE",
                        EVIDENCE_ID,
                        "authorization-gateway",
                        List.of(new ReportEvidenceObservation(
                                "evt-1", Instant.parse("2026-08-29T08:00:00Z"), "GATEWAY_TIMEOUT", 12))),
                new ReportKnowledgeSnapshot(RETRIEVAL_ID, "NO_MATCH", List.of()));
    }

    private static ReportDocument insufficientReport() {
        ReportClaim claim = new ReportClaim("Available evidence is insufficient.", List.of(EVIDENCE_ID), List.of());
        return new ReportDocument(
                ReportDisposition.INSUFFICIENT_EVIDENCE,
                claim,
                List.of(claim),
                List.of(),
                null,
                new ReportConfidence(
                        ReportConfidenceLevel.LOW, "Approved guidance is unavailable.", List.of(EVIDENCE_ID)),
                null,
                List.of(),
                List.of(new ReportGap("No approved knowledge matched.")));
    }

    private void insertPrerequisites() {
        jdbcClient
                .sql("""
                        INSERT INTO incident (
                            id, tenant_id, external_alert_id, incident_type, severity,
                            status, summary, description, occurred_at, received_at
                        ) VALUES (
                            :incidentId, :tenantId, 'synthetic-report-alert',
                            'AUTHORIZATION_DECLINE_RATE_SPIKE', 'HIGH', 'INVESTIGATING',
                            'Authorization declines elevated', 'Synthetic incident.',
                            TIMESTAMPTZ '2026-08-29 09:55:00Z', TIMESTAMPTZ '2026-08-29 09:56:00Z'
                        )
                        """)
                .param("incidentId", INCIDENT_ID)
                .param("tenantId", TENANT_ID)
                .update();
        jdbcClient
                .sql("""
                        INSERT INTO investigation (
                            id, tenant_id, incident_id, started_by, started_at, correlation_id
                        ) VALUES (
                            :investigationId, :tenantId, :incidentId, :operatorId,
                            TIMESTAMPTZ '2026-08-29 09:57:00Z', :correlationId
                        )
                        """)
                .param("investigationId", INVESTIGATION_ID)
                .param("tenantId", TENANT_ID)
                .param("incidentId", INCIDENT_ID)
                .param("operatorId", OPERATOR_ID)
                .param("correlationId", CORRELATION_ID)
                .update();
        jdbcClient
                .sql("""
                        INSERT INTO evidence_collection_attempt (
                            id, tenant_id, investigation_id, tool_call_id,
                            investigation_correlation_id, source_system, source_tool,
                            scenario_reference, status, requested_at, retrieved_at,
                            completed_at, content_schema_version, content
                        ) VALUES (
                            :evidenceId, :tenantId, :investigationId, :toolCallId,
                            :correlationId, 'synthetic-observability', 'getRecentServiceErrors',
                            'synthetic-report-alert', 'AVAILABLE',
                            TIMESTAMPTZ '2026-08-29 09:58:00Z', TIMESTAMPTZ '2026-08-29 09:58:01Z',
                            TIMESTAMPTZ '2026-08-29 09:58:02Z', 'service-errors/v1',
                            '{"serviceName":"authorization-gateway","observedFrom":"2026-08-29T09:55:00Z","observedTo":"2026-08-29T09:58:00Z","errors":[]}'::jsonb
                        )
                        """)
                .param("evidenceId", EVIDENCE_ID)
                .param("tenantId", TENANT_ID)
                .param("investigationId", INVESTIGATION_ID)
                .param("toolCallId", UUID.randomUUID())
                .param("correlationId", CORRELATION_ID)
                .update();
        jdbcClient
                .sql("""
                        INSERT INTO knowledge_retrieval_attempt (
                            id, tenant_id, investigation_id, investigation_correlation_id,
                            status, requested_at, completed_at, query_text,
                            query_template_version, contributing_evidence_ids,
                            embedding_model_id, embedding_dimensions, metadata_filters,
                            ranking_version, rrf_k, candidate_depth, minimum_lexical_rank,
                            minimum_vector_similarity, status_detail
                        ) VALUES (
                            :retrievalId, :tenantId, :investigationId, :correlationId,
                            'NO_MATCH', TIMESTAMPTZ '2026-08-29 09:59:00Z',
                            TIMESTAMPTZ '2026-08-29 09:59:02Z', 'synthetic query',
                            'knowledge-query/v1', ARRAY[:evidenceId]::uuid[],
                            'amazon.titan-embed-text-v2:0', 1024, '{}'::jsonb,
                            'postgres-hybrid-rrf/v1', 60, 20, 0, 0.55,
                            'No approved knowledge matched.'
                        )
                        """)
                .param("retrievalId", RETRIEVAL_ID)
                .param("tenantId", TENANT_ID)
                .param("investigationId", INVESTIGATION_ID)
                .param("correlationId", CORRELATION_ID)
                .param("evidenceId", EVIDENCE_ID)
                .update();
    }
}
