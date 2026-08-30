package com.cguzowski.syntheticincidentgenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.cguzowski.syntheticincidentgenerator.scenario.AlertReferenceCodec;
import com.cguzowski.syntheticincidentgenerator.scenario.EvidenceAvailability;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioDefinition;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioError;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioEvidence;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioRarity;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioTruth;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class IncidentGenerationServiceTest {

    private static final Instant DETECTED_AT = Instant.parse("2026-08-31T09:15:30Z");
    private static final UUID INCIDENT_ID = UUID.fromString("36cfb9b5-21c9-44b8-b10c-ad2a60706ab6");

    @Test
    void submitsOnlySparseAlertFieldsAndReturnsSeparateReviewOracle() throws Exception {
        ScenarioDefinition scenario = scenario();
        AtomicReference<AlertIntakeRequest> submitted = new AtomicReference<>();
        AlertIntakeClient intake = request -> {
            submitted.set(request);
            return new AlertIntakeResponse(
                    INCIDENT_ID, "AUTHORIZATION_DECLINE_RATE_SPIKE", "NEW", Instant.parse("2026-08-31T09:15:31Z"));
        };
        IncidentGenerationService service = new IncidentGenerationService(
                () -> scenario,
                new AlertReferenceCodec(() -> UUID.fromString("12345678-90ab-cdef-1234-567890abcdef")),
                intake,
                Clock.fixed(DETECTED_AT, ZoneOffset.UTC));

        GeneratedIncident result = service.generate();

        assertThat(submitted.get())
                .isEqualTo(new AlertIntakeRequest(
                        "sig-v1-S203-1788167730-1234567890ab",
                        "CRITICAL",
                        DETECTED_AT,
                        "Authorization decline rate above threshold",
                        "Synthetic authorizations failed on one encrypted route."));
        String serializedAlert =
                JsonMapper.builder().findAndAddModules().build().writeValueAsString(submitted.get());
        assertThat(serializedAlert)
                .doesNotContain("rootCause", "expectedDisposition", "requiredEvidence", "OCSP responder");

        assertThat(result.incidentId()).isEqualTo(INCIDENT_ID);
        assertThat(result.queueStatus()).isEqualTo("NEW");
        assertThat(result.scenarioCode()).isEqualTo("S203");
        assertThat(result.rarity()).isEqualTo(ScenarioRarity.RARE);
        assertThat(result.answerKey()).isEqualTo(scenario.truth());
        assertThat(result.alert().externalAlertId()).isEqualTo(submitted.get().externalAlertId());
    }

    private static ScenarioDefinition scenario() {
        return new ScenarioDefinition(
                "S203",
                ScenarioRarity.RARE,
                "CRITICAL",
                "Authorization decline rate above threshold",
                "Synthetic authorizations failed on one encrypted route.",
                new ScenarioEvidence(
                        EvidenceAvailability.AVAILABLE,
                        null,
                        "payment-authorization",
                        List.of(new ScenarioError("OCSP_RESPONDER_UNAVAILABLE", 12, 40))),
                new ScenarioTruth(
                        "The OCSP responder was unavailable.",
                        "PROPOSED",
                        "HIGH",
                        List.of("OCSP_RESPONDER_UNAVAILABLE"),
                        "Escalate for certificate-path review.",
                        "Approve only if it matches; otherwise reject it."));
    }
}
