package com.cguzowski.paymentcopilot.incident;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextExceptionHandler;
import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextResolver;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InvestigationControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INCIDENT_ID = UUID.fromString("f4749ecb-49b0-4277-a140-cb69485b082f");
    private static final UUID INVESTIGATION_ID = UUID.fromString("a012c9cb-85a6-4d77-9703-3b53377b56c3");
    private static final UUID OPERATOR_ID = UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1");

    private InvestigationService investigationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        investigationService = mock(InvestigationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new InvestigationController(investigationService, new SyntheticRequestContextResolver()))
                .setControllerAdvice(new ApiExceptionHandler(), new SyntheticRequestContextExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    @Test
    void createsInvestigationAndReturnsCreatedContract() throws Exception {
        when(investigationService.start(TENANT_ID, INCIDENT_ID, OPERATOR_ID))
                .thenReturn(new InvestigationStartResult(response(), true));

        mockMvc.perform(startRequest(INCIDENT_ID, TENANT_ID.toString(), OPERATOR_ID.toString()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/investigations/" + INVESTIGATION_ID))
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.investigationId").value(INVESTIGATION_ID.toString()))
                .andExpect(jsonPath("$.incidentId").value(INCIDENT_ID.toString()))
                .andExpect(jsonPath("$.incidentStatus").value("INVESTIGATING"))
                .andExpect(jsonPath("$.startedBy").value(OPERATOR_ID.toString()))
                .andExpect(jsonPath("$.startedAt").value("2026-08-27T18:30:00Z"));
    }

    @Test
    void returnsExistingInvestigationForRepeatedStart() throws Exception {
        when(investigationService.start(TENANT_ID, INCIDENT_ID, OPERATOR_ID))
                .thenReturn(new InvestigationStartResult(response(), false));

        mockMvc.perform(startRequest(INCIDENT_ID, TENANT_ID.toString(), OPERATOR_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.investigationId").value(INVESTIGATION_ID.toString()));
    }

    @Test
    void rejectsMalformedTenantIncidentInvestigationAndOperatorIds() throws Exception {
        mockMvc.perform(startRequest(INCIDENT_ID, "not-a-tenant", OPERATOR_ID.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-synthetic-request-context"))
                .andExpect(jsonPath("$.errors[0].field").value("tenantId"));

        mockMvc.perform(startRequest(INCIDENT_ID, TENANT_ID.toString(), "not-an-operator"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-synthetic-request-context"))
                .andExpect(jsonPath("$.errors[0].field").value("operatorId"));

        mockMvc.perform(startRequest("not-an-incident", TENANT_ID.toString(), OPERATOR_ID.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-investigation-request"));

        mockMvc.perform(get("/api/investigations/not-an-investigation")
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-investigation-request"));
    }

    @Test
    void requiresOperatorHeaderAndRejectsLegacyIdentityTransport() throws Exception {
        mockMvc.perform(post("/api/incidents/{incidentId}/investigations", INCIDENT_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-synthetic-request-context"))
                .andExpect(jsonPath("$.errors[0].field").value("operatorId"));

        mockMvc.perform(startRequest(INCIDENT_ID, TENANT_ID.toString(), OPERATOR_ID.toString())
                        .queryParam("tenantId", TENANT_ID.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-synthetic-request-context"));

        mockMvc.perform(startRequest(INCIDENT_ID, TENANT_ID.toString(), OPERATOR_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operatorId\":\"" + OPERATOR_ID + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-investigation-request"))
                .andExpect(jsonPath("$.errors[0].field").value("request"));
    }

    @Test
    void returnsTenantScopedInvestigationWorkspace() throws Exception {
        when(investigationService.get(TENANT_ID, INVESTIGATION_ID)).thenReturn(response());

        mockMvc.perform(get("/api/investigations/{investigationId}", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.investigationId").value(INVESTIGATION_ID.toString()))
                .andExpect(jsonPath("$.incidentId").value(INCIDENT_ID.toString()));
    }

    @Test
    void returnsInvestigationNotFoundWithoutTenantLeakage() throws Exception {
        when(investigationService.get(TENANT_ID, INVESTIGATION_ID)).thenThrow(new InvestigationNotFoundException());

        mockMvc.perform(get("/api/investigations/{investigationId}", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem:investigation-not-found"))
                .andExpect(jsonPath("$.tenantId").doesNotExist())
                .andExpect(jsonPath("$.investigationId").doesNotExist());
    }

    private static InvestigationResponse response() {
        return new InvestigationResponse(
                INVESTIGATION_ID,
                INCIDENT_ID,
                IncidentStatus.INVESTIGATING,
                OPERATOR_ID,
                Instant.parse("2026-08-27T18:30:00Z"));
    }

    private static MockHttpServletRequestBuilder startRequest(Object incidentId, String tenantId, String operatorId) {
        return post("/api/incidents/{incidentId}/investigations", incidentId)
                .header("X-Synthetic-Tenant-Id", tenantId)
                .header("X-Synthetic-Operator-Id", operatorId);
    }
}
