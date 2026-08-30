package com.cguzowski.syntheticincidentgenerator.generation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioRarity;
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

    @Test
    void redButtonEndpointCreatesOneIncidentAndKeepsTruthInSeparateAnswerKey() throws Exception {
        IncidentGenerationService service = mock(IncidentGenerationService.class);
        when(service.generate()).thenReturn(generated());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new IncidentGenerationController(service))
                .setControllerAdvice(new GeneratorExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(
                        JsonMapper.builder().findAndAddModules().build()))
                .build();

        mockMvc.perform(post("/api/generations"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentId").value("36cfb9b5-21c9-44b8-b10c-ad2a60706ab6"))
                .andExpect(jsonPath("$.queueStatus").value("NEW"))
                .andExpect(jsonPath("$.alert.externalAlertId").value("sig-v1-S203-1788167730-1234567890ab"))
                .andExpect(jsonPath("$.alert.rootCause").doesNotExist())
                .andExpect(jsonPath("$.answerKey.rootCause").value("The OCSP responder was unavailable."))
                .andExpect(jsonPath("$.answerKey.decisionRule")
                        .value(org.hamcrest.Matchers.containsString("otherwise reject")));
    }

    @Test
    void copilotIntakeFailureReturnsSafeBadGatewayWithoutFabricatingAnIncident() throws Exception {
        IncidentGenerationService service = mock(IncidentGenerationService.class);
        when(service.generate()).thenThrow(new AlertIntakeException("Copilot alert intake returned HTTP 503."));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new IncidentGenerationController(service))
                .setControllerAdvice(new GeneratorExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(
                        JsonMapper.builder().findAndAddModules().build()))
                .build();

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
                "S203",
                ScenarioRarity.RARE,
                new GeneratedAlert(
                        "sig-v1-S203-1788167730-1234567890ab",
                        "CRITICAL",
                        Instant.parse("2026-08-31T09:15:30Z"),
                        "Authorization decline rate above threshold",
                        "Synthetic authorizations failed on one encrypted route."),
                new ScenarioTruth(
                        "The OCSP responder was unavailable.",
                        "PROPOSED",
                        "HIGH",
                        List.of("OCSP_RESPONDER_UNAVAILABLE"),
                        "Escalate for certificate-path review.",
                        "Approve only if it matches; otherwise reject it."));
    }
}
