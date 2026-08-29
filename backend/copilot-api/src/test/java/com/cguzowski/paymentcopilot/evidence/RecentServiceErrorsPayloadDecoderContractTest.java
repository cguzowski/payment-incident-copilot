package com.cguzowski.paymentcopilot.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class RecentServiceErrorsPayloadDecoderContractTest {

    private static final UUID CORRELATION_ID = UUID.fromString("a5d978b5-34c7-42da-9076-22f8e5169315");
    private static final UUID TOOL_CALL_ID = UUID.fromString("21fdc56b-267a-4cb5-81b9-50f092e0ef35");
    private final RecentServiceErrorsPayloadDecoder decoder = new RecentServiceErrorsPayloadDecoder();

    @Test
    void decodesCanonicalAvailableAndUnavailableV1Fixtures() {
        EvidenceSourceResult available =
                decoder.decode(fixture("available-response.json"), CORRELATION_ID, TOOL_CALL_ID);
        EvidenceSourceResult unavailable =
                decoder.decode(fixture("unavailable-response.json"), CORRELATION_ID, TOOL_CALL_ID);

        assertThat(available.status()).isEqualTo(EvidenceCollectionStatus.AVAILABLE);
        assertThat(available.retrievedAt()).isEqualTo(Instant.parse("2026-08-28T10:00:00Z"));
        assertThat(available.content().serviceName()).isEqualTo("payment-authorization");
        assertThat(available.content().errors()).hasSize(2);
        assertThat(unavailable.status()).isEqualTo(EvidenceCollectionStatus.UNAVAILABLE);
        assertThat(unavailable.content()).isNull();
    }

    @Test
    void rejectsUnknownFieldsAndMismatchedIdentifiers() {
        Map<String, Object> unknown = fixture("available-response.json");
        unknown.put("futureField", true);
        Map<String, Object> mismatched = fixture("available-response.json");
        mismatched.put("toolCallId", UUID.randomUUID().toString());

        assertThatThrownBy(() -> decoder.decode(unknown, CORRELATION_ID, TOOL_CALL_ID))
                .isInstanceOf(InvalidEvidenceSourceResultException.class);
        assertThatThrownBy(() -> decoder.decode(mismatched, CORRELATION_ID, TOOL_CALL_ID))
                .isInstanceOf(InvalidEvidenceSourceResultException.class);
    }

    @Test
    void rejectsIncompatibleStatusContentAndBounds() {
        Map<String, Object> missingContent = fixture("available-response.json");
        missingContent.remove("content");

        Map<String, Object> unexpectedContent = fixture("unavailable-response.json");
        unexpectedContent.put("content", fixture("available-response.json").get("content"));

        Map<String, Object> excessiveCount = fixture("available-response.json");
        errors(excessiveCount).getFirst().put("count", 1_000_001);

        Map<String, Object> outsideWindow = fixture("available-response.json");
        errors(outsideWindow).getFirst().put("observedAt", "2026-08-22T07:15:00Z");

        Map<String, Object> tooManyErrors = fixture("available-response.json");
        List<Map<String, Object>> errors = new ArrayList<>();
        for (int index = 0; index < 101; index++) {
            errors.add(new LinkedHashMap<>(
                    errors(fixture("available-response.json")).getFirst()));
        }
        content(tooManyErrors).put("errors", errors);

        for (Map<String, Object> incompatible :
                List.of(missingContent, unexpectedContent, excessiveCount, outsideWindow, tooManyErrors)) {
            assertThatThrownBy(() -> decoder.decode(incompatible, CORRELATION_ID, TOOL_CALL_ID))
                    .isInstanceOf(InvalidEvidenceSourceResultException.class);
        }
    }

    private static Map<String, Object> fixture(String name) {
        JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
        try (InputStream input =
                new ClassPathResource("contracts/mcp/get-recent-service-errors/v1/fixtures/" + name).getInputStream()) {
            return mapper.readValue(input, new TypeReference<>() {});
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read MCP contract fixture " + name, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> content(Map<String, Object> result) {
        return (Map<String, Object>) result.get("content");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> errors(Map<String, Object> result) {
        return (List<Map<String, Object>>) content(result).get("errors");
    }
}
