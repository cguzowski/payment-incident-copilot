package com.cguzowski.paymentcopilot.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OperationsMcpServerContractTest {

    @LocalServerPort
    private int port;

    private McpSyncClient client;

    @BeforeEach
    void connect() {
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:" + port)
                .endpoint("/mcp")
                .build();
        client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(5))
                .build();
        client.initialize();
    }

    @AfterEach
    void close() {
        if (client != null) {
            client.closeGracefully();
        }
    }

    @Test
    void listsGetRecentServiceErrorsWithStableSchema() {
        assertThat(client.listTools().tools()).hasSize(1);
        Tool tool = client.listTools().tools().getFirst();

        assertThat(tool.name()).isEqualTo("getRecentServiceErrors");
        assertThat(tool.inputSchema().get("properties")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) tool.inputSchema().get("properties");
        assertThat(properties.keySet()).containsExactlyInAnyOrder(
                "tenantId",
                "scenarioReference",
                "correlationId",
                "toolCallId");
        assertThat(tool.inputSchema().get("required")).isEqualTo(
                java.util.List.of("tenantId", "scenarioReference", "correlationId", "toolCallId"));
        assertThat(tool.outputSchema()).isNotNull();
        assertThat(tool.outputSchema().get("required").toString()).doesNotContain("statusDetail");
        assertThat(tool.annotations().readOnlyHint()).isTrue();
        assertThat(tool.annotations().destructiveHint()).isFalse();
        assertThat(tool.annotations().idempotentHint()).isTrue();
        assertThat(tool.annotations().openWorldHint()).isFalse();
    }

    @Test
    void invokesKnownScenarioAndReturnsStructuredContent() {
        CallToolResult result = client.callTool(CallToolRequest.builder("getRecentServiceErrors")
                .arguments(Map.of(
                        "tenantId", "8b860d80-d17f-4e6b-8c48-af35f26a4d61",
                        "scenarioReference", "alert-auth-decline-001",
                        "correlationId", "a5d978b5-34c7-42da-9076-22f8e5169315",
                        "toolCallId", "21fdc56b-267a-4cb5-81b9-50f092e0ef35"))
                .build());

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(result.structuredContent()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) result.structuredContent();
        assertThat(content.keySet()).isEqualTo(Set.of(
                "sourceSystem",
                "sourceTool",
                "retrievedAt",
                "correlationId",
                "toolCallId",
                "status",
                "contentSchemaVersion",
                "content"));
        assertThat(content).doesNotContainKey("statusDetail");
        assertThat(content.get("status")).isEqualTo("AVAILABLE");
        assertThat(content.get("sourceTool")).isEqualTo("getRecentServiceErrors");
    }
}
