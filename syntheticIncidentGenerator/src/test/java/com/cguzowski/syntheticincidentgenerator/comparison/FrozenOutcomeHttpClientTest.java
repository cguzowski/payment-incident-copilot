package com.cguzowski.syntheticincidentgenerator.comparison;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cguzowski.syntheticincidentgenerator.config.GeneratorProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class FrozenOutcomeHttpClientTest {
    private final UUID incident = UUID.randomUUID(),
            investigation = UUID.randomUUID(),
            report = UUID.randomUUID(),
            tenant = UUID.randomUUID(),
            evidence = UUID.randomUUID();
    private HttpServer server;
    private final List<String> headers = new ArrayList<>();
    private String decisionReport;
    private String investigationIncident;

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    @Test
    void selectsDecisionBoundReportRatherThanNewestAndReadsOnlyWithTenantHeaders() throws Exception {
        start();
        var result = client().find(incident, "REJECTED");
        assertThat(result.reportAttemptId()).isEqualTo(report);
        assertThat(result.report().path("probableCause").path("statement").asText())
                .isEqualTo("Bound report");
        assertThat(result.evidence()).hasSize(1);
        assertThat(headers).hasSize(5).containsOnly("GET:" + tenant);
    }

    @Test
    void missingDecisionBoundReportFailsClosed() throws Exception {
        start();
        decisionReport = UUID.randomUUID().toString();
        assertThatThrownBy(() -> client().find(incident, "REJECTED")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void foreignInvestigationAndTerminalMismatchFailClosed() throws Exception {
        start();
        investigationIncident = UUID.randomUUID().toString();
        assertThatThrownBy(() -> client().find(incident, "REJECTED")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client().find(incident, "APPROVED")).isInstanceOf(IllegalArgumentException.class);
    }

    private FrozenOutcomeHttpClient client() {
        return new FrozenOutcomeHttpClient(
                JsonMapper.builder().build(),
                new GeneratorProperties(
                        URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                        tenant,
                        Duration.ofSeconds(2)));
    }

    private void start() throws Exception {
        decisionReport = report.toString();
        investigationIncident = incident.toString();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/", exchange -> {
            headers.add(exchange.getRequestMethod() + ":"
                    + exchange.getRequestHeaders().getFirst("X-Synthetic-Tenant-Id"));
            String path = exchange.getRequestURI().getPath();
            String body;
            if (path.endsWith("/decisions"))
                body =
                        "[{\"decisionId\":\"%s\",\"investigationId\":\"%s\",\"reportAttemptId\":\"%s\",\"outcome\":\"REJECTED\",\"incidentStatus\":\"REJECTED\"}]"
                                .formatted(UUID.randomUUID(), investigation, decisionReport);
            else if (path.endsWith("/reports"))
                body =
                        "[{\"attemptId\":\"%s\",\"investigationId\":\"%s\",\"status\":\"AVAILABLE\",\"latestEvidenceId\":\"%s\",\"applicableEvidenceId\":\"%s\",\"report\":{\"disposition\":\"PROPOSED\",\"confidence\":{\"level\":\"HIGH\"},\"probableCause\":{\"statement\":\"Bound report\"},\"recommendation\":null}},{\"attemptId\":\"%s\",\"investigationId\":\"%s\",\"status\":\"AVAILABLE\",\"report\":{\"probableCause\":{\"statement\":\"Unrelated newest retry\"}}}]"
                                .formatted(report, investigation, evidence, evidence, UUID.randomUUID(), investigation);
            else if (path.endsWith("/evidence-collections"))
                body = "[{\"evidenceId\":\"%s\",\"status\":\"AVAILABLE\",\"content\":{\"errors\":[]}}]"
                        .formatted(evidence);
            else if (path.contains("/incidents/"))
                body = "{\"incidentId\":\"%s\",\"status\":\"REJECTED\",\"activeInvestigationId\":\"%s\"}"
                        .formatted(incident, investigation);
            else
                body = "{\"investigationId\":\"%s\",\"incidentId\":\"%s\"}"
                        .formatted(investigation, investigationIncident);
            byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }
}
