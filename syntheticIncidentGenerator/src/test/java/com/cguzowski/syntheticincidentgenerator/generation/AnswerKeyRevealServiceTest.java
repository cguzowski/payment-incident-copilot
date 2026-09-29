package com.cguzowski.syntheticincidentgenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cguzowski.syntheticincidentgenerator.scenario.AlertReferenceCodec;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioOracleCatalog;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioOracleEntry;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioTruth;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AnswerKeyRevealServiceTest {

    private static final UUID INCIDENT_ID = UUID.fromString("36cfb9b5-21c9-44b8-b10c-ad2a60706ab6");
    private static final UUID OPERATOR_ID = UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1");
    private static final Instant REVEALED_AT = Instant.parse("2026-09-24T12:00:00Z");
    private static final String REFERENCE = "sig-v1-S203-1788167730-1234567890ab";

    @ParameterizedTest
    @ValueSource(strings = {"APPROVED", "REJECTED"})
    void revealsCanonicalScenarioAfterTerminalDecisionAndCapturesAuditMetadata(String terminalStatus) {
        AtomicReference<AnswerKeyRevealEvent> audited = new AtomicReference<>();
        AnswerKeyRevealService service =
                service(ignored -> new IncidentReviewState(INCIDENT_ID, REFERENCE, terminalStatus), audited::set);

        AnswerKeyRevealResponse response = service.reveal(INCIDENT_ID, OPERATOR_ID);

        assertThat(response.incidentId()).isEqualTo(INCIDENT_ID);
        assertThat(response.terminalStatus()).isEqualTo(terminalStatus);
        assertThat(response.revealedBy()).isEqualTo(OPERATOR_ID);
        assertThat(response.revealedAt()).isEqualTo(REVEALED_AT);
        assertThat(response.oracleVersion()).isEqualTo("scenario-oracle/v1");
        assertThat(response.answerKey()).isEqualTo(truth());
        assertThat(audited.get())
                .isEqualTo(new AnswerKeyRevealEvent(
                        INCIDENT_ID, OPERATOR_ID, terminalStatus, "S203", "scenario-oracle/v1", REVEALED_AT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NEW", "INVESTIGATING", "AWAITING_REVIEW"})
    void rejectsRevealBeforeTheHumanDecisionIsTerminal(String status) {
        AtomicReference<AnswerKeyRevealEvent> audited = new AtomicReference<>();
        AnswerKeyRevealService service =
                service(ignored -> new IncidentReviewState(INCIDENT_ID, REFERENCE, status), audited::set);

        assertThatThrownBy(() -> service.reveal(INCIDENT_ID, OPERATOR_ID))
                .isInstanceOf(AnswerKeyNotReadyException.class);
        assertThat(audited).hasValue(null);
    }

    @Test
    void failsClosedWhenCanonicalIncidentReferenceCannotResolveToAnOracleScenario() {
        AnswerKeyRevealService service = service(
                ignored -> new IncidentReviewState(INCIDENT_ID, "alert-without-an-oracle", "APPROVED"), ignored -> {});

        assertThatThrownBy(() -> service.reveal(INCIDENT_ID, OPERATOR_ID))
                .isInstanceOf(AnswerKeyUnavailableException.class);
    }

    private static AnswerKeyRevealService service(
            CopilotIncidentReviewClient client, AnswerKeyRevealAuditSink auditSink) {
        ScenarioOracleEntry scenario = new ScenarioOracleEntry("S203", truth());
        ScenarioOracleCatalog catalog = new ScenarioOracleCatalog() {
            @Override
            public String version() {
                return "scenario-oracle/v1";
            }

            @Override
            public List<ScenarioOracleEntry> all() {
                return List.of(scenario);
            }

            @Override
            public Optional<ScenarioOracleEntry> findByCode(String code) {
                return scenario.code().equals(code) ? Optional.of(scenario) : Optional.empty();
            }
        };
        return new AnswerKeyRevealService(
                client,
                catalog,
                new AlertReferenceCodec(UUID::randomUUID),
                auditSink,
                Clock.fixed(REVEALED_AT, ZoneOffset.UTC));
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
