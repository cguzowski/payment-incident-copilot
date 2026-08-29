package com.cguzowski.paymentcopilot.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cguzowski.paymentcopilot.incident.EvidenceCollectionContext;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SpringAiRecentServiceErrorsClientTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INVESTIGATION_ID = UUID.fromString("a012c9cb-85a6-4d77-9703-3b53377b56c3");
    private static final UUID CORRELATION_ID = UUID.fromString("a5d978b5-34c7-42da-9076-22f8e5169315");
    private static final UUID TOOL_CALL_ID = UUID.fromString("21fdc56b-267a-4cb5-81b9-50f092e0ef35");
    private static final EvidenceCollectionContext CONTEXT =
            new EvidenceCollectionContext(TENANT_ID, INVESTIGATION_ID, CORRELATION_ID, "alert-auth-decline-001");

    @Test
    void invokesKnownToolWithApplicationOwnedArguments() {
        McpSyncClient mcpClient = mock(McpSyncClient.class);
        Map<String, Object> structuredContent = Map.of("status", "AVAILABLE");
        when(mcpClient.isInitialized()).thenReturn(true);
        when(mcpClient.callTool(any())).thenReturn(new CallToolResult(List.of(), false, structuredContent, Map.of()));
        SpringAiRecentServiceErrorsClient client = new SpringAiRecentServiceErrorsClient(List.of(mcpClient));

        assertThat(client.call(CONTEXT, TOOL_CALL_ID)).isEqualTo(structuredContent);

        ArgumentCaptor<CallToolRequest> request = ArgumentCaptor.forClass(CallToolRequest.class);
        verify(mcpClient).callTool(request.capture());
        assertThat(request.getValue().name()).isEqualTo("getRecentServiceErrors");
        assertThat(request.getValue().arguments())
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "tenantId", TENANT_ID.toString(),
                        "scenarioReference", "alert-auth-decline-001",
                        "correlationId", CORRELATION_ID.toString(),
                        "toolCallId", TOOL_CALL_ID.toString()));
    }

    @Test
    void initializesLazilyAndClassifiesTimeouts() {
        McpSyncClient mcpClient = mock(McpSyncClient.class);
        when(mcpClient.isInitialized()).thenReturn(false);
        when(mcpClient.callTool(any())).thenThrow(new RuntimeException(new HttpTimeoutException("timed out")));
        SpringAiRecentServiceErrorsClient client = new SpringAiRecentServiceErrorsClient(List.of(mcpClient));

        assertThatThrownBy(() -> client.call(CONTEXT, TOOL_CALL_ID))
                .isInstanceOf(EvidenceSourceTimedOutException.class);
        verify(mcpClient).initialize();
    }

    @Test
    void rejectsErrorOrUnstructuredToolResponses() {
        McpSyncClient errorClient = mock(McpSyncClient.class);
        when(errorClient.isInitialized()).thenReturn(true);
        when(errorClient.callTool(any())).thenReturn(new CallToolResult(List.of(), true, Map.of(), Map.of()));
        McpSyncClient unstructuredClient = mock(McpSyncClient.class);
        when(unstructuredClient.isInitialized()).thenReturn(true);
        when(unstructuredClient.callTool(any()))
                .thenReturn(new CallToolResult(List.of(), false, "not-an-object", Map.of()));

        assertThatThrownBy(
                        () -> new SpringAiRecentServiceErrorsClient(List.of(errorClient)).call(CONTEXT, TOOL_CALL_ID))
                .isInstanceOf(EvidenceSourceUnavailableException.class);
        assertThatThrownBy(() ->
                        new SpringAiRecentServiceErrorsClient(List.of(unstructuredClient)).call(CONTEXT, TOOL_CALL_ID))
                .isInstanceOf(EvidenceSourceMalformedException.class);
    }
}
