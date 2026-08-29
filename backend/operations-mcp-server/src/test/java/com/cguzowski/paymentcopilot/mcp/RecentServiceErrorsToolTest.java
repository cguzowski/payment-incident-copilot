package com.cguzowski.paymentcopilot.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecentServiceErrorsToolTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID CORRELATION_ID = UUID.fromString("a5d978b5-34c7-42da-9076-22f8e5169315");
    private static final UUID TOOL_CALL_ID = UUID.fromString("21fdc56b-267a-4cb5-81b9-50f092e0ef35");
    private static final Instant RETRIEVED_AT = Instant.parse("2026-08-28T10:00:00Z");

    @Test
    void returnsDeterministicErrorsForKnownScenario() {
        RecentServiceErrorsContent content = new RecentServiceErrorsContent(
                "payment-authorization",
                Instant.parse("2026-08-22T07:09:00Z"),
                Instant.parse("2026-08-22T07:14:00Z"),
                List.of(new RecentServiceErrorObservation(
                        "service-error-001", Instant.parse("2026-08-22T07:12:00Z"), "GATEWAY_TIMEOUT", 37)));
        RecentServiceErrorScenarioRepository repository = (tenantId, scenarioReference) ->
                Optional.of(new RecentServiceErrorScenario(EvidenceAvailabilityStatus.AVAILABLE, null, content));
        RecentServiceErrorsTool tool =
                new RecentServiceErrorsTool(repository, Clock.fixed(RETRIEVED_AT, ZoneOffset.UTC));

        RecentServiceErrorsResult result =
                tool.getRecentServiceErrors(TENANT_ID, "alert-auth-decline-001", CORRELATION_ID, TOOL_CALL_ID);

        assertThat(result)
                .isEqualTo(new RecentServiceErrorsResult(
                        "synthetic-observability",
                        "getRecentServiceErrors",
                        RETRIEVED_AT,
                        CORRELATION_ID,
                        TOOL_CALL_ID,
                        EvidenceAvailabilityStatus.AVAILABLE,
                        null,
                        "service-errors/v1",
                        content));
    }

    @Test
    void returnsNotFoundWithoutFabricatingContentForUnknownScenario() {
        RecentServiceErrorScenarioRepository repository = (tenantId, scenarioReference) -> Optional.empty();
        RecentServiceErrorsTool tool =
                new RecentServiceErrorsTool(repository, Clock.fixed(RETRIEVED_AT, ZoneOffset.UTC));

        RecentServiceErrorsResult result =
                tool.getRecentServiceErrors(TENANT_ID, "unknown-scenario", CORRELATION_ID, TOOL_CALL_ID);

        assertThat(result.status()).isEqualTo(EvidenceAvailabilityStatus.NOT_FOUND);
        assertThat(result.statusDetail())
                .isEqualTo("No synthetic service-error scenario matched the supplied reference.");
        assertThat(result.content()).isNull();
    }

    @Test
    void preservesAvailableEmptyPartialUnavailableTimedOutAndMalformedStates() {
        for (EvidenceAvailabilityStatus status : List.of(
                EvidenceAvailabilityStatus.AVAILABLE,
                EvidenceAvailabilityStatus.PARTIAL,
                EvidenceAvailabilityStatus.UNAVAILABLE,
                EvidenceAvailabilityStatus.TIMED_OUT,
                EvidenceAvailabilityStatus.MALFORMED)) {
            RecentServiceErrorsContent content =
                    status == EvidenceAvailabilityStatus.AVAILABLE || status == EvidenceAvailabilityStatus.PARTIAL
                            ? new RecentServiceErrorsContent(
                                    "payment-authorization",
                                    Instant.parse("2026-08-22T07:09:00Z"),
                                    Instant.parse("2026-08-22T07:14:00Z"),
                                    List.of())
                            : null;
            RecentServiceErrorScenarioRepository repository = (tenantId, scenarioReference) -> Optional.of(
                    new RecentServiceErrorScenario(status, status.name().toLowerCase(), content));
            RecentServiceErrorsTool tool =
                    new RecentServiceErrorsTool(repository, Clock.fixed(RETRIEVED_AT, ZoneOffset.UTC));

            RecentServiceErrorsResult result = tool.getRecentServiceErrors(
                    TENANT_ID, "scenario-" + status.name().toLowerCase(), CORRELATION_ID, TOOL_CALL_ID);

            assertThat(result.status()).isEqualTo(status);
            assertThat(result.content()).isEqualTo(content);
        }
    }

    @Test
    void rejectsInvalidToolArguments() {
        RecentServiceErrorsTool tool = new RecentServiceErrorsTool(
                (tenantId, scenarioReference) -> Optional.empty(), Clock.fixed(RETRIEVED_AT, ZoneOffset.UTC));

        assertThatThrownBy(() -> tool.getRecentServiceErrors(null, "scenario", CORRELATION_ID, TOOL_CALL_ID))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tool.getRecentServiceErrors(TENANT_ID, " ", CORRELATION_ID, TOOL_CALL_ID))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tool.getRecentServiceErrors(TENANT_ID, "x".repeat(121), CORRELATION_ID, TOOL_CALL_ID))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tool.getRecentServiceErrors(TENANT_ID, "scenario", null, TOOL_CALL_ID))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tool.getRecentServiceErrors(TENANT_ID, "scenario", CORRELATION_ID, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
