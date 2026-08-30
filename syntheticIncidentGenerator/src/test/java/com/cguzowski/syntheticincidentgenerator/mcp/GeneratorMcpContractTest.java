package com.cguzowski.syntheticincidentgenerator.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "generator.copilot-api-base-url=http://127.0.0.1:1")
@Import(GeneratorMcpContractTest.ContractClockConfiguration.class)
class GeneratorMcpContractTest {

    @LocalServerPort
    private int port;

    private McpSyncClient client;

    @BeforeEach
    void connect() {
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(
                        "http://127.0.0.1:" + port)
                .endpoint("/mcp")
                .build();
        client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(5)).build();
        client.initialize();
    }

    @AfterEach
    void close() {
        if (client != null) {
            client.closeGracefully();
        }
    }

    @Test
    void liveDiscoveryMatchesTheCopilotOwnedV1InputAndSafetyContract() {
        assertThat(client.listTools().tools()).hasSize(1);
        Tool tool = client.listTools().tools().getFirst();

        assertThat(tool.name()).isEqualTo("getRecentServiceErrors");
        assertThat(tool.description())
                .isEqualTo(
                        "Returns deterministic recent payment-authorization service errors for a synthetic scenario.");
        assertThat(tool.annotations().readOnlyHint()).isTrue();
        assertThat(tool.annotations().destructiveHint()).isFalse();
        assertThat(tool.annotations().idempotentHint()).isTrue();
        assertThat(tool.annotations().openWorldHint()).isFalse();
        assertThat(tool.inputSchema().get("properties"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsOnlyKeys("tenantId", "scenarioReference", "correlationId", "toolCallId");
        assertThat(tool.inputSchema().get("required"))
                .asList()
                .containsExactlyInAnyOrder("tenantId", "scenarioReference", "correlationId", "toolCallId");
    }

    @Test
    void liveInvocationReturnsStructuredDeterministicEvidence() {
        CallToolResult result = client.callTool(CallToolRequest.builder("getRecentServiceErrors")
                .arguments(Map.of(
                        "tenantId", "8b860d80-d17f-4e6b-8c48-af35f26a4d61",
                        "scenarioReference", "sig-v1-S001-1788167730-1234567890ab",
                        "correlationId", "e147fdc4-2bf8-4708-bbb1-f19556292ed7",
                        "toolCallId", "31b783f7-adea-46e1-a479-fe33adc1766d"))
                .build());

        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        Map<String, Object> structured = object(result.structuredContent());
        assertThat(structured)
                .containsEntry("sourceSystem", "synthetic-observability")
                .containsEntry("sourceTool", "getRecentServiceErrors")
                .containsEntry("status", "AVAILABLE")
                .containsEntry("contentSchemaVersion", "service-errors/v1")
                .containsEntry("retrievedAt", "2026-08-31T09:20:00Z");
        assertThat(structured.keySet())
                .containsAll(Set.of(
                        "sourceSystem",
                        "sourceTool",
                        "retrievedAt",
                        "correlationId",
                        "toolCallId",
                        "status",
                        "contentSchemaVersion",
                        "content"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new AssertionError("MCP structured content is not an object.");
        }
        return (Map<String, Object>) map;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ContractClockConfiguration {

        @Bean
        @Primary
        Clock contractClock() {
            return Clock.fixed(Instant.parse("2026-08-31T09:20:00Z"), ZoneOffset.UTC);
        }
    }
}
