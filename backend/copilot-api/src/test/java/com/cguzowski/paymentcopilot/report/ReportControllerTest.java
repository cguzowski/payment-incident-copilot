package com.cguzowski.paymentcopilot.report;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextExceptionHandler;
import com.cguzowski.paymentcopilot.requestcontext.SyntheticRequestContextResolver;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ReportControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID OPERATOR_ID = UUID.fromString("6904706f-d9e6-4543-a1bf-fc4b729e4c05");
    private static final UUID INVESTIGATION_ID = UUID.fromString("7f50162a-8dc5-45b0-9c88-dc2f77135e0f");

    private ReportGenerationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ReportGenerationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ReportController(service, new SyntheticRequestContextResolver()))
                .setControllerAdvice(new ReportApiExceptionHandler(), new SyntheticRequestContextExceptionHandler())
                .build();
    }

    @Test
    void createsReportWithoutClientPromptModelSchemaOrSources() throws Exception {
        when(service.generate(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID)).thenReturn(response());

        mockMvc.perform(post("/api/investigations/{investigationId}/reports", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .header("X-Synthetic-Operator-Id", OPERATOR_ID))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/investigations/" + INVESTIGATION_ID + "/reports"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.report.disposition").value("PROPOSED"))
                .andExpect(jsonPath("$.report.summary.evidenceIds[0]").isNotEmpty())
                .andExpect(
                        jsonPath("$.report.recommendation.knowledgeChunkIds[0]").isNotEmpty());

        verify(service).generate(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID);
    }

    @Test
    void returnsHistoryNewestFirstAsProvidedByService() throws Exception {
        when(service.history(TENANT_ID, INVESTIGATION_ID)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/investigations/{investigationId}/reports", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].attemptId").value(response().attemptId().toString()));
    }

    @Test
    void requiresOperatorAndRejectsMalformedIdOrBodyWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/investigations/{investigationId}/reports", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-synthetic-request-context"));
        mockMvc.perform(post("/api/investigations/not-an-id/reports")
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .header("X-Synthetic-Operator-Id", OPERATOR_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-report-request"));
        mockMvc.perform(post("/api/investigations/{investigationId}/reports", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .header("X-Synthetic-Operator-Id", OPERATOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(service, never())
                .generate(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void mapsNotFoundAndPrerequisiteConflictToProblemDetails() throws Exception {
        when(service.generate(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID))
                .thenThrow(new ReportInvestigationNotFoundException())
                .thenThrow(new ReportGenerationConflictException("Terminal evidence is required."));

        var request = post("/api/investigations/{investigationId}/reports", INVESTIGATION_ID)
                .header("X-Synthetic-Tenant-Id", TENANT_ID)
                .header("X-Synthetic-Operator-Id", OPERATOR_ID);
        mockMvc.perform(request)
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:investigation-not-found"));
        mockMvc.perform(request)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:problem:report-generation-conflict"))
                .andExpect(jsonPath("$.detail").value("Terminal evidence is required."));
    }

    private static ReportGenerationResponse response() {
        UUID evidenceId = UUID.fromString("d3a12cd9-ef1d-4328-b63d-2ecea6558e2d");
        UUID chunkId = UUID.fromString("97ec5709-147d-458a-a5b4-c95de1a7a32a");
        ReportClaim evidence = new ReportClaim("Timeouts were observed.", List.of(evidenceId), List.of());
        ReportClaim guided = new ReportClaim("Follow diagnostics.", List.of(evidenceId), List.of(chunkId));
        return new ReportGenerationResponse(
                UUID.fromString("28165339-8e37-49c7-9859-493277b34da2"),
                INVESTIGATION_ID,
                ReportGenerationStatus.AVAILABLE,
                Instant.parse("2026-08-29T10:00:00Z"),
                Instant.parse("2026-08-29T10:00:02Z"),
                "global.amazon.nova-2-lite-v1:0",
                "report-prompt/v1",
                "report-v1",
                evidenceId,
                evidenceId,
                UUID.fromString("a74f88ed-e295-4caf-9404-a22f733d86ec"),
                null,
                new ReportDocument(
                        ReportDisposition.PROPOSED,
                        evidence,
                        List.of(evidence),
                        List.of(guided),
                        guided,
                        new ReportConfidence(ReportConfidenceLevel.MEDIUM, "One source.", List.of(evidenceId)),
                        guided,
                        List.of(),
                        List.of()));
    }
}
