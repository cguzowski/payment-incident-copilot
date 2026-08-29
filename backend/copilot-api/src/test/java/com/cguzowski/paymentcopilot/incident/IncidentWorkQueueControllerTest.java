package com.cguzowski.paymentcopilot.incident;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextExceptionHandler;
import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextResolver;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IncidentWorkQueueControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INCIDENT_ID = UUID.fromString("f4749ecb-49b0-4277-a140-cb69485b082f");
    private static final UUID INVESTIGATION_ID = UUID.fromString("a012c9cb-85a6-4d77-9703-3b53377b56c3");

    private IncidentWorkQueueService incidentWorkQueueService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        incidentWorkQueueService = mock(IncidentWorkQueueService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new IncidentWorkQueueController(
                        incidentWorkQueueService, new SyntheticRequestContextResolver()))
                .setControllerAdvice(new ApiExceptionHandler(), new SyntheticRequestContextExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    @Test
    void returnsAllActiveIncidentsForTenantWithoutAgeCutoff() throws Exception {
        when(incidentWorkQueueService.getQueue(TENANT_ID)).thenReturn(List.of(queueItem()));

        mockMvc.perform(get("/api/incidents").header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].incidentId").value(INCIDENT_ID.toString()))
                .andExpect(jsonPath("$[0].status").value("INVESTIGATING"))
                .andExpect(jsonPath("$[0].activeInvestigationId").value(INVESTIGATION_ID.toString()))
                .andExpect(jsonPath("$[0].tenantId").doesNotExist())
                .andExpect(jsonPath("$[0].description").doesNotExist())
                .andExpect(jsonPath("$[0].*", hasSize(9)));
    }

    @Test
    void rejectsMissingAndMalformedTenantContext() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-synthetic-request-context"))
                .andExpect(jsonPath("$.errors[0].field").value("tenantId"));

        mockMvc.perform(get("/api/incidents").header("X-Synthetic-Tenant-Id", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-synthetic-request-context"))
                .andExpect(jsonPath("$.errors[0].field").value("tenantId"));
    }

    private static IncidentWorkQueueItem queueItem() {
        return new IncidentWorkQueueItem(
                INCIDENT_ID,
                "alert-auth-decline-001",
                IncidentType.AUTHORIZATION_DECLINE_RATE_SPIKE,
                IncidentSeverity.HIGH,
                IncidentStatus.INVESTIGATING,
                "Authorization decline rate above threshold",
                Instant.parse("2026-08-20T07:14:00Z"),
                Instant.parse("2026-08-20T07:15:00Z"),
                INVESTIGATION_ID);
    }
}
