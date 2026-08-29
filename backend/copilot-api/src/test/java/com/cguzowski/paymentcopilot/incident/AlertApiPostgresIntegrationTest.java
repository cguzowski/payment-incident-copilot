package com.cguzowski.paymentcopilot.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
class AlertApiPostgresIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("076a18a3-d54f-486a-b3ec-189e1048fd28");

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

    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private InvestigationService investigationService;

    @BeforeEach
    void clearIncidents() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
        jdbcClient.sql("DELETE FROM investigation").update();
        jdbcClient.sql("DELETE FROM incident").update();
    }

    @Test
    void validAlertIsPersistedWithNewStatus() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAlertJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"));

        Map<String, Object> incident = jdbcClient.sql("""
                        SELECT tenant_id, external_alert_id, incident_type, severity, status,
                               summary, description
                        FROM incident
                        """).query().singleRow();
        assertThat(incident)
                .containsEntry("tenant_id", TENANT_ID)
                .containsEntry("external_alert_id", "alert-auth-decline-001")
                .containsEntry("incident_type", "AUTHORIZATION_DECLINE_RATE_SPIKE")
                .containsEntry("severity", "CRITICAL")
                .containsEntry("status", "NEW")
                .containsEntry("summary", "Authorization decline rate above threshold")
                .containsEntry("description", "Synthetic authorization declines exceeded 25% for five minutes.");
    }

    @Test
    void repeatedTenantAndExternalAlertIdDoesNotCreateADuplicate() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAlertJson()))
                .andExpect(status().isCreated());
        UUID incidentId =
                jdbcClient.sql("SELECT id FROM incident").query(UUID.class).single();

        mockMvc.perform(post("/api/alerts")
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAlertJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(incidentId.toString()));

        Integer count = jdbcClient
                .sql("SELECT COUNT(*) FROM incident")
                .query(Integer.class)
                .single();
        assertThat(count).isOne();
    }

    @Test
    void invalidAlertReturnsStructuredBadRequestWithoutPersistence() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-alert"))
                .andExpect(jsonPath("$.errors.length()").value(5));

        Integer count = jdbcClient
                .sql("SELECT COUNT(*) FROM incident")
                .query(Integer.class)
                .single();
        assertThat(count).isZero();
    }

    @Test
    void emptyTenantQueueReturnsAnEmptyList() throws Exception {
        mockMvc.perform(get("/api/incidents").header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void workQueueRetainsNewAndAwaitingReviewIncidentsWithoutAgeCutoff() throws Exception {
        UUID oldNewIncidentId = UUID.fromString("057ced7b-1a45-4695-ae0e-f2ad9fc1bd73");
        UUID newerAwaitingReviewIncidentId = UUID.fromString("724547d4-76d7-45d3-a6a5-afdf2096229b");
        UUID investigationId = UUID.fromString("a012c9cb-85a6-4d77-9703-3b53377b56c3");
        insertIncident(oldNewIncidentId, TENANT_ID, "alert-old-new", "NEW", "2026-07-01T07:15:00Z");
        insertIncident(
                newerAwaitingReviewIncidentId,
                TENANT_ID,
                "alert-newer-awaiting-review",
                "AWAITING_REVIEW",
                "2026-08-20T07:15:00Z");
        insertInvestigation(investigationId, TENANT_ID, newerAwaitingReviewIncidentId);

        mockMvc.perform(get("/api/incidents").header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].incidentId").value(newerAwaitingReviewIncidentId.toString()))
                .andExpect(jsonPath("$[0].status").value("AWAITING_REVIEW"))
                .andExpect(jsonPath("$[0].activeInvestigationId").value(investigationId.toString()))
                .andExpect(jsonPath("$[1].incidentId").value(oldNewIncidentId.toString()))
                .andExpect(jsonPath("$[1].status").value("NEW"))
                .andExpect(jsonPath("$[1].activeInvestigationId").isEmpty());
    }

    @Test
    void workQueueExcludesOtherTenantIncidents() throws Exception {
        insertIncident(
                UUID.fromString("057ced7b-1a45-4695-ae0e-f2ad9fc1bd73"),
                TENANT_ID,
                "alert-owning-tenant",
                "NEW",
                "2026-08-20T07:15:00Z");
        insertIncident(
                UUID.fromString("724547d4-76d7-45d3-a6a5-afdf2096229b"),
                OTHER_TENANT_ID,
                "alert-other-tenant",
                "NEW",
                "2026-08-21T07:15:00Z");

        mockMvc.perform(get("/api/incidents").header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].externalAlertId").value("alert-owning-tenant"));
    }

    @Test
    void retiredAlertQueueEndpointIsNotAvailable() throws Exception {
        mockMvc.perform(get("/api/tenants/{tenantId}/alert-queue", TENANT_ID)).andExpect(status().isNotFound());
    }

    @Test
    void persistsInvestigationAndIncidentTransitionAtomically() throws Exception {
        UUID incidentId = createIncident();

        mockMvc.perform(post("/api/incidents/{incidentId}/investigations", incidentId)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString())
                        .header("X-Synthetic-Operator-Id", "7b636625-53d1-46f7-92a9-9c8c27a243d1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentId").value(incidentId.toString()))
                .andExpect(jsonPath("$.incidentStatus").value("INVESTIGATING"));

        Map<String, Object> persisted =
                jdbcClient.sql("""
                        SELECT investigation.id AS investigation_id, investigation.tenant_id,
                               investigation.started_by, investigation.started_at,
                               investigation.correlation_id, incident.status
                        FROM investigation
                        JOIN incident ON incident.tenant_id = investigation.tenant_id
                                     AND incident.id = investigation.incident_id
                        WHERE investigation.incident_id = :incidentId
                        """).param("incidentId", incidentId).query().singleRow();
        assertThat(persisted)
                .containsEntry("tenant_id", TENANT_ID)
                .containsEntry("started_by", UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"))
                .containsEntry("status", "INVESTIGATING");
        assertThat(persisted.get("correlation_id")).isNotNull();

        mockMvc.perform(get("/api/investigations/{investigationId}", persisted.get("investigation_id"))
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(incidentId.toString()))
                .andExpect(jsonPath("$.incidentStatus").value("INVESTIGATING"));
    }

    @Test
    void repeatedStartPreservesOriginalInvestigationMetadata() throws Exception {
        UUID incidentId = createIncident();
        mockMvc.perform(post("/api/incidents/{incidentId}/investigations", incidentId)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString())
                        .header("X-Synthetic-Operator-Id", "7b636625-53d1-46f7-92a9-9c8c27a243d1"))
                .andExpect(status().isCreated());
        Map<String, Object> before = persistedInvestigation(incidentId);

        mockMvc.perform(post("/api/incidents/{incidentId}/investigations", incidentId)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString())
                        .header("X-Synthetic-Operator-Id", "5481b654-1834-4f9b-b8c7-90dbf007e906"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.investigationId").value(before.get("id").toString()))
                .andExpect(
                        jsonPath("$.startedBy").value(before.get("started_by").toString()));

        assertThat(persistedInvestigation(incidentId)).isEqualTo(before);
        assertThat(investigationCount()).isOne();
    }

    @Test
    void concurrentStartsReturnOneInvestigation() throws Exception {
        UUID incidentId = UUID.fromString("057ced7b-1a45-4695-ae0e-f2ad9fc1bd73");
        insertIncident(incidentId, TENANT_ID, "alert-concurrent", "NEW", "2026-08-20T07:15:00Z");
        UUID operatorId = UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<InvestigationStartResult> request = () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return investigationService.start(TENANT_ID, incidentId, operatorId);
        };
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<InvestigationStartResult> first = executor.submit(request);
            Future<InvestigationStartResult> second = executor.submit(request);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            InvestigationStartResult firstResult = first.get(10, TimeUnit.SECONDS);
            InvestigationStartResult secondResult = second.get(10, TimeUnit.SECONDS);
            assertThat(firstResult.response().investigationId())
                    .isEqualTo(secondResult.response().investigationId());
            assertThat(List.of(firstResult.created(), secondResult.created())).containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }

        assertThat(investigationCount()).isOne();
        assertThat(incidentStatus(incidentId)).isEqualTo("INVESTIGATING");
    }

    @Test
    void crossTenantStartAndWorkspaceAreIndistinguishableFromNotFound() throws Exception {
        UUID incidentId = createIncident();

        mockMvc.perform(post("/api/incidents/{incidentId}/investigations", incidentId)
                        .header("X-Synthetic-Tenant-Id", OTHER_TENANT_ID.toString())
                        .header("X-Synthetic-Operator-Id", "7b636625-53d1-46f7-92a9-9c8c27a243d1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem:incident-not-found"));
        assertThat(investigationCount()).isZero();
        assertThat(incidentStatus(incidentId)).isEqualTo("NEW");

        mockMvc.perform(get("/api/investigations/{investigationId}", UUID.randomUUID())
                        .header("X-Synthetic-Tenant-Id", OTHER_TENANT_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem:investigation-not-found"));
    }

    @Test
    void preventsCrossTenantInvestigationAssociation() {
        UUID incidentId = UUID.fromString("057ced7b-1a45-4695-ae0e-f2ad9fc1bd73");
        insertIncident(incidentId, TENANT_ID, "alert-tenant-integrity", "NEW", "2026-08-20T07:15:00Z");

        assertThatThrownBy(() -> insertInvestigation(UUID.randomUUID(), OTHER_TENANT_ID, incidentId))
                .isInstanceOf(RuntimeException.class);
        assertThat(investigationCount()).isZero();
    }

    @Test
    void rollsBackInvestigationWhenIncidentTransitionFails() {
        UUID incidentId = UUID.fromString("057ced7b-1a45-4695-ae0e-f2ad9fc1bd73");
        insertIncident(incidentId, TENANT_ID, "alert-rollback", "NEW", "2026-08-20T07:15:00Z");
        jdbcClient.sql("""
                        CREATE FUNCTION reject_investigating_transition()
                        RETURNS TRIGGER AS $$
                        BEGIN
                          IF NEW.status = 'INVESTIGATING' THEN
                            RAISE EXCEPTION 'synthetic transition failure';
                          END IF;
                          RETURN NEW;
                        END;
                        $$ LANGUAGE plpgsql
                        """).update();
        jdbcClient.sql("""
                        CREATE TRIGGER reject_investigating_transition
                        BEFORE UPDATE ON incident
                        FOR EACH ROW EXECUTE FUNCTION reject_investigating_transition()
                        """).update();
        try {
            assertThatThrownBy(() -> investigationService.start(
                            TENANT_ID, incidentId, UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1")))
                    .isInstanceOf(RuntimeException.class);
        } finally {
            jdbcClient
                    .sql("DROP TRIGGER reject_investigating_transition ON incident")
                    .update();
            jdbcClient.sql("DROP FUNCTION reject_investigating_transition()").update();
        }

        assertThat(investigationCount()).isZero();
        assertThat(incidentStatus(incidentId)).isEqualTo("NEW");
    }

    @Test
    void findsIncidentDetailByTenantAndIncidentId() throws Exception {
        UUID incidentId = createIncident();

        Optional<Incident> incident = incidentRepository.findByTenantIdAndIncidentId(TENANT_ID, incidentId);

        assertThat(incident).isPresent();
        assertThat(incident.orElseThrow().description())
                .isEqualTo("Synthetic authorization declines exceeded 25% for five minutes.");
        mockMvc.perform(get("/api/incidents/{incidentId}", incidentId)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(incidentId.toString()))
                .andExpect(jsonPath("$.description")
                        .value("Synthetic authorization declines exceeded 25% for five minutes."));
    }

    @Test
    void doesNotFindIncidentForDifferentTenant() throws Exception {
        UUID incidentId = createIncident();

        assertThat(incidentRepository.findByTenantIdAndIncidentId(OTHER_TENANT_ID, incidentId))
                .isEmpty();
        mockMvc.perform(get("/api/incidents/{incidentId}", incidentId)
                        .header("X-Synthetic-Tenant-Id", OTHER_TENANT_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem:incident-not-found"))
                .andExpect(
                        jsonPath("$.detail").value("No incident was found for the requested tenant and incident ID."));
    }

    @Test
    void detailLookupDoesNotModifyPersistedIncident() throws Exception {
        UUID incidentId = createIncident();
        Map<String, Object> before = persistedIncident(incidentId);

        for (int request = 0; request < 2; request++) {
            mockMvc.perform(get("/api/incidents/{incidentId}", incidentId)
                            .header("X-Synthetic-Tenant-Id", TENANT_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("NEW"));
        }

        assertThat(persistedIncident(incidentId)).isEqualTo(before);
        Integer count = jdbcClient
                .sql("SELECT COUNT(*) FROM incident")
                .query(Integer.class)
                .single();
        assertThat(count).isOne();
    }

    private UUID createIncident() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAlertJson()))
                .andExpect(status().isCreated());
        return jdbcClient.sql("SELECT id FROM incident").query(UUID.class).single();
    }

    private Map<String, Object> persistedIncident(UUID incidentId) {
        return jdbcClient.sql("""
                        SELECT id, tenant_id, external_alert_id, incident_type, severity, status,
                               summary, description, occurred_at, received_at
                        FROM incident
                        WHERE id = :incidentId
                        """).param("incidentId", incidentId).query().singleRow();
    }

    private Map<String, Object> persistedInvestigation(UUID incidentId) {
        return jdbcClient.sql("""
                        SELECT id, tenant_id, incident_id, started_by, started_at, correlation_id
                        FROM investigation
                        WHERE incident_id = :incidentId
                        """).param("incidentId", incidentId).query().singleRow();
    }

    private int investigationCount() {
        return jdbcClient
                .sql("SELECT COUNT(*) FROM investigation")
                .query(Integer.class)
                .single();
    }

    private String incidentStatus(UUID incidentId) {
        return jdbcClient
                .sql("SELECT status FROM incident WHERE id = :incidentId")
                .param("incidentId", incidentId)
                .query(String.class)
                .single();
    }

    private void insertIncident(
            UUID incidentId, UUID tenantId, String externalAlertId, String status, String receivedAt) {
        jdbcClient
                .sql("""
                        INSERT INTO incident (
                            id, tenant_id, external_alert_id, incident_type, severity, status,
                            summary, description, occurred_at, received_at
                        ) VALUES (
                            :id, :tenantId, :externalAlertId,
                            'AUTHORIZATION_DECLINE_RATE_SPIKE', 'HIGH', :status,
                            'Authorization decline rate above threshold',
                            'Synthetic authorization decline incident.',
                            CAST(:receivedAt AS TIMESTAMPTZ) - INTERVAL '1 minute',
                            CAST(:receivedAt AS TIMESTAMPTZ)
                        )
                        """)
                .param("id", incidentId)
                .param("tenantId", tenantId)
                .param("externalAlertId", externalAlertId)
                .param("status", status)
                .param("receivedAt", receivedAt)
                .update();
    }

    private void insertInvestigation(UUID investigationId, UUID tenantId, UUID incidentId) {
        jdbcClient
                .sql("""
                        INSERT INTO investigation (
                            id, tenant_id, incident_id, started_by, started_at, correlation_id
                        ) VALUES (
                            :id, :tenantId, :incidentId, :startedBy,
                            TIMESTAMPTZ '2026-08-20 07:16:00Z', :correlationId
                        )
                        """)
                .param("id", investigationId)
                .param("tenantId", tenantId)
                .param("incidentId", incidentId)
                .param("startedBy", UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"))
                .param("correlationId", UUID.fromString("a5d978b5-34c7-42da-9076-22f8e5169315"))
                .update();
    }

    private static String validAlertJson() {
        return """
                {
                  "externalAlertId": "alert-auth-decline-001",
                  "severity": "CRITICAL",
                  "detectedAt": "2026-08-22T07:14:00Z",
                  "title": "Authorization decline rate above threshold",
                  "description": "Synthetic authorization declines exceeded 25% for five minutes."
                }
                """;
    }
}
