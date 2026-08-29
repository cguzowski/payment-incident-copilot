package com.cguzowski.paymentcopilot.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import com.cguzowski.paymentcopilot.incident.EvidenceCollectionContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class McpServiceErrorEvidenceGatewayTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INVESTIGATION_ID = UUID.fromString("a012c9cb-85a6-4d77-9703-3b53377b56c3");
    private static final UUID CORRELATION_ID = UUID.fromString("a5d978b5-34c7-42da-9076-22f8e5169315");
    private static final UUID TOOL_CALL_ID = UUID.fromString("21fdc56b-267a-4cb5-81b9-50f092e0ef35");
    private static final Instant RETRIEVED_AT = Instant.parse("2026-08-28T10:00:01Z");
    private static final EvidenceCollectionContext CONTEXT =
            new EvidenceCollectionContext(TENANT_ID, INVESTIGATION_ID, CORRELATION_ID, "alert-auth-decline-001");

    @Test
    void mapsEveryToolOutcomeWithoutFabrication() {
        for (EvidenceCollectionStatus status : List.of(
                EvidenceCollectionStatus.AVAILABLE,
                EvidenceCollectionStatus.PARTIAL,
                EvidenceCollectionStatus.NOT_FOUND,
                EvidenceCollectionStatus.UNAVAILABLE,
                EvidenceCollectionStatus.TIMED_OUT,
                EvidenceCollectionStatus.MALFORMED)) {
            Map<String, Object> raw = validResult(status);

            EvidenceSourceResult result = gateway(raw).collect(CONTEXT, TOOL_CALL_ID);

            assertThat(result.status()).isEqualTo(status);
            assertThat(result.content())
                    .isEqualTo(
                            status == EvidenceCollectionStatus.AVAILABLE || status == EvidenceCollectionStatus.PARTIAL
                                    ? expectedContent()
                                    : null);
        }
    }

    @Test
    void preservesAvailableEmptyEvidenceAsSuccessfulObservation() {
        Map<String, Object> raw = validResult(EvidenceCollectionStatus.AVAILABLE);
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) raw.get("content");
        content.put("errors", List.of());

        EvidenceSourceResult result = gateway(raw).collect(CONTEXT, TOOL_CALL_ID);

        assertThat(result.status()).isEqualTo(EvidenceCollectionStatus.AVAILABLE);
        assertThat(result.content().errors()).isEmpty();
    }

    @Test
    void marksMismatchedOrInvalidToolResultsMalformed() {
        Map<String, Object> mismatchedCorrelation = validResult(EvidenceCollectionStatus.AVAILABLE);
        mismatchedCorrelation.put("correlationId", UUID.randomUUID().toString());
        Map<String, Object> unknownField = validResult(EvidenceCollectionStatus.AVAILABLE);
        unknownField.put("rawPayload", "must not be accepted");
        Map<String, Object> invalidCount = validResult(EvidenceCollectionStatus.AVAILABLE);
        errors(invalidCount).getFirst().put("count", -1);
        Map<String, Object> excessiveContent = validResult(EvidenceCollectionStatus.AVAILABLE);
        List<Map<String, Object>> tooManyErrors = new ArrayList<>();
        for (int index = 0; index < 101; index++) {
            tooManyErrors.add(new LinkedHashMap<>(
                    errors(validResult(EvidenceCollectionStatus.AVAILABLE)).getFirst()));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> excessiveBody = (Map<String, Object>) excessiveContent.get("content");
        excessiveBody.put("errors", tooManyErrors);

        for (Map<String, Object> invalid :
                List.of(mismatchedCorrelation, unknownField, invalidCount, excessiveContent)) {
            EvidenceSourceResult result = gateway(invalid).collect(CONTEXT, TOOL_CALL_ID);

            assertThat(result.status()).isEqualTo(EvidenceCollectionStatus.MALFORMED);
            assertThat(result.content()).isNull();
            assertThat(result.statusDetail()).isEqualTo("Tool result failed validation.");
        }
    }

    @Test
    void mapsTimeoutAndUnavailableTransportFailuresWithoutPayloads() {
        EvidenceSourceResult timedOut = gateway((context, toolCallId) -> {
                    throw new EvidenceSourceTimedOutException();
                })
                .collect(CONTEXT, TOOL_CALL_ID);
        EvidenceSourceResult unavailable = gateway((context, toolCallId) -> {
                    throw new EvidenceSourceUnavailableException();
                })
                .collect(CONTEXT, TOOL_CALL_ID);

        assertThat(timedOut.status()).isEqualTo(EvidenceCollectionStatus.TIMED_OUT);
        assertThat(timedOut.retrievedAt()).isNull();
        assertThat(timedOut.content()).isNull();
        assertThat(unavailable.status()).isEqualTo(EvidenceCollectionStatus.UNAVAILABLE);
        assertThat(unavailable.retrievedAt()).isNull();
        assertThat(unavailable.content()).isNull();
    }

    private static McpServiceErrorEvidenceGateway gateway(Map<String, Object> raw) {
        return gateway((context, toolCallId) -> raw);
    }

    private static McpServiceErrorEvidenceGateway gateway(RecentServiceErrorsClient client) {
        return new McpServiceErrorEvidenceGateway(client, new RecentServiceErrorsPayloadDecoder());
    }

    private static Map<String, Object> validResult(EvidenceCollectionStatus status) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sourceSystem", "synthetic-observability");
        result.put("sourceTool", "getRecentServiceErrors");
        result.put("retrievedAt", RETRIEVED_AT.toString());
        result.put("correlationId", CORRELATION_ID.toString());
        result.put("toolCallId", TOOL_CALL_ID.toString());
        result.put("status", status.name());
        result.put("contentSchemaVersion", "service-errors/v1");
        if (status == EvidenceCollectionStatus.AVAILABLE || status == EvidenceCollectionStatus.PARTIAL) {
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("serviceName", "payment-authorization-service");
            content.put("observedFrom", "2026-08-28T09:55:00Z");
            content.put("observedTo", "2026-08-28T10:00:00Z");
            content.put(
                    "errors",
                    List.of(new LinkedHashMap<>(Map.of(
                            "sourceEventId", "service-error-001",
                            "observedAt", "2026-08-28T09:58:00Z",
                            "errorCode", "UPSTREAM_TIMEOUT",
                            "count", 14))));
            result.put("content", content);
        } else {
            result.put("statusDetail", "Synthetic source did not return evidence.");
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> errors(Map<String, Object> result) {
        return (List<Map<String, Object>>) ((Map<String, Object>) result.get("content")).get("errors");
    }

    private static ServiceErrorEvidenceContent expectedContent() {
        return new ServiceErrorEvidenceContent(
                "payment-authorization-service",
                Instant.parse("2026-08-28T09:55:00Z"),
                Instant.parse("2026-08-28T10:00:00Z"),
                List.of(new ServiceErrorObservation(
                        "service-error-001", Instant.parse("2026-08-28T09:58:00Z"), "UPSTREAM_TIMEOUT", 14)));
    }
}
