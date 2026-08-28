package com.cguzowski.paymentcopilot.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
class EvidenceCollectionApiPostgresIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("076a18a3-d54f-486a-b3ec-189e1048fd28");
    private static final UUID INCIDENT_ID = UUID.fromString("f4749ecb-49b0-4277-a140-cb69485b082f");
    private static final UUID INVESTIGATION_ID = UUID.fromString("a012c9cb-85a6-4d77-9703-3b53377b56c3");
    private static final UUID CORRELATION_ID = UUID.fromString("a5d978b5-34c7-42da-9076-22f8e5169315");

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
    private ServiceErrorEvidenceGateway gateway;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
        reset(gateway);
        jdbcClient.sql("DELETE FROM evidence_collection_attempt").update();
        jdbcClient.sql("DELETE FROM investigation").update();
        jdbcClient.sql("DELETE FROM incident").update();
        insertIncidentAndInvestigation();
    }

    @Test
    void collectsServiceErrorsWithoutHoldingTransactionAcrossMcpCall() throws Exception {
        AtomicBoolean observedCommittedStartedAttempt = new AtomicBoolean();
        when(gateway.collect(any(), any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            UUID toolCallId = invocation.getArgument(1);
            Map<String, Object> started = jdbcClient.sql("""
                            SELECT status, tool_call_id
                            FROM evidence_collection_attempt
                            WHERE tenant_id = :tenantId AND investigation_id = :investigationId
                            """)
                    .param("tenantId", TENANT_ID)
                    .param("investigationId", INVESTIGATION_ID)
                    .query()
                    .singleRow();
            observedCommittedStartedAttempt.set(
                    started.get("status").equals("STARTED")
                            && started.get("tool_call_id").equals(toolCallId));
            return availableResult(toolCallId);
        });

        mockMvc.perform(post("/api/investigations/{investigationId}/evidence-collections", INVESTIGATION_ID)
                        .queryParam("tenantId", TENANT_ID.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.content.errors[0].errorCode").value("UPSTREAM_TIMEOUT"));

        assertThat(observedCommittedStartedAttempt).isTrue();
        assertThat(jdbcClient.sql("SELECT status FROM evidence_collection_attempt")
                        .query(String.class)
                        .single())
                .isEqualTo("AVAILABLE");
        assertThat(jdbcClient.sql("SELECT status FROM incident WHERE id = :id")
                        .param("id", INCIDENT_ID)
                        .query(String.class)
                        .single())
                .isEqualTo("INVESTIGATING");
    }

    @Test
    void retriesAppendAndHistoryReturnsNewestFirst() throws Exception {
        when(gateway.collect(any(), any())).thenAnswer(invocation -> availableResult(invocation.getArgument(1)));

        mockMvc.perform(post("/api/investigations/{investigationId}/evidence-collections", INVESTIGATION_ID)
                        .queryParam("tenantId", TENANT_ID.toString()))
                .andExpect(status().isCreated());
        Thread.sleep(5);
        mockMvc.perform(post("/api/investigations/{investigationId}/evidence-collections", INVESTIGATION_ID)
                        .queryParam("tenantId", TENANT_ID.toString()))
                .andExpect(status().isCreated());

        List<Map<String, Object>> persisted = jdbcClient.sql("""
                        SELECT id, requested_at
                        FROM evidence_collection_attempt
                        ORDER BY requested_at DESC, id DESC
                        """)
                .query()
                .listOfRows();
        assertThat(persisted).hasSize(2);
        mockMvc.perform(get("/api/investigations/{investigationId}/evidence-collections", INVESTIGATION_ID)
                        .queryParam("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].evidenceId").value(persisted.getFirst().get("id").toString()))
                .andExpect(jsonPath("$[1].evidenceId").value(persisted.getLast().get("id").toString()));
        verify(gateway, org.mockito.Mockito.times(2)).collect(any(), any());
    }

    @Test
    void crossTenantInvestigationCreatesNothingAndDoesNotCallMcp() throws Exception {
        mockMvc.perform(post("/api/investigations/{investigationId}/evidence-collections", INVESTIGATION_ID)
                        .queryParam("tenantId", OTHER_TENANT_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem:investigation-not-found"));

        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM evidence_collection_attempt")
                        .query(Integer.class)
                        .single())
                .isZero();
        verifyNoInteractions(gateway);
    }

    private static EvidenceSourceResult availableResult(UUID toolCallId) {
        return new EvidenceSourceResult(
                "synthetic-observability",
                "getRecentServiceErrors",
                Instant.parse("2026-08-28T10:00:01Z"),
                CORRELATION_ID,
                toolCallId,
                EvidenceCollectionStatus.AVAILABLE,
                null,
                "service-errors/v1",
                new ServiceErrorEvidenceContent(
                        "payment-authorization-service",
                        Instant.parse("2026-08-28T09:55:00Z"),
                        Instant.parse("2026-08-28T10:00:00Z"),
                        List.of(new ServiceErrorObservation(
                                "service-error-001",
                                Instant.parse("2026-08-28T09:58:00Z"),
                                "UPSTREAM_TIMEOUT",
                                14))));
    }

    private void insertIncidentAndInvestigation() {
        jdbcClient.sql("""
                        INSERT INTO incident (
                            id, tenant_id, external_alert_id, incident_type, severity, status,
                            summary, description, occurred_at, received_at
                        ) VALUES (
                            :id, :tenantId, 'alert-auth-decline-001',
                            'AUTHORIZATION_DECLINE_RATE_SPIKE', 'HIGH', 'INVESTIGATING',
                            'Authorization decline rate above threshold',
                            'Synthetic authorization decline incident.',
                            TIMESTAMPTZ '2026-08-22 07:14:00Z',
                            TIMESTAMPTZ '2026-08-22 07:15:00Z'
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
                            TIMESTAMPTZ '2026-08-22 07:16:00Z', :correlationId
                        )
                        """)
                .param("id", INVESTIGATION_ID)
                .param("tenantId", TENANT_ID)
                .param("incidentId", INCIDENT_ID)
                .param("startedBy", UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"))
                .param("correlationId", CORRELATION_ID)
                .update();
    }
}
