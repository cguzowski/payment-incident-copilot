package com.cguzowski.paymentcopilot.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

    @BeforeEach
    void clearIncidents() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
        jdbcClient.sql("DELETE FROM incident").update();
    }

    @Test
    void validAlertIsPersistedWithNewStatus() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAlertJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"));

        Map<String, Object> incident = jdbcClient.sql("""
                        SELECT tenant_id, external_alert_id, incident_type, severity, status,
                               summary, description
                        FROM incident
                        """)
                .query()
                .singleRow();
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAlertJson()))
                .andExpect(status().isCreated());
        UUID incidentId = jdbcClient.sql("SELECT id FROM incident")
                .query(UUID.class)
                .single();

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAlertJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(incidentId.toString()));

        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM incident")
                .query(Integer.class)
                .single();
        assertThat(count).isOne();
    }

    @Test
    void invalidAlertReturnsStructuredBadRequestWithoutPersistence() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-alert"))
                .andExpect(jsonPath("$.errors.length()").value(6));

        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM incident")
                .query(Integer.class)
                .single();
        assertThat(count).isZero();
    }

    @Test
    void emptyTenantQueueReturnsAnEmptyList() throws Exception {
        mockMvc.perform(get("/api/tenants/{tenantId}/alert-queue", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void findsIncidentDetailByTenantAndIncidentId() throws Exception {
        UUID incidentId = createIncident();

        Optional<Incident> incident = incidentRepository.findByTenantIdAndIncidentId(TENANT_ID, incidentId);

        assertThat(incident).isPresent();
        assertThat(incident.orElseThrow().description())
                .isEqualTo("Synthetic authorization declines exceeded 25% for five minutes.");
        mockMvc.perform(get("/api/incidents/{incidentId}", incidentId)
                        .queryParam("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(incidentId.toString()))
                .andExpect(jsonPath("$.description")
                        .value("Synthetic authorization declines exceeded 25% for five minutes."));
    }

    @Test
    void doesNotFindIncidentForDifferentTenant() throws Exception {
        UUID incidentId = createIncident();

        assertThat(incidentRepository.findByTenantIdAndIncidentId(OTHER_TENANT_ID, incidentId)).isEmpty();
        mockMvc.perform(get("/api/incidents/{incidentId}", incidentId)
                        .queryParam("tenantId", OTHER_TENANT_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem:incident-not-found"))
                .andExpect(jsonPath("$.detail")
                        .value("No incident was found for the requested tenant and incident ID."));
    }

    @Test
    void detailLookupDoesNotModifyPersistedIncident() throws Exception {
        UUID incidentId = createIncident();
        Map<String, Object> before = persistedIncident(incidentId);

        for (int request = 0; request < 2; request++) {
            mockMvc.perform(get("/api/incidents/{incidentId}", incidentId)
                            .queryParam("tenantId", TENANT_ID.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("NEW"));
        }

        assertThat(persistedIncident(incidentId)).isEqualTo(before);
        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM incident")
                .query(Integer.class)
                .single();
        assertThat(count).isOne();
    }

    private UUID createIncident() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAlertJson()))
                .andExpect(status().isCreated());
        return jdbcClient.sql("SELECT id FROM incident")
                .query(UUID.class)
                .single();
    }

    private Map<String, Object> persistedIncident(UUID incidentId) {
        return jdbcClient.sql("""
                        SELECT id, tenant_id, external_alert_id, incident_type, severity, status,
                               summary, description, occurred_at, received_at
                        FROM incident
                        WHERE id = :incidentId
                        """)
                .param("incidentId", incidentId)
                .query()
                .singleRow();
    }

    private static String validAlertJson() {
        return """
                {
                  "tenantId": "8b860d80-d17f-4e6b-8c48-af35f26a4d61",
                  "externalAlertId": "alert-auth-decline-001",
                  "severity": "CRITICAL",
                  "detectedAt": "2026-08-22T07:14:00Z",
                  "title": "Authorization decline rate above threshold",
                  "description": "Synthetic authorization declines exceeded 25% for five minutes."
                }
                """;
    }
}
