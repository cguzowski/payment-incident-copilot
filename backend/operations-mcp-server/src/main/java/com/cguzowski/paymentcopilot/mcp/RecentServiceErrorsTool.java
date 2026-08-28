package com.cguzowski.paymentcopilot.mcp;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

@Service
public class RecentServiceErrorsTool {

    static final String SOURCE_SYSTEM = "synthetic-observability";
    static final String TOOL_NAME = "getRecentServiceErrors";
    static final String CONTENT_SCHEMA_VERSION = "service-errors/v1";

    private final RecentServiceErrorScenarioRepository repository;
    private final Clock clock;

    RecentServiceErrorsTool(RecentServiceErrorScenarioRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @McpTool(
            name = TOOL_NAME,
            description = "Returns deterministic recent payment-authorization service errors for a synthetic scenario.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public RecentServiceErrorsResult getRecentServiceErrors(
            @McpToolParam(description = "Synthetic tenant UUID") UUID tenantId,
            @McpToolParam(description = "Opaque synthetic alert scenario reference") String scenarioReference,
            @McpToolParam(description = "Investigation correlation UUID") UUID correlationId,
            @McpToolParam(description = "Unique tool-call UUID") UUID toolCallId) {
        validate(tenantId, scenarioReference, correlationId, toolCallId);
        Instant retrievedAt = Instant.now(clock);
        return repository.find(tenantId, scenarioReference)
                .map(scenario -> result(retrievedAt, correlationId, toolCallId, scenario))
                .orElseGet(() -> new RecentServiceErrorsResult(
                        SOURCE_SYSTEM,
                        TOOL_NAME,
                        retrievedAt,
                        correlationId,
                        toolCallId,
                        EvidenceAvailabilityStatus.NOT_FOUND,
                        "No synthetic service-error scenario matched the supplied reference.",
                        CONTENT_SCHEMA_VERSION,
                        null));
    }

    private static void validate(
            UUID tenantId,
            String scenarioReference,
            UUID correlationId,
            UUID toolCallId) {
        if (tenantId == null
                || scenarioReference == null
                || scenarioReference.isBlank()
                || scenarioReference.length() > 120
                || correlationId == null
                || toolCallId == null) {
            throw new IllegalArgumentException("Invalid recent-service-errors tool arguments.");
        }
    }

    private static RecentServiceErrorsResult result(
            Instant retrievedAt,
            UUID correlationId,
            UUID toolCallId,
            RecentServiceErrorScenario scenario) {
        return new RecentServiceErrorsResult(
                SOURCE_SYSTEM,
                TOOL_NAME,
                retrievedAt,
                correlationId,
                toolCallId,
                scenario.status(),
                scenario.statusDetail(),
                CONTENT_SCHEMA_VERSION,
                scenario.content());
    }
}
