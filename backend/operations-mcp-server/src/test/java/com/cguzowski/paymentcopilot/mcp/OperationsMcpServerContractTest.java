package com.cguzowski.paymentcopilot.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
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
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(OperationsMcpServerContractTest.ContractClockConfiguration.class)
class OperationsMcpServerContractTest {

    private static final String CONTRACT_ROOT = "contracts/mcp/get-recent-service-errors/v1/";

    @LocalServerPort
    private int port;

    private McpSyncClient client;

    @BeforeEach
    void connect() {
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(
                        "http://localhost:" + port)
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
    void liveDiscoverySemanticallyMatchesTheImmutableV1Contract() {
        assertThat(client.listTools().tools()).hasSize(1);
        Tool tool = client.listTools().tools().getFirst();
        Map<String, Object> metadata = contract("metadata.json");
        Map<String, Object> expectedTool = object(metadata.get("tool"));
        Map<String, Object> annotations = object(expectedTool.get("annotations"));

        assertThat(tool.name()).isEqualTo(expectedTool.get("name"));
        assertThat(tool.description()).isEqualTo(expectedTool.get("description"));
        assertThat(tool.annotations().readOnlyHint()).isEqualTo(annotations.get("readOnlyHint"));
        assertThat(tool.annotations().destructiveHint()).isEqualTo(annotations.get("destructiveHint"));
        assertThat(tool.annotations().idempotentHint()).isEqualTo(annotations.get("idempotentHint"));
        assertThat(tool.annotations().openWorldHint()).isEqualTo(annotations.get("openWorldHint"));

        assertSchemaSemantics(tool.inputSchema(), contract("input.schema.json"));
        assertSchemaSemantics(tool.outputSchema(), contract("output.schema.json"));
    }

    @Test
    void liveResponsesMatchCanonicalV1Fixtures() {
        Map<String, Object> arguments = contract("fixtures/request.json");
        CallToolResult available = call(arguments);
        assertThat(available.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(available.structuredContent()).isEqualTo(contract("fixtures/available-response.json"));

        arguments.put("scenarioReference", "alert-auth-decline-unavailable");
        CallToolResult unavailable = call(arguments);
        assertThat(unavailable.isError()).isNotEqualTo(Boolean.TRUE);
        assertThat(unavailable.structuredContent()).isEqualTo(contract("fixtures/unavailable-response.json"));
    }

    private CallToolResult call(Map<String, Object> arguments) {
        return client.callTool(CallToolRequest.builder("getRecentServiceErrors")
                .arguments(arguments)
                .build());
    }

    private static void assertSchemaSemantics(Map<String, Object> live, Map<String, Object> expected) {
        assertThat(live).isNotNull();
        assertThat(live.get("type")).isEqualTo(expected.get("type"));
        assertThat(asSet(live.get("required"))).isEqualTo(asSet(expected.get("required")));
        Map<String, Object> liveProperties = object(live.get("properties"));
        Map<String, Object> expectedProperties = object(expected.get("properties"));
        assertThat(liveProperties.keySet()).isEqualTo(expectedProperties.keySet());
        for (String field : expectedProperties.keySet()) {
            Map<String, Object> liveProperty = object(liveProperties.get(field));
            Map<String, Object> expectedProperty = object(expectedProperties.get(field));
            for (String semantic : Set.of("type", "format", "description", "enum")) {
                if (expectedProperty.containsKey(semantic)) {
                    assertThat(liveProperty.get(semantic))
                            .as("%s schema semantic for %s", semantic, field)
                            .isEqualTo(expectedProperty.get(semantic));
                }
            }
        }
    }

    private static Set<Object> asSet(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            throw new AssertionError("Contract schema required field is not an array.");
        }
        Set<Object> result = new LinkedHashSet<>();
        iterable.forEach(result::add);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new AssertionError("Contract value is not an object.");
        }
        return (Map<String, Object>) map;
    }

    private static Map<String, Object> contract(String path) {
        JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
        try (InputStream input = new ClassPathResource(CONTRACT_ROOT + path).getInputStream()) {
            return mapper.readValue(input, new TypeReference<>() {});
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read MCP contract resource " + path, exception);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ContractClockConfiguration {

        @Bean
        @Primary
        Clock contractClock() {
            return Clock.fixed(Instant.parse("2026-08-28T10:00:00Z"), ZoneOffset.UTC);
        }
    }
}
