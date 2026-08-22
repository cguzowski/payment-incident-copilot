package com.cguzowski.paymentcopilot.incident;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AlertQueueControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");

    private AlertQueueService alertQueueService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        alertQueueService = mock(AlertQueueService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AlertQueueController(alertQueueService))
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    @Test
    void queueContainsOnlyFieldsNeededForTriage() throws Exception {
        AlertQueueSummary summary = new AlertQueueSummary(
                UUID.fromString("f4749ecb-49b0-4277-a140-cb69485b082f"),
                "alert-auth-decline-001",
                IncidentType.AUTHORIZATION_DECLINE_RATE_SPIKE,
                IncidentSeverity.CRITICAL,
                IncidentStatus.NEW,
                "Authorization decline rate above threshold",
                Instant.parse("2026-08-22T07:14:00Z"),
                Instant.parse("2026-08-22T07:15:00Z"));
        when(alertQueueService.getQueue(TENANT_ID)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/tenants/{tenantId}/alert-queue", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].incidentId").value("f4749ecb-49b0-4277-a140-cb69485b082f"))
                .andExpect(jsonPath("$[0].externalAlertId").value("alert-auth-decline-001"))
                .andExpect(jsonPath("$[0].incidentType").value("AUTHORIZATION_DECLINE_RATE_SPIKE"))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$[0].status").value("NEW"))
                .andExpect(jsonPath("$[0].title").value("Authorization decline rate above threshold"))
                .andExpect(jsonPath("$[0].detectedAt").value("2026-08-22T07:14:00Z"))
                .andExpect(jsonPath("$[0].receivedAt").value("2026-08-22T07:15:00Z"))
                .andExpect(jsonPath("$[0].tenantId").doesNotExist())
                .andExpect(jsonPath("$[0].description").doesNotExist());
    }

    @Test
    void queueIsEmptyWhenTheTenantHasNoNewIncidents() throws Exception {
        when(alertQueueService.getQueue(TENANT_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/tenants/{tenantId}/alert-queue", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
