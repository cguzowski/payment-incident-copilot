package com.cguzowski.paymentcopilot.incident;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Component;

@Component
class SpringAiRecentServiceErrorsClient implements RecentServiceErrorsClient {

    private final McpSyncClient client;

    SpringAiRecentServiceErrorsClient(List<McpSyncClient> clients) {
        if (clients.size() != 1) {
            throw new IllegalStateException("Exactly one operations MCP connection is required.");
        }
        this.client = clients.getFirst();
    }

    @Override
    public Map<String, Object> call(EvidenceCollectionContext context, UUID toolCallId) {
        try {
            if (!client.isInitialized()) {
                client.initialize();
            }
            CallToolResult result = client.callTool(CallToolRequest.builder("getRecentServiceErrors")
                    .arguments(Map.of(
                            "tenantId", context.tenantId().toString(),
                            "scenarioReference", context.scenarioReference(),
                            "correlationId", context.correlationId().toString(),
                            "toolCallId", toolCallId.toString()))
                    .build());
            if (Boolean.TRUE.equals(result.isError())) {
                throw new EvidenceSourceUnavailableException();
            }
            return requireStringKeyedObject(result.structuredContent());
        } catch (EvidenceSourceUnavailableException | EvidenceSourceMalformedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (causedByTimeout(exception)) {
                throw new EvidenceSourceTimedOutException(exception);
            }
            throw new EvidenceSourceUnavailableException(exception);
        }
    }

    private static Map<String, Object> requireStringKeyedObject(Object structuredContent) {
        if (!(structuredContent instanceof Map<?, ?> values)) {
            throw new EvidenceSourceMalformedException();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new EvidenceSourceMalformedException();
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    private static boolean causedByTimeout(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof TimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
