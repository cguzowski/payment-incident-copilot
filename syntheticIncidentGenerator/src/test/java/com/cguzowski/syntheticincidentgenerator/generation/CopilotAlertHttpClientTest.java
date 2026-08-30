package com.cguzowski.syntheticincidentgenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class CopilotAlertHttpClientTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INCIDENT_ID = UUID.fromString("36cfb9b5-21c9-44b8-b10c-ad2a60706ab6");

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void postsExactAlertContractAndTenantHeaderToCopilotIntake() throws Exception {
        AtomicReference<String> tenantHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/alerts", exchange -> respond(exchange, tenantHeader, requestBody));
        server.start();
        JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
        CopilotAlertHttpClient client = new CopilotAlertHttpClient(
                HttpClient.newHttpClient(),
                mapper,
                URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                TENANT_ID,
                Duration.ofSeconds(2));
        AlertIntakeRequest request = new AlertIntakeRequest(
                "sig-v1-S001-1788167730-1234567890ab",
                "HIGH",
                Instant.parse("2026-08-31T09:15:30Z"),
                "Authorization decline rate above threshold",
                "Synthetic declines exceeded the configured threshold.");

        AlertIntakeResponse response = client.submit(request);

        assertThat(tenantHeader.get()).isEqualTo(TENANT_ID.toString());
        Map<String, Object> body = mapper.readValue(requestBody.get(), new TypeReference<>() {});
        assertThat(body.keySet())
                .containsExactlyInAnyOrder("externalAlertId", "severity", "detectedAt", "title", "description");
        assertThat(body).doesNotContainKeys("tenantId", "scenarioCode", "answerKey", "rootCause", "evidence");
        assertThat(response.incidentId()).isEqualTo(INCIDENT_ID);
        assertThat(response.incidentType()).isEqualTo("AUTHORIZATION_DECLINE_RATE_SPIKE");
        assertThat(response.status()).isEqualTo("NEW");
    }

    private static void respond(
            HttpExchange exchange, AtomicReference<String> tenantHeader, AtomicReference<String> requestBody)
            throws IOException {
        tenantHeader.set(exchange.getRequestHeaders().getFirst("X-Synthetic-Tenant-Id"));
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] response = ("""
                {
                  "incidentId":"%s",
                  "tenantId":"%s",
                  "externalAlertId":"sig-v1-S001-1788167730-1234567890ab",
                  "incidentType":"AUTHORIZATION_DECLINE_RATE_SPIKE",
                  "severity":"HIGH",
                  "status":"NEW",
                  "title":"Authorization decline rate above threshold",
                  "description":"Synthetic declines exceeded the configured threshold.",
                  "detectedAt":"2026-08-31T09:15:30Z",
                  "receivedAt":"2026-08-31T09:15:31Z"
                }
                """).formatted(INCIDENT_ID, TENANT_ID).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(201, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
