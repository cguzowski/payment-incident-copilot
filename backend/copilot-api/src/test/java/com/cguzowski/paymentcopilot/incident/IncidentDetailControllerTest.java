package com.cguzowski.paymentcopilot.incident;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IncidentDetailControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INCIDENT_ID = UUID.fromString("f4749ecb-49b0-4277-a140-cb69485b082f");
    private static final UUID INVESTIGATION_ID = UUID.fromString("a012c9cb-85a6-4d77-9703-3b53377b56c3");

    private IncidentDetailService incidentDetailService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        incidentDetailService = mock(IncidentDetailService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new IncidentDetailController(incidentDetailService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    @Test
    void detailResponseContainsOnlyChosenFields() throws Exception {
        when(incidentDetailService.getDetail(TENANT_ID, INCIDENT_ID)).thenReturn(detailResponse());

        mockMvc.perform(get("/api/incidents/{incidentId}", INCIDENT_ID)
                        .queryParam("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(10)))
                .andExpect(jsonPath("$.incidentId").value(INCIDENT_ID.toString()))
                .andExpect(jsonPath("$.externalAlertId").value("alert-auth-decline-001"))
                .andExpect(jsonPath("$.incidentType").value("AUTHORIZATION_DECLINE_RATE_SPIKE"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.title").value("Authorization decline rate above threshold"))
                .andExpect(jsonPath("$.description")
                        .value("Synthetic authorization declines exceeded 25% for five minutes."))
                .andExpect(jsonPath("$.detectedAt").value("2026-08-22T07:14:00Z"))
                .andExpect(jsonPath("$.receivedAt").value("2026-08-22T07:15:00Z"))
                .andExpect(jsonPath("$.activeInvestigationId").value(INVESTIGATION_ID.toString()))
                .andExpect(jsonPath("$.tenantId").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void returnsBadRequestWhenTenantIdIsMissingOrBlank() throws Exception {
        mockMvc.perform(get("/api/incidents/{incidentId}", INCIDENT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-incident-request"))
                .andExpect(jsonPath("$.title").value("Invalid incident request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("tenantId"));

        mockMvc.perform(get("/api/incidents/{incidentId}", INCIDENT_ID).queryParam("tenantId", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-incident-request"))
                .andExpect(jsonPath("$.errors[0].field").value("tenantId"));
    }

    @Test
    void returnsBadRequestWhenIncidentIdIsMalformed() throws Exception {
        mockMvc.perform(get("/api/incidents/not-a-uuid").queryParam("tenantId", TENANT_ID.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-incident-request"))
                .andExpect(jsonPath("$.title").value("Invalid incident request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("incidentId"));
    }

    @Test
    void returnsStructuredNotFoundForUnavailableIncident() throws Exception {
        when(incidentDetailService.getDetail(TENANT_ID, INCIDENT_ID))
                .thenThrow(new IncidentNotFoundException());

        mockMvc.perform(get("/api/incidents/{incidentId}", INCIDENT_ID)
                        .queryParam("tenantId", TENANT_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:problem:incident-not-found"))
                .andExpect(jsonPath("$.title").value("Incident not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("No incident was found for the requested tenant and incident ID."))
                .andExpect(jsonPath("$.tenantId").doesNotExist())
                .andExpect(jsonPath("$.incidentId").doesNotExist());
    }

    private static IncidentDetailResponse detailResponse() {
        return new IncidentDetailResponse(
                INCIDENT_ID,
                "alert-auth-decline-001",
                IncidentType.AUTHORIZATION_DECLINE_RATE_SPIKE,
                IncidentSeverity.HIGH,
                IncidentStatus.NEW,
                "Authorization decline rate above threshold",
                "Synthetic authorization declines exceeded 25% for five minutes.",
                Instant.parse("2026-08-22T07:14:00Z"),
                Instant.parse("2026-08-22T07:15:00Z"),
                INVESTIGATION_ID);
    }
}
