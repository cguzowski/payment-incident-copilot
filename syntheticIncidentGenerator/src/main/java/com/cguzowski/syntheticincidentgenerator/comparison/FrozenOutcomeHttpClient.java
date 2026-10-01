package com.cguzowski.syntheticincidentgenerator.comparison;

import com.cguzowski.syntheticincidentgenerator.config.GeneratorProperties;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
final class FrozenOutcomeHttpClient implements FrozenOutcomeClient {
    private final JsonMapper mapper;
    private final GeneratorProperties properties;
    private final HttpClient http;

    FrozenOutcomeHttpClient(JsonMapper mapper, GeneratorProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
        this.http = HttpClient.newBuilder()
                .connectTimeout(properties.requestTimeout())
                .build();
    }

    @Override
    public FrozenOutcome find(UUID incidentId, String terminalStatus) {
        JsonNode incident = read("/api/incidents/" + incidentId);
        require(incidentId.equals(uuid(incident, "incidentId"))
                && terminalStatus.equals(incident.path("status").asText()));
        UUID investigationId = uuid(incident, "activeInvestigationId");
        String prefix = "/api/investigations/" + investigationId;
        JsonNode investigation = read(prefix);
        require(investigationId.equals(uuid(investigation, "investigationId"))
                && incidentId.equals(uuid(investigation, "incidentId")));
        JsonNode decisions = read(prefix + "/decisions");
        require(decisions.isArray() && decisions.size() == 1);
        JsonNode decision = decisions.get(0);
        require(investigationId.equals(uuid(decision, "investigationId"))
                && terminalStatus.equals(decision.path("outcome").asText())
                && terminalStatus.equals(decision.path("incidentStatus").asText()));
        UUID reportId = uuid(decision, "reportAttemptId");
        UUID decisionId = uuid(decision, "decisionId");
        JsonNode attempt = unique(read(prefix + "/reports"), "attemptId", reportId);
        require(investigationId.equals(uuid(attempt, "investigationId"))
                && "AVAILABLE".equals(attempt.path("status").asText()));
        ComparisonRubric.validateReport(attempt.get("report"));
        Set<UUID> evidenceIds = new HashSet<>();
        evidenceIds.add(uuid(attempt, "latestEvidenceId"));
        if (attempt.hasNonNull("applicableEvidenceId")) evidenceIds.add(uuid(attempt, "applicableEvidenceId"));
        JsonNode history = read(prefix + "/evidence-collections");
        var evidence = mapper.createArrayNode();
        for (UUID id : evidenceIds.stream().sorted().toList()) evidence.add(unique(history, "evidenceId", id));
        return new FrozenOutcome(
                investigationId,
                reportId,
                decisionId,
                terminalStatus,
                attempt.get("report"),
                evidence,
                decision,
                attempt);
    }

    private JsonNode read(String path) {
        try {
            var request = HttpRequest.newBuilder(properties.copilotApiBaseUrl().resolve(path))
                    .timeout(properties.requestTimeout())
                    .header("Accept", "application/json")
                    .header("X-Synthetic-Tenant-Id", properties.tenantId().toString())
                    .GET()
                    .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            require(response.statusCode() == 200 && response.body().length() <= 2_000_000);
            return mapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Comparison inputs unavailable", exception);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Comparison inputs unavailable", exception);
        }
    }

    private static JsonNode unique(JsonNode history, String field, UUID id) {
        require(history != null && history.isArray());
        JsonNode found = null;
        for (JsonNode entry : history) {
            if (id.toString().equals(entry.path(field).asText())) {
                require(found == null);
                found = entry;
            }
        }
        require(found != null);
        return found;
    }

    private static UUID uuid(JsonNode node, String field) {
        return UUID.fromString(node.path(field).asText());
    }

    private static void require(boolean condition) {
        if (!condition) throw new IllegalArgumentException("Comparison input binding unavailable");
    }
}
