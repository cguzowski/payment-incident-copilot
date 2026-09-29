package com.cguzowski.syntheticincidentgenerator.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class CopilotIncidentReviewHttpClientTest {

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
    void getsCanonicalIncidentStateWithTenantScope() throws Exception {
        AtomicReference<String> tenantHeader = new AtomicReference<>();
        server = server(exchange -> {
            tenantHeader.set(exchange.getRequestHeaders().getFirst("X-Synthetic-Tenant-Id"));
            respond(exchange, 200, """
                    {"incidentId":"%s","externalAlertId":"sig-v1-S203-1788167730-1234567890ab","status":"APPROVED"}
                    """.formatted(INCIDENT_ID));
        });
        CopilotIncidentReviewHttpClient client = client();

        IncidentReviewState result = client.find(INCIDENT_ID);

        assertThat(tenantHeader.get()).isEqualTo(TENANT_ID.toString());
        assertThat(result)
                .isEqualTo(new IncidentReviewState(INCIDENT_ID, "sig-v1-S203-1788167730-1234567890ab", "APPROVED"));
    }

    @Test
    void failsClosedWhenCopilotCannotConfirmIncidentState() throws Exception {
        server = server(exchange -> respond(exchange, 503, "{}"));
        CopilotIncidentReviewHttpClient client = client();

        assertThatThrownBy(() -> client.find(INCIDENT_ID)).isInstanceOf(AnswerKeyUnavailableException.class);
    }

    private CopilotIncidentReviewHttpClient client() {
        return new CopilotIncidentReviewHttpClient(
                HttpClient.newHttpClient(),
                JsonMapper.builder().findAndAddModules().build(),
                URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                TENANT_ID,
                Duration.ofSeconds(2));
    }

    private HttpServer server(ExchangeHandler handler) throws IOException {
        HttpServer created = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        created.createContext("/api/incidents/" + INCIDENT_ID, exchange -> handler.handle(exchange));
        created.start();
        return created;
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
