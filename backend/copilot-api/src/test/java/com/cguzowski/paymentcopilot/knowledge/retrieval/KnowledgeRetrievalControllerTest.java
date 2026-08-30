package com.cguzowski.paymentcopilot.knowledge.retrieval;

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

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingClient;
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

class KnowledgeRetrievalControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INVESTIGATION_ID = UUID.fromString("7f50162a-8dc5-45b0-9c88-dc2f77135e0f");

    private KnowledgeRetrievalService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(KnowledgeRetrievalService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new KnowledgeRetrievalController(service, new SyntheticRequestContextResolver()))
                .setControllerAdvice(new KnowledgeApiExceptionHandler(), new SyntheticRequestContextExceptionHandler())
                .build();
    }

    @Test
    void createsRetrievalWithoutClientQueryParametersAndReturnsRawSourceOnly() throws Exception {
        when(service.retrieve(TENANT_ID, INVESTIGATION_ID)).thenReturn(response());

        mockMvc.perform(post("/api/investigations/{investigationId}/knowledge-retrievals", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                                "Location", "/api/investigations/" + INVESTIGATION_ID + "/knowledge-retrievals"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.results[0].documentType").value("RUNBOOK"))
                .andExpect(jsonPath("$.results[0].rawContent").value("Inspect GATEWAY_TIMEOUT observations."))
                .andExpect(jsonPath("$.results[0].embeddingInput").doesNotExist());

        verify(service).retrieve(TENANT_ID, INVESTIGATION_ID);
    }

    @Test
    void returnsHistoryNewestFirstAsProvidedByService() throws Exception {
        when(service.history(TENANT_ID, INVESTIGATION_ID)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/investigations/{investigationId}/knowledge-retrievals", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].retrievalId")
                        .value(response().retrievalId().toString()));
    }

    @Test
    void rejectsMalformedIdentifiersAndUnexpectedBodyWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/investigations/not-an-id/knowledge-retrievals")
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-investigation-request"));
        mockMvc.perform(post("/api/investigations/{investigationId}/knowledge-retrievals", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", "bad-tenant"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-synthetic-request-context"));
        mockMvc.perform(post("/api/investigations/{investigationId}/knowledge-retrievals", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).retrieve(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsIndistinguishableNotFoundForMissingOrCrossTenantInvestigation() throws Exception {
        when(service.retrieve(TENANT_ID, INVESTIGATION_ID)).thenThrow(new KnowledgeInvestigationNotFoundException());

        mockMvc.perform(post("/api/investigations/{investigationId}/knowledge-retrievals", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:investigation-not-found"))
                .andExpect(jsonPath("$.detail")
                        .value("No investigation was found for the requested tenant and investigation ID."));
    }

    private static KnowledgeRetrievalResponse response() {
        Instant requestedAt = Instant.parse("2026-08-28T10:00:00Z");
        return new KnowledgeRetrievalResponse(
                UUID.fromString("a74f88ed-e295-4caf-9404-a22f733d86ec"),
                KnowledgeRetrievalStatus.AVAILABLE,
                requestedAt,
                Instant.parse("2026-08-28T10:00:02Z"),
                "Incident type: AUTHORIZATION_DECLINE_RATE_SPIKE",
                "knowledge-query/v1",
                List.of(),
                KnowledgeEmbeddingClient.MODEL_ID,
                KnowledgeEmbeddingClient.DIMENSIONS,
                new KnowledgeMetadataFilters(
                        "AUTHORIZATION_DECLINE_RATE_SPIKE",
                        List.of(KnowledgeDocumentType.RUNBOOK, KnowledgeDocumentType.POLICY),
                        KnowledgeApprovalStatus.APPROVED,
                        requestedAt),
                "postgres-hybrid-rrf/v1",
                60,
                20,
                0.0f,
                0.55f,
                null,
                List.of(new KnowledgeRetrievalResultResponse(
                        UUID.fromString("21111111-1111-4111-8111-111111111111"),
                        UUID.fromString("11111111-1111-4111-8111-111111111111"),
                        UUID.fromString("31111111-1111-4111-8111-111111111111"),
                        1,
                        0.5f,
                        1,
                        1.0f,
                        0.0f,
                        1,
                        1,
                        2.0 / 61.0,
                        KnowledgeDocumentType.RUNBOOK,
                        "Authorization Decline Runbook",
                        "1.0.0",
                        "Card authorization",
                        "Gateway Failures > Diagnosis",
                        "Inspect GATEWAY_TIMEOUT observations.",
                        20,
                        22,
                        KnowledgeApprovalStatus.APPROVED,
                        UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"),
                        Instant.parse("2026-08-20T10:00:00Z"),
                        Instant.parse("2026-08-21T00:00:00Z"))));
    }
}
