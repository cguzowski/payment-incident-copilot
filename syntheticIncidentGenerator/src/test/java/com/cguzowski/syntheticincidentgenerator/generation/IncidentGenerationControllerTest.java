package com.cguzowski.syntheticincidentgenerator.generation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioTruth;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

class IncidentGenerationControllerTest {

    private static final UUID INCIDENT_ID = UUID.fromString("36cfb9b5-21c9-44b8-b10c-ad2a60706ab6");
    private static final UUID OPERATOR_ID = UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1");

    @Test
    void redButtonEndpointCreatesOneIncidentWithoutReturningTruth() throws Exception {
        IncidentGenerationService service = mock(IncidentGenerationService.class);
        when(service.generate()).thenReturn(generated());
        MockMvc mockMvc = mockMvc(service, mock(AnswerKeyRevealService.class));

        mockMvc.perform(post("/api/generations"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentId").value(INCIDENT_ID.toString()))
                .andExpect(jsonPath("$.queueStatus").value("NEW"))
                .andExpect(jsonPath("$.alert.externalAlertId").value("sig-v1-S203-1788167730-1234567890ab"))
                .andExpect(jsonPath("$.alert.rootCause").doesNotExist())
                .andExpect(jsonPath("$.scenarioCode").doesNotExist())
                .andExpect(jsonPath("$.rarity").doesNotExist())
                .andExpect(jsonPath("$.answerKey").doesNotExist())
                .andExpect(jsonPath("$.rootCause").doesNotExist());
    }

    @Test
    void revealEndpointReturnsOracleOnlyAfterServiceAuthorizesTheTerminalIncident() throws Exception {
        AnswerKeyRevealService revealService = mock(AnswerKeyRevealService.class);
        when(revealService.reveal(INCIDENT_ID, OPERATOR_ID))
                .thenReturn(new AnswerKeyRevealResponse(
                        INCIDENT_ID,
                        "APPROVED",
                        OPERATOR_ID,
                        Instant.parse("2026-09-24T12:00:00Z"),
                        "scenario-catalog/v1",
                        truth()));
        MockMvc mockMvc = mockMvc(mock(IncidentGenerationService.class), revealService);

        mockMvc.perform(post("/api/generations/{incidentId}/answer-key", INCIDENT_ID)
                        .header("X-Synthetic-Operator-Id", OPERATOR_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(INCIDENT_ID.toString()))
                .andExpect(jsonPath("$.terminalStatus").value("APPROVED"))
                .andExpect(jsonPath("$.revealedBy").value(OPERATOR_ID.toString()))
                .andExpect(jsonPath("$.revealedAt").value("2026-09-24T12:00:00Z"))
                .andExpect(jsonPath("$.oracleVersion").value("scenario-catalog/v1"))
                .andExpect(jsonPath("$.answerKey.rootCause").value("The OCSP responder was unavailable."));
    }

    @Test
    void revealEndpointRejectsMissingOperatorIdentity() throws Exception {
        MockMvc mockMvc = mockMvc(mock(IncidentGenerationService.class), mock(AnswerKeyRevealService.class));

        mockMvc.perform(post("/api/generations/{incidentId}/answer-key", INCIDENT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-answer-key-reveal"));
    }

    @Test
    void revealEndpointReturnsConflictWhileTheAnswerKeyIsSealed() throws Exception {
        AnswerKeyRevealService revealService = mock(AnswerKeyRevealService.class);
        when(revealService.reveal(INCIDENT_ID, OPERATOR_ID)).thenThrow(new AnswerKeyNotReadyException());
        MockMvc mockMvc = mockMvc(mock(IncidentGenerationService.class), revealService);

        mockMvc.perform(post("/api/generations/{incidentId}/answer-key", INCIDENT_ID)
                        .header("X-Synthetic-Operator-Id", OPERATOR_ID.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:problem:answer-key-not-ready"))
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "The answer key remains sealed until the incident has an approved or rejected human decision."));
    }

    @Test
    void revealEndpointFailsSafelyWhenCopilotStateCannotBeConfirmed() throws Exception {
        AnswerKeyRevealService revealService = mock(AnswerKeyRevealService.class);
        when(revealService.reveal(INCIDENT_ID, OPERATOR_ID)).thenThrow(new AnswerKeyUnavailableException());
        MockMvc mockMvc = mockMvc(mock(IncidentGenerationService.class), revealService);

        mockMvc.perform(post("/api/generations/{incidentId}/answer-key", INCIDENT_ID)
                        .header("X-Synthetic-Operator-Id", OPERATOR_ID.toString()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.type").value("urn:problem:answer-key-unavailable"))
                .andExpect(jsonPath("$.detail").value("The answer-key reveal could not be safely authorized."));
    }

    private static MockMvc mockMvc(IncidentGenerationService generationService, AnswerKeyRevealService revealService) {
        return MockMvcBuilders.standaloneSetup(new IncidentGenerationController(generationService, revealService))
                .setControllerAdvice(new GeneratorExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(
                        JsonMapper.builder().findAndAddModules().build()))
                .build();
    }

    @Test
    void copilotIntakeFailureReturnsSafeBadGatewayWithoutFabricatingAnIncident() throws Exception {
        IncidentGenerationService service = mock(IncidentGenerationService.class);
        when(service.generate()).thenThrow(new AlertIntakeException("Copilot alert intake returned HTTP 503."));
        MockMvc mockMvc = mockMvc(service, mock(AnswerKeyRevealService.class));

        mockMvc.perform(post("/api/generations"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.type").value("urn:problem:copilot-alert-intake-unavailable"))
                .andExpect(jsonPath("$.detail").value("The synthetic alert was not accepted by the copilot API."));
    }

    private static GeneratedIncident generated() {
        return new GeneratedIncident(
                UUID.fromString("36cfb9b5-21c9-44b8-b10c-ad2a60706ab6"),
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "NEW",
                Instant.parse("2026-08-31T09:15:31Z"),
                new GeneratedAlert(
                        "sig-v1-S203-1788167730-1234567890ab",
                        "CRITICAL",
                        Instant.parse("2026-08-31T09:15:30Z"),
                        "Authorization decline rate above threshold",
                        "Synthetic authorizations failed on one encrypted route."));
    }

    private static ScenarioTruth truth() {
        return new ScenarioTruth(
                "The OCSP responder was unavailable.",
                "PROPOSED",
                "HIGH",
                List.of("OCSP_RESPONDER_UNAVAILABLE"),
                "Escalate for certificate-path review.",
                "Approve only if it matches; otherwise reject it.");
    }
}
