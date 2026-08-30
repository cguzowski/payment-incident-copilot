package com.cguzowski.syntheticincidentgenerator.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.cguzowski.syntheticincidentgenerator.scenario.AlertReferenceCodec;
import com.cguzowski.syntheticincidentgenerator.scenario.ClasspathScenarioCatalog;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class RecentServiceErrorsToolTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID CORRELATION_ID = UUID.fromString("e147fdc4-2bf8-4708-bbb1-f19556292ed7");
    private static final UUID TOOL_CALL_ID = UUID.fromString("31b783f7-adea-46e1-a479-fe33adc1766d");
    private static final Instant DETECTED_AT = Instant.parse("2026-08-31T09:15:30Z");
    private static final Instant RETRIEVED_AT = Instant.parse("2026-08-31T09:20:00Z");

    @Test
    void reconstructsDeterministicTimeAlignedEvidenceFromOpaqueAlertReference() {
        RecentServiceErrorsTool tool = tool();

        RecentServiceErrorsResult result = tool.getRecentServiceErrors(
                TENANT_ID, "sig-v1-S001-1788167730-1234567890ab", CORRELATION_ID, TOOL_CALL_ID);

        assertThat(result.status()).isEqualTo(EvidenceAvailabilityStatus.AVAILABLE);
        assertThat(result.retrievedAt()).isEqualTo(RETRIEVED_AT);
        assertThat(result.correlationId()).isEqualTo(CORRELATION_ID);
        assertThat(result.toolCallId()).isEqualTo(TOOL_CALL_ID);
        assertThat(result.content().serviceName()).isEqualTo("payment-authorization");
        assertThat(result.content().observedFrom()).isEqualTo(DETECTED_AT.minusSeconds(300));
        assertThat(result.content().observedTo()).isEqualTo(DETECTED_AT);
        assertThat(result.content().errors())
                .containsExactly(
                        new RecentServiceErrorObservation(
                                "sig-v1-S001-1788167730-1234567890ab-e01",
                                DETECTED_AT.minusSeconds(110),
                                "GATEWAY_TIMEOUT",
                                47),
                        new RecentServiceErrorObservation(
                                "sig-v1-S001-1788167730-1234567890ab-e02",
                                DETECTED_AT.minusSeconds(72),
                                "UPSTREAM_CONNECTION_RESET",
                                19));
    }

    @Test
    void preservesUnavailableAndUnknownScenarioOutcomesWithoutInventingContent() {
        RecentServiceErrorsTool tool = tool();

        RecentServiceErrorsResult unavailable = tool.getRecentServiceErrors(
                TENANT_ID, "sig-v1-S211-1788167730-1234567890ab", CORRELATION_ID, TOOL_CALL_ID);
        RecentServiceErrorsResult unknown = tool.getRecentServiceErrors(
                TENANT_ID, "sig-v1-S999-1788167730-1234567890ab", CORRELATION_ID, TOOL_CALL_ID);
        RecentServiceErrorsResult wrongTenant = tool.getRecentServiceErrors(
                UUID.fromString("6d482a65-8cf0-4d23-9f28-d753e9b8b042"),
                "sig-v1-S001-1788167730-1234567890ab",
                CORRELATION_ID,
                TOOL_CALL_ID);

        assertThat(unavailable.status()).isEqualTo(EvidenceAvailabilityStatus.UNAVAILABLE);
        assertThat(unavailable.content()).isNull();
        assertThat(unavailable.statusDetail()).contains("unavailable");
        assertThat(unknown.status()).isEqualTo(EvidenceAvailabilityStatus.NOT_FOUND);
        assertThat(unknown.content()).isNull();
        assertThat(wrongTenant.status()).isEqualTo(EvidenceAvailabilityStatus.NOT_FOUND);
        assertThat(wrongTenant.content()).isNull();
    }

    private static RecentServiceErrorsTool tool() {
        return new RecentServiceErrorsTool(
                new ClasspathScenarioCatalog(
                        JsonMapper.builder().findAndAddModules().build()),
                new AlertReferenceCodec(),
                TENANT_ID,
                Clock.fixed(RETRIEVED_AT, ZoneOffset.UTC));
    }
}
