package com.cguzowski.paymentcopilot.incident;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EvidenceCollectionControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INVESTIGATION_ID = UUID.fromString("a012c9cb-85a6-4d77-9703-3b53377b56c3");
    private static final UUID EVIDENCE_ID = UUID.fromString("a8bab9d4-dccc-4e70-acfe-174ac63a3b12");
    private static final UUID TOOL_CALL_ID = UUID.fromString("21fdc56b-267a-4cb5-81b9-50f092e0ef35");

    private EvidenceCollectionService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(EvidenceCollectionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new EvidenceCollectionController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    @Test
    void returnsCreatedForRecordedUnavailableAttempt() throws Exception {
        when(service.collect(TENANT_ID, INVESTIGATION_ID)).thenReturn(unavailableResponse());

        mockMvc.perform(post("/api/investigations/{investigationId}/evidence-collections", INVESTIGATION_ID)
                        .queryParam("tenantId", TENANT_ID.toString()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/investigations/" + INVESTIGATION_ID + "/evidence-collections"))
                .andExpect(jsonPath("$.*", hasSize(11)))
                .andExpect(jsonPath("$.evidenceId").value(EVIDENCE_ID.toString()))
                .andExpect(jsonPath("$.status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.sourceSystem").value("synthetic-observability"))
                .andExpect(jsonPath("$.sourceTool").value("getRecentServiceErrors"))
                .andExpect(jsonPath("$.toolCallId").value(TOOL_CALL_ID.toString()))
                .andExpect(jsonPath("$.requestedAt").value("2026-08-28T10:00:00Z"))
                .andExpect(jsonPath("$.retrievedAt").value("2026-08-28T10:00:01Z"))
                .andExpect(jsonPath("$.completedAt").value("2026-08-28T10:00:02Z"))
                .andExpect(jsonPath("$.contentSchemaVersion").value("service-errors/v1"))
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.statusDetail").value("Synthetic source unavailable."))
                .andExpect(jsonPath("$.tenantId").doesNotExist())
                .andExpect(jsonPath("$.correlationId").doesNotExist())
                .andExpect(jsonPath("$.scenarioReference").doesNotExist());
    }

    @Test
    void returnsEvidenceAttemptsNewestFirst() throws Exception {
        EvidenceCollectionResponse newest = unavailableResponse();
        EvidenceCollectionResponse older = new EvidenceCollectionResponse(
                UUID.randomUUID(),
                EvidenceCollectionStatus.STARTED,
                "synthetic-observability",
                "getRecentServiceErrors",
                UUID.randomUUID(),
                Instant.parse("2026-08-28T09:59:00Z"),
                null,
                null,
                "service-errors/v1",
                null,
                null);
        when(service.history(TENANT_ID, INVESTIGATION_ID)).thenReturn(List.of(newest, older));

        mockMvc.perform(get("/api/investigations/{investigationId}/evidence-collections", INVESTIGATION_ID)
                        .queryParam("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].evidenceId").value(EVIDENCE_ID.toString()))
                .andExpect(jsonPath("$[1].status").value("STARTED"));
    }

    @Test
    void rejectsMalformedIdentifiersAndAnyRequestBodyWithoutCollection() throws Exception {
        mockMvc.perform(post("/api/investigations/not-an-investigation/evidence-collections")
                        .queryParam("tenantId", TENANT_ID.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-investigation-request"));
        mockMvc.perform(post("/api/investigations/{investigationId}/evidence-collections", INVESTIGATION_ID)
                        .queryParam("tenantId", "not-a-tenant"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/investigations/{investigationId}/evidence-collections", INVESTIGATION_ID)
                        .queryParam("tenantId", TENANT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("request"));

        verify(service, never()).collect(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsInvestigationNotFoundWithoutTenantLeakage() throws Exception {
        when(service.history(TENANT_ID, INVESTIGATION_ID)).thenThrow(new InvestigationNotFoundException());

        mockMvc.perform(get("/api/investigations/{investigationId}/evidence-collections", INVESTIGATION_ID)
                        .queryParam("tenantId", TENANT_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem:investigation-not-found"))
                .andExpect(jsonPath("$.tenantId").doesNotExist())
                .andExpect(jsonPath("$.investigationId").doesNotExist());
    }

    private static EvidenceCollectionResponse unavailableResponse() {
        return new EvidenceCollectionResponse(
                EVIDENCE_ID,
                EvidenceCollectionStatus.UNAVAILABLE,
                "synthetic-observability",
                "getRecentServiceErrors",
                TOOL_CALL_ID,
                Instant.parse("2026-08-28T10:00:00Z"),
                Instant.parse("2026-08-28T10:00:01Z"),
                Instant.parse("2026-08-28T10:00:02Z"),
                "service-errors/v1",
                null,
                "Synthetic source unavailable.");
    }
}
