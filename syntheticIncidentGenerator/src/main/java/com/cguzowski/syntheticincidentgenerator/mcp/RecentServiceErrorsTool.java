package com.cguzowski.syntheticincidentgenerator.mcp;

import com.cguzowski.syntheticincidentgenerator.config.GeneratorProperties;
import com.cguzowski.syntheticincidentgenerator.scenario.AlertReferenceCodec;
import com.cguzowski.syntheticincidentgenerator.scenario.DecodedAlertReference;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioCatalog;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioDefinition;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioEvidence;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecentServiceErrorsTool {

    static final String SOURCE_SYSTEM = "synthetic-observability";
    static final String TOOL_NAME = "getRecentServiceErrors";
    static final String CONTENT_SCHEMA_VERSION = "service-errors/v1";

    private final ScenarioCatalog catalog;
    private final AlertReferenceCodec referenceCodec;
    private final UUID tenantId;
    private final Clock clock;

    @Autowired
    public RecentServiceErrorsTool(
            ScenarioCatalog catalog, AlertReferenceCodec referenceCodec, GeneratorProperties properties, Clock clock) {
        this(catalog, referenceCodec, properties.tenantId(), clock);
    }

    RecentServiceErrorsTool(ScenarioCatalog catalog, AlertReferenceCodec referenceCodec, UUID tenantId, Clock clock) {
        this.catalog = catalog;
        this.referenceCodec = referenceCodec;
        this.tenantId = tenantId;
        this.clock = clock;
    }

    @McpTool(
            name = TOOL_NAME,
            description = "Returns deterministic recent payment-authorization service errors for a synthetic scenario.",
            generateOutputSchema = true,
            annotations =
                    @McpTool.McpAnnotations(
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
        if (!this.tenantId.equals(tenantId)) {
            return notFound(retrievedAt, correlationId, toolCallId);
        }
        return referenceCodec
                .decode(scenarioReference)
                .flatMap(reference -> catalog.findByCode(reference.scenarioCode())
                        .map(scenario ->
                                result(scenarioReference, reference, scenario, retrievedAt, correlationId, toolCallId)))
                .orElseGet(() -> notFound(retrievedAt, correlationId, toolCallId));
    }

    private static RecentServiceErrorsResult result(
            String scenarioReference,
            DecodedAlertReference reference,
            ScenarioDefinition scenario,
            Instant retrievedAt,
            UUID correlationId,
            UUID toolCallId) {
        ScenarioEvidence evidence = scenario.evidence();
        EvidenceAvailabilityStatus status =
                EvidenceAvailabilityStatus.valueOf(evidence.availability().name());
        RecentServiceErrorsContent content =
                switch (status) {
                    case AVAILABLE, PARTIAL -> content(scenarioReference, reference.detectedAt(), evidence);
                    default -> null;
                };
        return new RecentServiceErrorsResult(
                SOURCE_SYSTEM,
                TOOL_NAME,
                retrievedAt,
                correlationId,
                toolCallId,
                status,
                evidence.statusDetail(),
                CONTENT_SCHEMA_VERSION,
                content);
    }

    private static RecentServiceErrorsContent content(
            String scenarioReference, Instant detectedAt, ScenarioEvidence evidence) {
        List<RecentServiceErrorObservation> observations = IntStream.range(
                        0, evidence.errors().size())
                .mapToObj(index -> {
                    var error = evidence.errors().get(index);
                    return new RecentServiceErrorObservation(
                            scenarioReference + "-e" + String.format("%02d", index + 1),
                            detectedAt.minusSeconds(error.secondsBeforeDetection()),
                            error.errorCode(),
                            error.count());
                })
                .toList();
        return new RecentServiceErrorsContent(
                evidence.serviceName(), detectedAt.minusSeconds(300), detectedAt, observations);
    }

    private static RecentServiceErrorsResult notFound(Instant retrievedAt, UUID correlationId, UUID toolCallId) {
        return new RecentServiceErrorsResult(
                SOURCE_SYSTEM,
                TOOL_NAME,
                retrievedAt,
                correlationId,
                toolCallId,
                EvidenceAvailabilityStatus.NOT_FOUND,
                "No synthetic service-error scenario matched the supplied reference.",
                CONTENT_SCHEMA_VERSION,
                null);
    }

    private static void validate(UUID tenantId, String scenarioReference, UUID correlationId, UUID toolCallId) {
        if (tenantId == null
                || scenarioReference == null
                || scenarioReference.isBlank()
                || scenarioReference.length() > 120
                || correlationId == null
                || toolCallId == null) {
            throw new IllegalArgumentException("Invalid recent-service-errors tool arguments.");
        }
    }
}
