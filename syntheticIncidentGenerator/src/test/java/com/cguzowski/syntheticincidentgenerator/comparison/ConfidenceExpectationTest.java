package com.cguzowski.syntheticincidentgenerator.comparison;

import static org.assertj.core.api.Assertions.*;

import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioTruth;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ConfidenceExpectationTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void directDiagnosticSignaturesCanReachHighWithoutIndependentConfirmation() {
        for (String[] pair : List.of(
                new String[] {"TLS_CERTIFICATE_EXPIRED", "TLS_HANDSHAKE_FAILED"},
                new String[] {"HSM_QUORUM_LOST", "HSM_SIGNING_TIMEOUT"},
                new String[] {"HSM_FIRMWARE_PROTOCOL_MISMATCH", "HSM_SIGNING_FAILED"})) {
            var evidence = snapshot("AVAILABLE", errors(pair[0], pair[1], 72, 72));
            assertThat(expect(List.of(pair), evidence).level()).isEqualTo("HIGH");
        }
    }

    @Test
    void rateLimitAndDnsPairsRemainMediumRegardlessOfVolume() {
        assertThat(expect(
                                List.of("UPSTREAM_RATE_LIMITED"),
                                snapshot("AVAILABLE", errors("UPSTREAM_RATE_LIMITED", null, 999999, 0)))
                        .level())
                .isEqualTo("MEDIUM");
        assertThat(expect(
                                List.of("DNS_SECURITY_POLICY_BLOCK", "DNS_RESOLUTION_FAILED"),
                                snapshot(
                                        "AVAILABLE",
                                        errors("DNS_SECURITY_POLICY_BLOCK", "DNS_RESOLUTION_FAILED", 72, 72)))
                        .level())
                .isEqualTo("MEDIUM");
    }

    @Test
    void incompleteOrInconsistentDirectSignatureCannotReachHigh() {
        var required = List.of("TLS_CERTIFICATE_EXPIRED", "TLS_HANDSHAKE_FAILED");
        assertThat(expect(required, snapshot("AVAILABLE", errors(required.get(0), null, 72, 0)))
                        .level())
                .isEqualTo("LOW");
        assertThat(expect(required, snapshot("AVAILABLE", errors(required.get(0), required.get(1), 72, 71)))
                        .level())
                .isEqualTo("MEDIUM");
        String reversed = errors(required.get(0), required.get(1), 72, 72).replace("11:57:00", "11:59:00");
        assertThat(expect(required, snapshot("AVAILABLE", reversed)).level()).isEqualTo("MEDIUM");
        String extra = errors(required.get(0), required.get(1), 72, 72)
                .replace("]", "," + row("UPSTREAM_RATE_LIMITED", 72, "11:58:00") + "]");
        assertThat(expect(required, snapshot("AVAILABLE", extra)).level()).isEqualTo("MEDIUM");
    }

    @Test
    void degradedEmptyInvalidAndSymptomOnlyEvidenceExpectLow() {
        for (String status : List.of("PARTIAL", "UNAVAILABLE", "MALFORMED", "TIMEOUT")) {
            assertThat(expect(List.of(), snapshot(status, errors("UPSTREAM_RATE_LIMITED", null, 83, 0)))
                            .level())
                    .isEqualTo("LOW");
        }
        for (String errors : List.of(
                "[]",
                errors("GATEWAY_TIMEOUT", null, 83, 0),
                errors("UPSTREAM_RATE_LIMITED", null, 0, 0),
                errors("UPSTREAM_RATE_LIMITED", null, 83, 0).replace("11:57:00", "12:01:00"),
                errors("UPSTREAM_RATE_LIMITED", null, 83, 0).replace("11:57:00", "invalid"))) {
            assertThat(expect(List.of(), snapshot("AVAILABLE", errors)).level()).isEqualTo("LOW");
        }
        var badWindow = snapshot("AVAILABLE", errors("UPSTREAM_RATE_LIMITED", null, 83, 0));
        assertThat(expect(List.of(), mapper.readTree(badWindow.toString().replace("11:55:00", "12:05:00")))
                        .level())
                .isEqualTo("LOW");
    }

    @Test
    void strongHistoryCannotOverrideDegradedLatestAndMissingBindingFails() {
        var latest = snapshot("PARTIAL", "[]");
        var history = snapshot("AVAILABLE", errors("TLS_CERTIFICATE_EXPIRED", "TLS_HANDSHAKE_FAILED", 72, 72));
        var combined = mapper.readTree(
                "[" + history.get(0).toString().replace("latest", "historical") + "," + latest.get(0) + "]");
        assertThat(expect(List.of(), combined).level()).isEqualTo("LOW");
        assertThatThrownBy(() ->
                        ConfidenceExpectation.evaluate(truth(List.of()), mapper.readTree("[]"), mapper.readTree("{}")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> expect(List.of(), mapper.readTree("[]"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownSignalsNeverBecomeHighAndMissingRequiredSignalsExpectLow() {
        assertThat(expect(
                                List.of("NEW_DIAGNOSTIC_CODE"),
                                snapshot("AVAILABLE", errors("NEW_DIAGNOSTIC_CODE", null, 83, 0)))
                        .level())
                .isEqualTo("LOW");
        assertThat(expect(
                                List.of("TLS_CERTIFICATE_EXPIRED"),
                                snapshot("AVAILABLE", errors("UPSTREAM_RATE_LIMITED", null, 83, 0)))
                        .level())
                .isEqualTo("LOW");
    }

    @Test
    void competingDirectDiagnosesAndUnknownExtraSignalsReduceSupport() {
        String competing = errors("TLS_CERTIFICATE_EXPIRED", "TLS_HANDSHAKE_FAILED", 72, 72)
                .replace("]", "," + row("HSM_QUORUM_LOST", 72, "11:58:00") + "]");
        assertThat(expect(List.of("TLS_CERTIFICATE_EXPIRED", "TLS_HANDSHAKE_FAILED"), snapshot("AVAILABLE", competing))
                        .level())
                .isEqualTo("LOW");
        String unknown = errors("TLS_CERTIFICATE_EXPIRED", "TLS_HANDSHAKE_FAILED", 72, 72)
                .replace("]", "," + row("NEW_DIAGNOSTIC_CODE", 72, "11:58:00") + "]");
        assertThat(expect(List.of("TLS_CERTIFICATE_EXPIRED", "TLS_HANDSHAKE_FAILED"), snapshot("AVAILABLE", unknown))
                        .level())
                .isEqualTo("LOW");
    }

    private ConfidenceExpectation expect(List<String> required, JsonNode evidence) {
        return ConfidenceExpectation.evaluate(
                truth(required), evidence, mapper.readTree("{\"latestEvidenceId\":\"latest\"}"));
    }

    private ScenarioTruth truth(List<String> required) {
        return new ScenarioTruth("Narrow mechanism", "PROPOSED", "HIGH", required, "Escalate", "Match");
    }

    private JsonNode snapshot(String status, String errors) {
        return mapper.readTree("""
                [{"evidenceId":"latest","status":"%s","contentSchemaVersion":"service-errors/v1",
                "content":{"serviceName":"payment-authorization","observedFrom":"2026-10-01T11:55:00Z",
                "observedTo":"2026-10-01T12:00:00Z","errors":%s}}]
                """.formatted(status, errors));
    }

    private String errors(String cause, String effect, int causeCount, int effectCount) {
        return "[" + row(cause, causeCount, "11:57:00")
                + (effect == null ? "" : "," + row(effect, effectCount, "11:58:00")) + "]";
    }

    private String row(String code, int count, String time) {
        return "{\"errorCode\":\"%s\",\"count\":%d,\"observedAt\":\"2026-10-01T%sZ\"}".formatted(code, count, time);
    }
}
