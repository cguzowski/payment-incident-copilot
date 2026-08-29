package com.cguzowski.paymentcopilot.evidence;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RecentServiceErrorsPayloadDecoder {

    static final String SOURCE_SYSTEM = "synthetic-observability";
    static final String SOURCE_TOOL = "getRecentServiceErrors";
    static final String CONTENT_SCHEMA_VERSION = "service-errors/v1";

    private static final Set<String> REQUIRED_RESULT_FIELDS = Set.of(
            "sourceSystem",
            "sourceTool",
            "retrievedAt",
            "correlationId",
            "toolCallId",
            "status",
            "contentSchemaVersion");
    private static final Set<String> OPTIONAL_RESULT_FIELDS = Set.of("statusDetail", "content");
    private static final Set<String> CONTENT_FIELDS = Set.of("serviceName", "observedFrom", "observedTo", "errors");
    private static final Set<String> ERROR_FIELDS = Set.of("sourceEventId", "observedAt", "errorCode", "count");
    private static final int MAX_ERRORS = 100;
    private static final int MAX_AGGREGATE_COUNT = 1_000_000;

    EvidenceSourceResult decode(Map<String, Object> result, UUID expectedCorrelationId, UUID expectedToolCallId) {
        if (result == null) {
            throw new InvalidEvidenceSourceResultException();
        }
        requireExactFields(result, REQUIRED_RESULT_FIELDS, OPTIONAL_RESULT_FIELDS);
        String sourceSystem = requiredString(result, "sourceSystem", 120);
        String sourceTool = requiredString(result, "sourceTool", 120);
        String contentSchemaVersion = requiredString(result, "contentSchemaVersion", 80);
        Instant retrievedAt = requiredInstant(result, "retrievedAt");
        UUID correlationId = requiredUuid(result, "correlationId");
        UUID toolCallId = requiredUuid(result, "toolCallId");
        EvidenceCollectionStatus status = requiredTerminalStatus(result);
        String statusDetail = optionalString(result, "statusDetail", 500);

        if (!SOURCE_SYSTEM.equals(sourceSystem)
                || !SOURCE_TOOL.equals(sourceTool)
                || !CONTENT_SCHEMA_VERSION.equals(contentSchemaVersion)
                || !expectedCorrelationId.equals(correlationId)
                || !expectedToolCallId.equals(toolCallId)) {
            throw new InvalidEvidenceSourceResultException();
        }

        ServiceErrorEvidenceContent content = parseContent(result.get("content"));
        boolean contentRequired =
                status == EvidenceCollectionStatus.AVAILABLE || status == EvidenceCollectionStatus.PARTIAL;
        if (contentRequired != (content != null)) {
            throw new InvalidEvidenceSourceResultException();
        }

        return new EvidenceSourceResult(
                sourceSystem,
                sourceTool,
                retrievedAt,
                correlationId,
                toolCallId,
                status,
                statusDetail,
                contentSchemaVersion,
                content);
    }

    private static ServiceErrorEvidenceContent parseContent(Object rawContent) {
        if (rawContent == null) {
            return null;
        }
        if (!(rawContent instanceof Map<?, ?> untypedContent)) {
            throw new InvalidEvidenceSourceResultException();
        }
        Map<String, Object> content = stringKeyedMap(untypedContent);
        requireExactFields(content, CONTENT_FIELDS, Set.of());
        String serviceName = requiredString(content, "serviceName", 120);
        Instant observedFrom = requiredInstant(content, "observedFrom");
        Instant observedTo = requiredInstant(content, "observedTo");
        if (observedFrom.isAfter(observedTo)) {
            throw new InvalidEvidenceSourceResultException();
        }
        Object rawErrors = content.get("errors");
        if (!(rawErrors instanceof List<?> errors) || errors.size() > MAX_ERRORS) {
            throw new InvalidEvidenceSourceResultException();
        }
        List<ServiceErrorObservation> observations = new ArrayList<>(errors.size());
        for (Object rawError : errors) {
            if (!(rawError instanceof Map<?, ?> untypedError)) {
                throw new InvalidEvidenceSourceResultException();
            }
            Map<String, Object> error = stringKeyedMap(untypedError);
            requireExactFields(error, ERROR_FIELDS, Set.of());
            Instant observedAt = requiredInstant(error, "observedAt");
            if (observedAt.isBefore(observedFrom) || observedAt.isAfter(observedTo)) {
                throw new InvalidEvidenceSourceResultException();
            }
            observations.add(new ServiceErrorObservation(
                    requiredString(error, "sourceEventId", 120),
                    observedAt,
                    requiredString(error, "errorCode", 120),
                    requiredCount(error)));
        }
        return new ServiceErrorEvidenceContent(serviceName, observedFrom, observedTo, observations);
    }

    private static int requiredCount(Map<String, Object> error) {
        Object rawCount = error.get("count");
        if (!(rawCount instanceof Byte
                        || rawCount instanceof Short
                        || rawCount instanceof Integer
                        || rawCount instanceof Long)
                || ((Number) rawCount).longValue() < 1
                || ((Number) rawCount).longValue() > MAX_AGGREGATE_COUNT) {
            throw new InvalidEvidenceSourceResultException();
        }
        return ((Number) rawCount).intValue();
    }

    private static EvidenceCollectionStatus requiredTerminalStatus(Map<String, Object> result) {
        String rawStatus = requiredString(result, "status", 30);
        try {
            EvidenceCollectionStatus status = EvidenceCollectionStatus.valueOf(rawStatus);
            if (!status.isTerminal()) {
                throw new InvalidEvidenceSourceResultException();
            }
            return status;
        } catch (IllegalArgumentException exception) {
            throw new InvalidEvidenceSourceResultException();
        }
    }

    private static UUID requiredUuid(Map<String, Object> values, String field) {
        try {
            return UUID.fromString(requiredString(values, field, 36));
        } catch (IllegalArgumentException exception) {
            throw new InvalidEvidenceSourceResultException();
        }
    }

    private static Instant requiredInstant(Map<String, Object> values, String field) {
        try {
            return Instant.parse(requiredString(values, field, 40));
        } catch (DateTimeException exception) {
            throw new InvalidEvidenceSourceResultException();
        }
    }

    private static String requiredString(Map<String, Object> values, String field, int maximumLength) {
        Object value = values.get(field);
        if (!(value instanceof String stringValue)
                || stringValue.isBlank()
                || stringValue.length() > maximumLength
                || containsControlCharacter(stringValue)) {
            throw new InvalidEvidenceSourceResultException();
        }
        return stringValue;
    }

    private static String optionalString(Map<String, Object> values, String field, int maximumLength) {
        if (!values.containsKey(field) || values.get(field) == null) {
            return null;
        }
        return requiredString(values, field, maximumLength);
    }

    private static boolean containsControlCharacter(String value) {
        return value.codePoints().anyMatch(Character::isISOControl);
    }

    private static void requireExactFields(Map<String, Object> values, Set<String> required, Set<String> optional) {
        if (!values.keySet().containsAll(required) || !union(required, optional).containsAll(values.keySet())) {
            throw new InvalidEvidenceSourceResultException();
        }
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        var union = new java.util.HashSet<>(first);
        union.addAll(second);
        return union;
    }

    private static Map<String, Object> stringKeyedMap(Map<?, ?> values) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new InvalidEvidenceSourceResultException();
            }
            result.put(key, entry.getValue());
        }
        return result;
    }
}
