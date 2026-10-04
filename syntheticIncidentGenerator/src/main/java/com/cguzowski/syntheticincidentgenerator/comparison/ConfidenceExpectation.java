package com.cguzowski.syntheticincidentgenerator.comparison;

import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioTruth;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

record ConfidenceExpectation(String level, String reason) {
    static final String VERSION = "confidence-evidence/v1";
    private static final Pattern SIGNAL = Pattern.compile("\\b[A-Z][A-Z0-9]*_[A-Z0-9_]+\\b");
    private static final Set<String> SYMPTOMS = Set.of(
            "GATEWAY_TIMEOUT",
            "REQUEST_DEADLINE_EXCEEDED",
            "AUTHORIZATION_STATE_LOOKUP_TIMEOUT",
            "TLS_HANDSHAKE_FAILED",
            "RESPONSE_MAPPING_FAILED",
            "HSM_SIGNING_TIMEOUT",
            "DNS_RESOLUTION_FAILED");
    private static final Set<String> MECHANISMS = Set.of(
            "UPSTREAM_CONNECTION_RESET",
            "UPSTREAM_RATE_LIMITED",
            "DATABASE_CONNECTION_POOL_EXHAUSTED",
            "ISSUER_DO_NOT_HONOR_SURGE",
            "RETRY_BUDGET_EXHAUSTED",
            "AUTHORIZATION_CPU_SATURATION",
            "ROUTE_CONFIGURATION_NOT_FOUND",
            "GATEWAY_MAINTENANCE_REJECTION",
            "MERCHANT_PROFILE_INVALID",
            "FRAUD_RULE_REJECTION_SURGE",
            "NETWORK_PACKET_LOSS",
            "GATEWAY_CREDENTIAL_REJECTED",
            "CACHE_STAMPEDE",
            "TLS_CERTIFICATE_EXPIRED",
            "CLOCK_SKEW_REJECTED",
            "REQUEST_SIGNATURE_INVALID",
            "FEATURE_FLAG_ROUTE_MISMATCH",
            "GATEWAY_RESPONSE_SCHEMA_MISMATCH",
            "REGIONAL_FAILOVER_LOOP",
            "DATABASE_LOCK_TIMEOUT",
            "AUTHORIZATION_STATE_UPDATE_FAILED",
            "BIN_ROUTE_TABLE_STALE",
            "ISSUER_ROUTE_NOT_FOUND",
            "MALFORMED_ISSUER_RESPONSE",
            "TOKEN_VAULT_TIMEOUT",
            "CONFIG_VERSION_SPLIT_BRAIN",
            "ROUTE_DECISION_DIVERGENCE",
            "BGP_ROUTE_UNREACHABLE",
            "OCSP_RESPONDER_UNAVAILABLE",
            "CERTIFICATE_STATUS_CHECK_FAILED",
            "HSM_QUORUM_LOST",
            "CALENDAR_RULE_EVALUATION_FAILED",
            "RECURRING_AUTHORIZATION_REJECTED",
            "SECURE_RANDOM_SOURCE_BLOCKED",
            "REQUEST_SIGNATURE_TIMEOUT",
            "UNICODE_NORMALIZATION_FAILURE",
            "MERCHANT_PROFILE_PARSE_FAILED",
            "DNS_SECURITY_POLICY_BLOCK",
            "HSM_FIRMWARE_PROTOCOL_MISMATCH",
            "HSM_SIGNING_FAILED",
            "CIRCUIT_BREAKER_SYNC_STORM",
            "UPSTREAM_CALL_REJECTED",
            "AUTHORIZATION_DEADLINE_EXCEEDED",
            "GATEWAY_RESPONSE_LATE",
            "CAPTURE_SUBMISSION_FAILED",
            "CAPTURE_UPSTREAM_UNAVAILABLE",
            "REFUND_DEPENDENCY_TIMEOUT",
            "REFUND_SUBMISSION_FAILED",
            "SETTLEMENT_ACK_MISSING",
            "SETTLEMENT_BATCH_OVERDUE",
            "WEBHOOK_DELIVERY_EXHAUSTED",
            "WEBHOOK_ENDPOINT_TIMEOUT",
            "RECONCILIATION_RECORD_MISSING",
            "RECONCILIATION_TOTAL_MISMATCH");
    private static final List<Signature> DIRECT = List.of(
            new Signature("TLS_CERTIFICATE_EXPIRED", "TLS_HANDSHAKE_FAILED"),
            new Signature("HSM_QUORUM_LOST", "HSM_SIGNING_TIMEOUT"),
            new Signature("HSM_FIRMWARE_PROTOCOL_MISMATCH", "HSM_SIGNING_FAILED"));

    // Read only the attempt's latest evidence binding; never its report or actual level.
    static ConfidenceExpectation evaluate(ScenarioTruth truth, JsonNode evidence, JsonNode attempt) {
        String latestId = attempt.path("latestEvidenceId").asText();
        if (latestId.isBlank() || !evidence.isArray()) {
            throw new IllegalArgumentException("Missing latest evidence binding");
        }
        JsonNode latest = null;
        for (JsonNode entry : evidence) {
            if (latestId.equals(entry.path("evidenceId").asText())) {
                if (latest != null) throw new IllegalArgumentException("Duplicate latest evidence binding");
                latest = entry;
            }
        }
        if (latest == null) throw new IllegalArgumentException("Missing latest evidence snapshot");
        if (!"AVAILABLE".equals(latest.path("status").asText())) {
            return low("Latest evidence is degraded or unavailable; earlier observations cannot restore support.");
        }
        JsonNode content = latest.path("content");
        JsonNode errors = content.path("errors");
        if (!"service-errors/v1".equals(latest.path("contentSchemaVersion").asText())
                || content.path("serviceName").asText().isBlank()
                || !errors.isArray()
                || errors.isEmpty()) {
            return low("No usable service-error observations in the latest supported snapshot.");
        }
        Map<String, Observation> observations = new HashMap<>();
        boolean repeated = false;
        try {
            Instant from = Instant.parse(content.path("observedFrom").asText());
            Instant to = Instant.parse(content.path("observedTo").asText());
            if (!from.isBefore(to)) return low("The observation window is invalid.");
            for (JsonNode error : errors) {
                String code = error.path("errorCode").asText();
                JsonNode count = error.path("count");
                Instant at = Instant.parse(error.path("observedAt").asText());
                if (!SIGNAL.matcher(code).matches()
                        || !count.isIntegralNumber()
                        || !count.canConvertToLong()
                        || count.asLong() <= 0
                        || at.isBefore(from)
                        || at.isAfter(to)) {
                    return low("Invalid counts, signals or observations outside the bounded window.");
                }
                repeated |= observations.put(code, new Observation(count.asLong(), at)) != null;
            }
        } catch (java.time.DateTimeException exception) {
            return low("Missing or invalid observation timestamps.");
        }
        Set<String> required = new HashSet<>();
        for (String requirement : truth.requiredEvidence()) {
            SIGNAL.matcher(requirement).results().forEach(match -> required.add(match.group()));
        }
        if (!observations.keySet().containsAll(required)) {
            var missing = new java.util.TreeSet<>(required);
            missing.removeAll(observations.keySet());
            return low("Required observed signals are missing: " + String.join(", ", missing) + ".");
        }
        if (SYMPTOMS.containsAll(observations.keySet())) {
            return low("Only nonspecific failure symptoms are supplied; no diagnostic mechanism is established.");
        }
        if (observations.keySet().stream().anyMatch(code -> !SYMPTOMS.contains(code) && !MECHANISMS.contains(code))) {
            return low("Unclassified observed signals require review before a mechanism can be supported.");
        }
        if (DIRECT.stream()
                        .filter(signature -> observations.containsKey(signature.diagnosis()))
                        .count()
                > 1) {
            return low(
                    "Competing direct diagnoses are supplied; the aggregate snapshot cannot resolve their relationship.");
        }
        for (Signature signature : DIRECT) {
            Set<String> pair = Set.of(signature.diagnosis(), signature.failure());
            if (!repeated && required.equals(pair) && observations.keySet().equals(pair)) {
                var diagnosis = observations.get(signature.diagnosis());
                var failure = observations.get(signature.failure());
                if (diagnosis.count() == failure.count() && !diagnosis.at().isAfter(failure.at())) {
                    return new ConfidenceExpectation(
                            "HIGH",
                            "Direct diagnostic signature " + signature.diagnosis()
                                    + " with " + signature.failure()
                                    + ": matching positive counts, diagnosis no later than failure, one valid window and no additional signals."
                                    + " Supports the narrow mechanism without requiring independent confirmation; deeper cause and final outcome remain unconfirmed.");
                }
            }
        }
        return new ConfidenceExpectation(
                "MEDIUM",
                "Bounded observations support a candidate mechanism, but aggregate counts do not establish affected-path linkage, traffic shape or deeper causation. No complete exceptional direct signature is supplied.");
    }

    private static ConfidenceExpectation low(String reason) {
        return new ConfidenceExpectation("LOW", reason);
    }

    private record Observation(long count, Instant at) {}

    private record Signature(String diagnosis, String failure) {}
}
