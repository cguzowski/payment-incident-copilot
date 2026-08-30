package com.cguzowski.paymentcopilot.decision;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

class HumanDecisionControllerTest {

    private static final UUID DECISION_ID = UUID.fromString("955865d8-f60a-4c37-a7f4-92d51b41f01a");
    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INVESTIGATION_ID = UUID.fromString("7f50162a-8dc5-45b0-9c88-dc2f77135e0f");
    private static final UUID INCIDENT_ID = UUID.fromString("ce22cb8d-10d6-4d6d-9a56-f644ae84573d");
    private static final UUID CORRELATION_ID = UUID.fromString("133767cf-8ec8-487d-a09d-19d5efcece07");
    private static final UUID REPORT_ATTEMPT_ID = UUID.fromString("28165339-8e37-49c7-9859-493277b34da2");
    private static final UUID OPERATOR_ID = UUID.fromString("6904706f-d9e6-4543-a1bf-fc4b729e4c05");

    private HumanDecisionService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(HumanDecisionService.class);
        Validator validator = validator();
        JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(
                JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new HumanDecisionController(service, new SyntheticRequestContextResolver()))
                .setControllerAdvice(
                        new HumanDecisionApiExceptionHandler(), new SyntheticRequestContextExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void createsApprovalWithOnlyOutcomeAndReasonFromClient() throws Exception {
        HumanDecision decision = decision(DecisionOutcome.APPROVED, "Reviewed against the cited sources.");
        when(service.record(
                        TENANT_ID,
                        INVESTIGATION_ID,
                        OPERATOR_ID,
                        DecisionOutcome.APPROVED,
                        "Reviewed against the cited sources."))
                .thenReturn(new HumanDecisionRecordResult(decision, true));

        mockMvc.perform(post("/api/investigations/{investigationId}/decisions", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .header("X-Synthetic-Operator-Id", OPERATOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "outcome": "APPROVED",
                                  "reason": "Reviewed against the cited sources."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/investigations/" + INVESTIGATION_ID + "/decisions"))
                .andExpect(jsonPath("$.decisionId").value(DECISION_ID.toString()))
                .andExpect(jsonPath("$.investigationId").value(INVESTIGATION_ID.toString()))
                .andExpect(jsonPath("$.reportAttemptId").value(REPORT_ATTEMPT_ID.toString()))
                .andExpect(jsonPath("$.outcome").value("APPROVED"))
                .andExpect(jsonPath("$.incidentStatus").value("APPROVED"))
                .andExpect(jsonPath("$.reason").value("Reviewed against the cited sources."))
                .andExpect(jsonPath("$.decidedBy").value(OPERATOR_ID.toString()))
                .andExpect(jsonPath("$.tenantId").doesNotExist())
                .andExpect(jsonPath("$.incidentId").doesNotExist())
                .andExpect(jsonPath("$.correlationId").doesNotExist());
    }

    @Test
    void returnsOkForExactReplayAndListsStoredDecision() throws Exception {
        HumanDecision decision = decision(DecisionOutcome.REJECTED, "The cause is not supported.");
        when(service.record(
                        TENANT_ID,
                        INVESTIGATION_ID,
                        OPERATOR_ID,
                        DecisionOutcome.REJECTED,
                        "The cause is not supported."))
                .thenReturn(new HumanDecisionRecordResult(decision, false));
        when(service.history(TENANT_ID, INVESTIGATION_ID)).thenReturn(List.of(decision));

        mockMvc.perform(post("/api/investigations/{investigationId}/decisions", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .header("X-Synthetic-Operator-Id", OPERATOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"outcome":"REJECTED","reason":"The cause is not supported."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("REJECTED"));

        mockMvc.perform(get("/api/investigations/{investigationId}/decisions", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].decisionId").value(DECISION_ID.toString()));
    }

    @Test
    void rejectsMissingMalformedOversizedOrUnknownInputWithoutServiceCall() throws Exception {
        var request = post("/api/investigations/{investigationId}/decisions", INVESTIGATION_ID)
                .header("X-Synthetic-Tenant-Id", TENANT_ID)
                .header("X-Synthetic-Operator-Id", OPERATOR_ID)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request.content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-human-decision"));
        mockMvc.perform(request.content("{\"outcome\":\"APPROVED\",\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(request.content("{\"outcome\":\"APPROVED\",\"reason\":\"" + "x".repeat(1_001) + "\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(request.content("{\"outcome\":\"APPROVED\",\"reason\":\"Reviewed.\",\"reportAttemptId\":\""
                        + REPORT_ATTEMPT_ID
                        + "\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/investigations/not-an-id/decisions")
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .header("X-Synthetic-Operator-Id", OPERATOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\":\"APPROVED\",\"reason\":\"Reviewed.\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/investigations/{investigationId}/decisions", INVESTIGATION_ID)
                        .header("X-Synthetic-Tenant-Id", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"outcome\":\"APPROVED\",\"reason\":\"Reviewed.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-synthetic-request-context"));

        verify(service, never())
                .record(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void mapsMissingAndConflictingDecisionToSafeProblems() throws Exception {
        when(service.record(TENANT_ID, INVESTIGATION_ID, OPERATOR_ID, DecisionOutcome.APPROVED, "Reviewed."))
                .thenThrow(new DecisionInvestigationNotFoundException())
                .thenThrow(new HumanDecisionConflictException("A final human decision is already recorded."));

        var request = post("/api/investigations/{investigationId}/decisions", INVESTIGATION_ID)
                .header("X-Synthetic-Tenant-Id", TENANT_ID)
                .header("X-Synthetic-Operator-Id", OPERATOR_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outcome\":\"APPROVED\",\"reason\":\"Reviewed.\"}");
        mockMvc.perform(request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem:investigation-not-found"));
        mockMvc.perform(request)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:problem:human-decision-conflict"));
    }

    private static HumanDecision decision(DecisionOutcome outcome, String reason) {
        return new HumanDecision(
                DECISION_ID,
                TENANT_ID,
                INVESTIGATION_ID,
                INCIDENT_ID,
                CORRELATION_ID,
                REPORT_ATTEMPT_ID,
                OPERATOR_ID,
                outcome,
                reason,
                Instant.parse("2026-08-30T12:00:00Z"));
    }

    private static Validator validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return validator;
    }
}
