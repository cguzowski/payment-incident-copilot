package com.cguzowski.paymentcopilot.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbedding;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingClient;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeIngestionService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ReportApiPostgresIntegrationTest {

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
    private WebApplicationContext applicationContext;

    @Autowired
    private JdbcClient jdbcClient;

    @MockitoBean
    private ReportModel model;

    @MockitoBean
    private KnowledgeEmbeddingClient embeddingClient;

    @Autowired
    private KnowledgeIngestionService ingestion;

    @Autowired
    private ReportContextAssembler contexts;

    @Autowired
    private ReportOutputParser parser;

    @Autowired
    private ReportGenerationPersistenceService persistence;

    private MockMvc mockMvc;

    @Test
    void persistsAvailableHighForHumanReview() throws Exception {
        ReportGenerationContext context = prepareKnowledge();
        ReportDocument medium = proposedReport(context, ReportConfidenceLevel.MEDIUM);
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        assertThat(parser.parse(mapper.writeValueAsString(medium), context)).isEqualTo(medium);
        String high = mapper.writeValueAsString(proposedReport(context, ReportConfidenceLevel.HIGH));
        when(model.generate(any(), any())).thenReturn(new ReportModelResponse(high, "high-request"));

        mockMvc.perform(post("/api/investigations/{investigationId}/reports", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .header("X-Synthetic-Operator-Id", OPERATOR_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.report.confidence.level").value("HIGH"))
                .andExpect(jsonPath("$.promptVersion").value("report-prompt/v8"));
        mockMvc.perform(get("/api/investigations/{investigationId}/reports", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].report.confidence.level").value("HIGH"));
        assertThat(jdbcClient
                        .sql("SELECT status FROM incident WHERE id = :id")
                        .param("id", INCIDENT_ID)
                        .query(String.class)
                        .single())
                .isEqualTo("AWAITING_REVIEW");
        assertThat(jdbcClient
                        .sql("SELECT COUNT(*) FROM report_claim")
                        .query(Integer.class)
                        .single())
                .isPositive();
        verify(model, times(1)).generate(any(), any());
    }

    @Test
    void readsHistoricalHighConfidenceWithoutRevalidating() throws Exception {
        ReportGenerationContext context = prepareKnowledge();
        var historical = ReportGenerationAttempt.started(
                UUID.randomUUID(),
                OPERATOR_ID,
                Instant.parse("2026-10-01T10:00:00Z"),
                context,
                "historical-model",
                new ReportPrompt("historical", "report-prompt/v5", "a".repeat(64), "report-v1", "b".repeat(64)));
        assertThat(persistence.start(historical)).isTrue();
        assertThat(persistence.completeAvailable(historical.completeAvailable(
                        Instant.parse("2026-10-01T10:00:02Z"),
                        new ReportModelResponse("historical", "historical-request"),
                        proposedReport(context, ReportConfidenceLevel.HIGH))))
                .isTrue();
        mockMvc.perform(get("/api/investigations/{investigationId}/reports", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].report.confidence.level").value("HIGH"))
                .andExpect(jsonPath("$[0].promptVersion").value("report-prompt/v5"));
        verifyNoInteractions(model);
    }

    private ReportGenerationContext prepareKnowledge() throws Exception {
        float[] vector = new float[KnowledgeEmbeddingClient.DIMENSIONS];
        vector[0] = 1;
        when(embeddingClient.embed(anyString()))
                .thenReturn(new KnowledgeEmbedding(
                        KnowledgeEmbeddingClient.MODEL_ID, KnowledgeEmbeddingClient.DIMENSIONS, true, vector));
        ingestion.importApprovedSources();
        mockMvc.perform(post("/api/investigations/{investigationId}/knowledge-retrievals", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .header("X-Synthetic-Operator-Id", OPERATOR_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
        return contexts.find(TENANT_ID, INVESTIGATION_ID).orElseThrow();
    }

    private static ReportDocument proposedReport(ReportGenerationContext context, ReportConfidenceLevel level) {
        var evidence = new ReportClaim("Gateway timeouts observed.", List.of(EVIDENCE_ID), List.of());
        var advice = new ReportClaim(
                "Inspect gateway telemetry.",
                List.of(EVIDENCE_ID),
                List.of(context.knowledge().chunks().getFirst().chunkId()));
        return new ReportDocument(
                ReportDisposition.PROPOSED,
                evidence,
                List.of(evidence),
                List.of(),
                advice,
                new ReportConfidence(level, "Aggregate evidence lacks independent confirmation.", List.of(EVIDENCE_ID)),
                advice,
                List.of(),
                List.of(new ReportGap("Independent confirmation is missing.")));
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
        reset(model);
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
        insertInvestigation();
        insertTerminalPrerequisites();
        when(model.modelId()).thenReturn("global.amazon.nova-2-lite-v1:0");
    }

    @Test
    void createsAndReturnsReportHistoryWithCommittedStartedBeforeModelCall() throws Exception {
        AtomicBoolean observedStartedOutsideTransaction = new AtomicBoolean();
        when(model.generate(any(), any())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(0);
            assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                    .isFalse();
            assertThat(prompt).contains(EVIDENCE_ID.toString(), RETRIEVAL_ID.toString(), "report-v1");
            assertThat(jdbcClient
                            .sql("SELECT status FROM report_generation_attempt")
                            .query(String.class)
                            .single())
                    .isEqualTo("STARTED");
            observedStartedOutsideTransaction.set(true);
            return new ReportModelResponse(validInsufficientReportJson(), "provider-request-1");
        });

        mockMvc.perform(post("/api/investigations/{investigationId}/reports", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .header("X-Synthetic-Operator-Id", OPERATOR_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.report.disposition").value("INSUFFICIENT_EVIDENCE"))
                .andExpect(jsonPath("$.report.probableCause").isEmpty())
                .andExpect(jsonPath("$.report.recommendation").isEmpty());

        assertThat(observedStartedOutsideTransaction).isTrue();
        assertThat(jdbcClient
                        .sql("SELECT status FROM incident WHERE id = :incidentId")
                        .param("incidentId", INCIDENT_ID)
                        .query(String.class)
                        .single())
                .isEqualTo("AWAITING_REVIEW");
        mockMvc.perform(get("/api/investigations/{investigationId}/reports", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void crossTenantAndNeverAttemptedPrerequisiteShortCircuitBeforeModelOrAttempt() throws Exception {
        mockMvc.perform(post("/api/investigations/{investigationId}/reports", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", OTHER_TENANT_ID)
                        .header("X-Synthetic-Operator-Id", OPERATOR_ID))
                .andExpect(status().isNotFound());
        jdbcClient.sql("DELETE FROM knowledge_retrieval_attempt").update();
        jdbcClient.sql("DELETE FROM evidence_collection_attempt").update();
        mockMvc.perform(post("/api/investigations/{investigationId}/reports", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .header("X-Synthetic-Operator-Id", OPERATOR_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:problem:report-generation-conflict"));

        verifyNoInteractions(model);
        assertThat(jdbcClient
                        .sql("SELECT COUNT(*) FROM report_generation_attempt")
                        .query(Integer.class)
                        .single())
                .isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PARTIAL", "UNAVAILABLE"})
    void persistsCompliantDegradedReportAndRequiresHumanReview(String evidenceStatus) throws Exception {
        degradeEvidence(evidenceStatus);
        when(model.generate(any(), any()))
                .thenReturn(new ReportModelResponse(validInsufficientReportJson(), "degraded-request"));

        mockMvc.perform(post("/api/investigations/{investigationId}/reports", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .header("X-Synthetic-Operator-Id", OPERATOR_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.report.disposition").value("INSUFFICIENT_EVIDENCE"))
                .andExpect(jsonPath("$.report.confidence.level").value("LOW"))
                .andExpect(jsonPath("$.latestEvidenceId").value(EVIDENCE_ID.toString()))
                .andExpect(jsonPath("$.report.probableCause").isEmpty())
                .andExpect(jsonPath("$.report.recommendation").isEmpty());
        assertThat(jdbcClient
                        .sql("SELECT status FROM incident WHERE id = :id")
                        .param("id", INCIDENT_ID)
                        .query(String.class)
                        .single())
                .isEqualTo("AWAITING_REVIEW");
        assertThat(jdbcClient
                        .sql("SELECT prompt_version FROM report_generation_attempt")
                        .query(String.class)
                        .single())
                .isEqualTo("report-prompt/v8");
        verify(model, times(1)).generate(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PARTIAL", "UNAVAILABLE"})
    void rejectsMissingDegradedEvidenceGapWithoutTransitionOrAutomaticRetry(String evidenceStatus) throws Exception {
        degradeEvidence(evidenceStatus);
        String invalid =
                validInsufficientReportJson().replace("[{\"description\":\"No approved knowledge matched.\"}]", "[]");
        when(model.generate(any(), any())).thenReturn(new ReportModelResponse(invalid, "invalid-request"));

        mockMvc.perform(post("/api/investigations/{investigationId}/reports", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .header("X-Synthetic-Operator-Id", OPERATOR_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("MALFORMED"))
                .andExpect(jsonPath("$.report").isEmpty());
        mockMvc.perform(get("/api/investigations/{investigationId}/reports", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("MALFORMED"));
        assertThat(jdbcClient
                        .sql("SELECT status FROM incident WHERE id = :id")
                        .param("id", INCIDENT_ID)
                        .query(String.class)
                        .single())
                .isEqualTo("INVESTIGATING");
        assertThat(jdbcClient
                        .sql("SELECT COUNT(*) FROM report_claim")
                        .query(Integer.class)
                        .single())
                .isZero();
        verify(model, times(1)).generate(any(), any());
    }

    private void degradeEvidence(String status) {
        jdbcClient
                .sql(
                        "UPDATE evidence_collection_attempt SET status = :status, content = CASE WHEN :status = 'PARTIAL' THEN content ELSE NULL END WHERE id = :id")
                .param("status", status)
                .param("id", EVIDENCE_ID)
                .update();
    }

    private void insertInvestigation() {
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
    }

    private void insertTerminalPrerequisites() {
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
                            CAST(:content AS JSONB)
                        )
                        """)
                .param("evidenceId", EVIDENCE_ID)
                .param("tenantId", TENANT_ID)
                .param("investigationId", INVESTIGATION_ID)
                .param("toolCallId", UUID.randomUUID())
                .param("correlationId", CORRELATION_ID)
                .param("content", """
                        {"serviceName":"authorization-gateway","observedFrom":"2026-08-29T09:55:00Z","observedTo":"2026-08-29T09:58:00Z","errors":[{"sourceEventId":"evt-1","observedAt":"2026-08-29T09:57:00Z","errorCode":"GATEWAY_TIMEOUT","count":12}]}""")
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
                            'knowledge-query/v1', CAST(:evidenceIds AS UUID[]),
                            'amazon.titan-embed-text-v2:0', 1024, '{}'::jsonb,
                            'postgres-hybrid-rrf/v1', 60, 20, 0, 0.55,
                            'No approved knowledge matched.'
                        )
                        """)
                .param("retrievalId", RETRIEVAL_ID)
                .param("tenantId", TENANT_ID)
                .param("investigationId", INVESTIGATION_ID)
                .param("correlationId", CORRELATION_ID)
                .param("evidenceIds", "{" + EVIDENCE_ID + "}")
                .update();
    }

    private static String validInsufficientReportJson() {
        return """
                {
                  "disposition":"INSUFFICIENT_EVIDENCE",
                  "summary":{"statement":"Available evidence is insufficient.","evidenceIds":["%s"],"knowledgeChunkIds":[]},
                  "observations":[{"statement":"Gateway timeouts were observed.","evidenceIds":["%s"],"knowledgeChunkIds":[]}],
                  "inferences":[],
                  "probableCause":null,
                  "confidence":{"level":"LOW","rationale":"Approved guidance is unavailable.","evidenceIds":["%s"]},
                  "recommendation":null,
                  "contradictions":[],
                  "evidenceGaps":[{"description":"No approved knowledge matched."}]
                }
                """.formatted(EVIDENCE_ID, EVIDENCE_ID, EVIDENCE_ID);
    }
}
