package com.cguzowski.paymentcopilot.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cguzowski.paymentcopilot.evidence.EvidenceTimelineSnapshotProvider;
import com.cguzowski.paymentcopilot.incident.IncidentTimelineSnapshotProvider;
import com.cguzowski.paymentcopilot.knowledge.retrieval.KnowledgeTimelineSnapshotProvider;
import com.cguzowski.paymentcopilot.report.ReportTimelineSnapshotProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class HumanDecisionPersistencePostgresIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("076a18a3-d54f-486a-b3ec-189e1048fd28");
    private static final UUID DECISION_ID = UUID.fromString("955865d8-f60a-4c37-a7f4-92d51b41f01a");
    private static final UUID INCIDENT_ID = UUID.fromString("ce22cb8d-10d6-4d6d-9a56-f644ae84573d");
    private static final UUID INVESTIGATION_ID = UUID.fromString("7f50162a-8dc5-45b0-9c88-dc2f77135e0f");
    private static final UUID CORRELATION_ID = UUID.fromString("133767cf-8ec8-487d-a09d-19d5efcece07");
    private static final UUID REPORT_ATTEMPT_ID = UUID.fromString("28165339-8e37-49c7-9859-493277b34da2");
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
    private HumanDecisionPersistenceService persistence;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private IncidentTimelineSnapshotProvider incidentTimeline;

    @Autowired
    private EvidenceTimelineSnapshotProvider evidenceTimeline;

    @Autowired
    private KnowledgeTimelineSnapshotProvider knowledgeTimeline;

    @Autowired
    private ReportTimelineSnapshotProvider reportTimeline;

    @Autowired
    private DecisionTimelineSnapshotProvider decisionTimeline;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
        jdbcClient.sql("DELETE FROM human_decision").update();
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
        insertReviewCandidate();
    }

    @Test
    void commitsDecisionAndTerminalStateInOneTransaction() {
        HumanDecision decision = decision(DecisionOutcome.APPROVED, "Reviewed against the cited sources.");

        HumanDecisionRecordResult result = persistence.record(decision);

        assertThat(result).isEqualTo(new HumanDecisionRecordResult(decision, true));
        assertThat(persistence.find(TENANT_ID, INVESTIGATION_ID)).contains(decision);
        assertThat(persistence.find(OTHER_TENANT_ID, INVESTIGATION_ID)).isEmpty();
        assertThat(jdbcClient
                        .sql("SELECT status FROM incident WHERE tenant_id = :tenantId AND id = :incidentId")
                        .param("tenantId", TENANT_ID)
                        .param("incidentId", INCIDENT_ID)
                        .query(String.class)
                        .single())
                .isEqualTo("APPROVED");
    }

    @Test
    void returnsExistingDecisionForExactReplayAndRejectsDifferentDecision() {
        HumanDecision decision = decision(DecisionOutcome.REJECTED, "The proposed cause is not supported.");
        assertThat(persistence.record(decision).created()).isTrue();

        assertThat(persistence.record(decision)).isEqualTo(new HumanDecisionRecordResult(decision, false));
        assertThatThrownBy(() -> persistence.record(new HumanDecision(
                        UUID.randomUUID(),
                        TENANT_ID,
                        INVESTIGATION_ID,
                        INCIDENT_ID,
                        CORRELATION_ID,
                        REPORT_ATTEMPT_ID,
                        OPERATOR_ID,
                        DecisionOutcome.APPROVED,
                        "Changed outcome.",
                        Instant.parse("2026-08-30T12:01:00Z"))))
                .isInstanceOf(HumanDecisionConflictException.class);
        assertThat(jdbcClient
                        .sql("SELECT COUNT(*) FROM human_decision")
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
    }

    @Test
    void rollsBackDecisionWhenLifecycleTransitionFails() {
        jdbcClient
                .sql("UPDATE incident SET status = 'INVESTIGATING' WHERE id = :incidentId")
                .param("incidentId", INCIDENT_ID)
                .update();

        assertThatThrownBy(() -> persistence.record(decision(DecisionOutcome.APPROVED, "Reviewed.")))
                .isInstanceOf(HumanDecisionConflictException.class);

        assertThat(jdbcClient
                        .sql("SELECT COUNT(*) FROM human_decision")
                        .query(Integer.class)
                        .single())
                .isZero();
        assertThat(jdbcClient
                        .sql("SELECT status FROM incident WHERE id = :incidentId")
                        .param("incidentId", INCIDENT_ID)
                        .query(String.class)
                        .single())
                .isEqualTo("INVESTIGATING");
    }

    @Test
    void appliesV8WithNullableHistoricalAttemptActors() {
        assertThat(jdbcClient
                        .sql("SELECT requested_by FROM evidence_collection_attempt WHERE id = :id")
                        .param("id", EVIDENCE_ID)
                        .query(UUID.class)
                        .optional())
                .isEmpty();
        assertThat(jdbcClient
                        .sql("SELECT requested_by FROM knowledge_retrieval_attempt WHERE id = :id")
                        .param("id", RETRIEVAL_ID)
                        .query(UUID.class)
                        .optional())
                .isEmpty();
    }

    @Test
    void allowsExactlyOneOfTwoConcurrentConflictingDecisions() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        HumanDecision approval = decisionWithId(
                UUID.fromString("155865d8-f60a-4c37-a7f4-92d51b41f01a"),
                DecisionOutcome.APPROVED,
                "Approve after review.");
        HumanDecision rejection = decisionWithId(
                UUID.fromString("255865d8-f60a-4c37-a7f4-92d51b41f01a"),
                DecisionOutcome.REJECTED,
                "Reject after review.");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = List.of(
                    executor.submit(() -> concurrentRecord(approval, ready, start)),
                    executor.submit(() -> concurrentRecord(rejection, ready, start)));
            ready.await();
            start.countDown();
            List<Object> outcomes = List.of(results.get(0).get(), results.get(1).get());

            assertThat(outcomes)
                    .filteredOn(HumanDecisionRecordResult.class::isInstance)
                    .hasSize(1);
            assertThat(outcomes)
                    .filteredOn(HumanDecisionConflictException.class::isInstance)
                    .hasSize(1);
        }
        assertThat(jdbcClient
                        .sql("SELECT COUNT(*) FROM human_decision")
                        .query(Integer.class)
                        .single())
                .isOne();
        String terminalStatus = jdbcClient
                .sql("SELECT status FROM incident WHERE id = :incidentId")
                .param("incidentId", INCIDENT_ID)
                .query(String.class)
                .single();
        assertThat(terminalStatus).isIn("APPROVED", "REJECTED");
    }

    @Test
    void publishesTenantScopedTimelineSnapshotsWithoutInventingHistoricalActors() throws Exception {
        persistence.record(decision(DecisionOutcome.REJECTED, "The evidence is insufficient."));

        assertThat(incidentTimeline.findTimelineSnapshot(TENANT_ID, INVESTIGATION_ID))
                .isPresent();
        assertThat(evidenceTimeline.findTimelineSnapshots(TENANT_ID, INVESTIGATION_ID))
                .singleElement()
                .satisfies(snapshot -> assertThat(snapshot.requestedBy()).isNull());
        assertThat(knowledgeTimeline.findTimelineSnapshots(TENANT_ID, INVESTIGATION_ID))
                .singleElement()
                .satisfies(snapshot -> assertThat(snapshot.requestedBy()).isNull());
        assertThat(reportTimeline.findTimelineSnapshots(TENANT_ID, INVESTIGATION_ID))
                .singleElement()
                .satisfies(snapshot -> {
                    assertThat(snapshot.status()).isEqualTo("AVAILABLE");
                    assertThat(snapshot.requestedBy()).isEqualTo(OPERATOR_ID);
                    assertThat(snapshot.disposition()).isEqualTo("INSUFFICIENT_EVIDENCE");
                });
        assertThat(decisionTimeline.findTimelineSnapshots(TENANT_ID, INVESTIGATION_ID))
                .singleElement()
                .satisfies(snapshot -> assertThat(snapshot.reason()).isEqualTo("The evidence is insufficient."));

        assertThat(incidentTimeline.findTimelineSnapshot(OTHER_TENANT_ID, INVESTIGATION_ID))
                .isEmpty();
        assertThat(evidenceTimeline.findTimelineSnapshots(OTHER_TENANT_ID, INVESTIGATION_ID))
                .isEmpty();
        assertThat(knowledgeTimeline.findTimelineSnapshots(OTHER_TENANT_ID, INVESTIGATION_ID))
                .isEmpty();
        assertThat(reportTimeline.findTimelineSnapshots(OTHER_TENANT_ID, INVESTIGATION_ID))
                .isEmpty();
        assertThat(decisionTimeline.findTimelineSnapshots(OTHER_TENANT_ID, INVESTIGATION_ID))
                .isEmpty();

        mockMvc.perform(get("/api/investigations/{investigationId}/timeline", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].eventType").value("ALERT_RECEIVED"))
                .andExpect(jsonPath("$[1].eventType").value("INVESTIGATION_STARTED"))
                .andExpect(jsonPath("$[2].eventType").value("EVIDENCE_COLLECTION"))
                .andExpect(jsonPath("$[2].actorKind").value("UNATTRIBUTED"))
                .andExpect(jsonPath("$[3].eventType").value("KNOWLEDGE_RETRIEVAL"))
                .andExpect(jsonPath("$[3].actorKind").value("UNATTRIBUTED"))
                .andExpect(jsonPath("$[4].eventType").value("REPORT_GENERATION"))
                .andExpect(jsonPath("$[4].modelId").value("test-report-model"))
                .andExpect(jsonPath("$[4].promptVersion").value("report-prompt/v1"))
                .andExpect(jsonPath("$[5].eventType").value("HUMAN_DECISION"))
                .andExpect(jsonPath("$[5].reason").value("The evidence is insufficient."))
                .andExpect(jsonPath("$[5].resultingIncidentStatus").value("REJECTED"))
                .andExpect(jsonPath("$[0].content").doesNotExist())
                .andExpect(jsonPath("$[0].prompt").doesNotExist())
                .andExpect(jsonPath("$[0].statusDetail").doesNotExist());
        mockMvc.perform(get("/api/investigations/{investigationId}/timeline", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", OTHER_TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void recordsAndReloadsDecisionThroughTenantScopedHttpApi() throws Exception {
        var request = post("/api/investigations/{investigationId}/decisions", INVESTIGATION_ID)
                .header("X-Synthetic-Tenant-Id", TENANT_ID)
                .header("X-Synthetic-Operator-Id", OPERATOR_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outcome\":\"APPROVED\",\"reason\":\"Reviewed through HTTP.\"}");

        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportAttemptId").value(REPORT_ATTEMPT_ID.toString()))
                .andExpect(jsonPath("$.incidentStatus").value("APPROVED"));
        mockMvc.perform(request).andExpect(status().isOk());
        mockMvc.perform(get("/api/investigations/{investigationId}/decisions", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reason").value("Reviewed through HTTP."));
        mockMvc.perform(get("/api/investigations/{investigationId}/decisions", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", OTHER_TENANT_ID))
                .andExpect(status().isNotFound());
    }

    private static HumanDecision decision(DecisionOutcome outcome, String reason) {
        return decisionWithId(DECISION_ID, outcome, reason);
    }

    private static HumanDecision decisionWithId(UUID decisionId, DecisionOutcome outcome, String reason) {
        return new HumanDecision(
                decisionId,
                TENANT_ID,
                INVESTIGATION_ID,
                INCIDENT_ID,
                CORRELATION_ID,
                REPORT_ATTEMPT_ID,
                OPERATOR_ID,
                outcome,
                reason,
                Instant.parse("2026-08-30T12:00:00Z"));
    }

    private Object concurrentRecord(HumanDecision decision, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await();
            return persistence.record(decision);
        } catch (HumanDecisionConflictException exception) {
            return exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private void insertReviewCandidate() {
        jdbcClient
                .sql("""
                        INSERT INTO incident (
                            id, tenant_id, external_alert_id, incident_type, severity,
                            status, summary, description, occurred_at, received_at
                        ) VALUES (
                            :incidentId, :tenantId, 'synthetic-decision-alert',
                            'AUTHORIZATION_DECLINE_RATE_SPIKE', 'HIGH', 'AWAITING_REVIEW',
                            'Authorization declines elevated', 'Synthetic incident.',
                            TIMESTAMPTZ '2026-08-30 11:55:00Z', TIMESTAMPTZ '2026-08-30 11:56:00Z'
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
                            TIMESTAMPTZ '2026-08-30 11:57:00Z', :correlationId
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
                            'synthetic-decision-alert', 'AVAILABLE',
                            TIMESTAMPTZ '2026-08-30 11:58:00Z', TIMESTAMPTZ '2026-08-30 11:58:01Z',
                            TIMESTAMPTZ '2026-08-30 11:58:02Z', 'service-errors/v1',
                            '{"serviceName":"authorization-gateway","observedFrom":"2026-08-30T11:55:00Z","observedTo":"2026-08-30T11:58:00Z","errors":[]}'::jsonb
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
                            'NO_MATCH', TIMESTAMPTZ '2026-08-30 11:59:00Z',
                            TIMESTAMPTZ '2026-08-30 11:59:02Z', 'synthetic query',
                            'knowledge-query/v1', ARRAY[:evidenceId]::uuid[],
                            'nomic-embed-text', 768, '{}'::jsonb,
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
        jdbcClient
                .sql("""
                        INSERT INTO report_generation_attempt (
                            id, tenant_id, investigation_id, incident_id,
                            investigation_correlation_id, requested_by, status,
                            requested_at, completed_at, model_id, temperature,
                            max_output_tokens, prompt_version, prompt_hash,
                            schema_version, schema_hash, latest_evidence_id,
                            applicable_evidence_id, retrieval_id, disposition,
                            report_content
                        ) VALUES (
                            :reportId, :tenantId, :investigationId, :incidentId,
                            :correlationId, :operatorId, 'AVAILABLE',
                            TIMESTAMPTZ '2026-08-30 11:59:10Z',
                            TIMESTAMPTZ '2026-08-30 11:59:20Z', 'test-report-model', 0,
                            4096, 'report-prompt/v1', :promptHash,
                            'report-v1', :schemaHash, :evidenceId,
                            :evidenceId, :retrievalId, 'INSUFFICIENT_EVIDENCE',
                            '{}'::jsonb
                        )
                        """)
                .param("reportId", REPORT_ATTEMPT_ID)
                .param("tenantId", TENANT_ID)
                .param("investigationId", INVESTIGATION_ID)
                .param("incidentId", INCIDENT_ID)
                .param("correlationId", CORRELATION_ID)
                .param("operatorId", OPERATOR_ID)
                .param("promptHash", "a".repeat(64))
                .param("schemaHash", "b".repeat(64))
                .param("evidenceId", EVIDENCE_ID)
                .param("retrievalId", RETRIEVAL_ID)
                .update();
    }
}
