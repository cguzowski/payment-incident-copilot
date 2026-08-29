package com.cguzowski.paymentcopilot.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbedding;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingClient;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeImportSummary;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeIngestionService;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class KnowledgeRetrievalApiPostgresIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("076a18a3-d54f-486a-b3ec-189e1048fd28");
    private static final UUID INCIDENT_ID = UUID.fromString("ce22cb8d-10d6-4d6d-9a56-f644ae84573d");
    private static final UUID INVESTIGATION_ID = UUID.fromString("7f50162a-8dc5-45b0-9c88-dc2f77135e0f");
    private static final UUID CORRELATION_ID = UUID.fromString("133767cf-8ec8-487d-a09d-19d5efcece07");

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

    @Autowired
    private KnowledgeIngestionService ingestionService;

    @MockitoBean
    private KnowledgeEmbeddingClient embeddingClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
        reset(embeddingClient);
        jdbcClient.sql("DELETE FROM knowledge_retrieval_result").update();
        jdbcClient.sql("DELETE FROM knowledge_retrieval_attempt").update();
        jdbcClient.sql("DELETE FROM knowledge_chunk").update();
        jdbcClient.sql("DELETE FROM knowledge_document_version").update();
        jdbcClient.sql("DELETE FROM evidence_collection_attempt").update();
        jdbcClient.sql("DELETE FROM investigation").update();
        jdbcClient.sql("DELETE FROM incident").update();
        when(embeddingClient.embed(anyString())).thenReturn(normalizedEmbedding());
        assertThat(ingestionService.importApprovedSources())
                .isEqualTo(new KnowledgeImportSummary(2, 0, persistedChunkCount()));
        assertThat(ingestionService.importApprovedSources()).isEqualTo(new KnowledgeImportSummary(0, 2, 0));
        insertIncidentInvestigationAndEvidence();
        reset(embeddingClient);
    }

    @Test
    void retrievesAndPersistsApprovedKnowledgeWithoutHoldingTransactionAcrossEmbedding() throws Exception {
        AtomicBoolean observedCommittedStartedAttempt = new AtomicBoolean();
        when(embeddingClient.embed(anyString())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                    .isFalse();
            assertThat(invocation.<String>getArgument(0))
                    .contains("Incident type: AUTHORIZATION_DECLINE_RATE_SPIKE", "GATEWAY_TIMEOUT");
            observedCommittedStartedAttempt.set(jdbcClient
                    .sql("""
                            SELECT EXISTS (
                                SELECT 1 FROM knowledge_retrieval_attempt
                                WHERE tenant_id = :tenantId
                                  AND investigation_id = :investigationId
                                  AND status = 'STARTED'
                            )
                            """)
                    .param("tenantId", TENANT_ID)
                    .param("investigationId", INVESTIGATION_ID)
                    .query(Boolean.class)
                    .single());
            return normalizedEmbedding();
        });

        mockMvc.perform(post("/api/investigations/{investigationId}/knowledge-retrievals", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.results.length()").value(7))
                .andExpect(jsonPath("$.results[?(@.documentType == 'RUNBOOK')]").value(hasSize(4)))
                .andExpect(jsonPath("$.results[?(@.documentType == 'POLICY')]").value(hasSize(3)))
                .andExpect(jsonPath("$.results[0].rawContent").isNotEmpty())
                .andExpect(jsonPath("$.results[0].embeddingInput").doesNotExist());

        assertThat(observedCommittedStartedAttempt).isTrue();
        assertThat(jdbcClient
                        .sql("SELECT COUNT(*) FROM knowledge_retrieval_result")
                        .query(Integer.class)
                        .single())
                .isEqualTo(7);
        assertThat(jdbcClient
                        .sql("SELECT status FROM incident WHERE id = :id")
                        .param("id", INCIDENT_ID)
                        .query(String.class)
                        .single())
                .isEqualTo("INVESTIGATING");
    }

    @Test
    void retriesAppendHistoryAndCrossTenantRequestCreatesNothing() throws Exception {
        when(embeddingClient.embed(anyString())).thenReturn(normalizedEmbedding());
        mockMvc.perform(post("/api/investigations/{investigationId}/knowledge-retrievals", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isCreated());
        Thread.sleep(5);
        mockMvc.perform(post("/api/investigations/{investigationId}/knowledge-retrievals", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/investigations/{investigationId}/knowledge-retrievals", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        reset(embeddingClient);
        mockMvc.perform(post("/api/investigations/{investigationId}/knowledge-retrievals", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", OTHER_TENANT_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem:investigation-not-found"));
        assertThat(jdbcClient
                        .sql("SELECT COUNT(*) FROM knowledge_retrieval_attempt")
                        .query(Integer.class)
                        .single())
                .isEqualTo(2);
        verifyNoInteractions(embeddingClient);
    }

    private int persistedChunkCount() {
        return jdbcClient
                .sql("SELECT COUNT(*) FROM knowledge_chunk")
                .query(Integer.class)
                .single();
    }

    private void insertIncidentInvestigationAndEvidence() {
        jdbcClient
                .sql("""
                        INSERT INTO incident (
                            id, tenant_id, external_alert_id, incident_type, severity,
                            status, summary, description, occurred_at, received_at
                        ) VALUES (
                            :id, :tenantId, 'synthetic-alert-knowledge-api-001',
                            'AUTHORIZATION_DECLINE_RATE_SPIKE', 'HIGH', 'INVESTIGATING',
                            'Authorization declines elevated',
                            'Synthetic gateway failures increased authorization declines.',
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
                            'synthetic-alert-knowledge-api-001', 'AVAILABLE',
                            TIMESTAMPTZ '2026-08-28 09:58:00Z',
                            TIMESTAMPTZ '2026-08-28 09:58:01Z',
                            TIMESTAMPTZ '2026-08-28 09:58:02Z', 'service-errors/v1',
                            CAST(:content AS JSONB)
                        )
                        """)
                .param("id", UUID.fromString("d3a12cd9-ef1d-4328-b63d-2ecea6558e2d"))
                .param("tenantId", TENANT_ID)
                .param("investigationId", INVESTIGATION_ID)
                .param("toolCallId", UUID.fromString("7a04902c-dc0a-4477-878b-698caadf37aa"))
                .param("correlationId", CORRELATION_ID)
                .param("content", """
                        {"serviceName":"authorization-gateway","observedFrom":"2026-08-28T09:55:00Z","observedTo":"2026-08-28T09:58:00Z","errors":[{"sourceEventId":"evt-1","observedAt":"2026-08-28T09:57:00Z","errorCode":"GATEWAY_TIMEOUT","count":12},{"sourceEventId":"evt-2","observedAt":"2026-08-28T09:57:30Z","errorCode":"UPSTREAM_CONNECTION_RESET","count":4}]}""")
                .update();
    }

    private static KnowledgeEmbedding normalizedEmbedding() {
        float[] vector = new float[1024];
        vector[0] = 1.0f;
        return new KnowledgeEmbedding(
                KnowledgeEmbeddingClient.MODEL_ID, KnowledgeEmbeddingClient.DIMENSIONS, true, vector);
    }
}
