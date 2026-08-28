package com.cguzowski.paymentcopilot.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecentServiceErrorsResult(
        String sourceSystem,
        String sourceTool,
        Instant retrievedAt,
        UUID correlationId,
        UUID toolCallId,
        EvidenceAvailabilityStatus status,
        @JsonProperty(required = false) @Nullable String statusDetail,
        String contentSchemaVersion,
        @JsonProperty(required = false) @Nullable RecentServiceErrorsContent content) {
}
