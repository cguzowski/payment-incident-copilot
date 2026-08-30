package com.cguzowski.syntheticincidentgenerator.generation;

import com.cguzowski.syntheticincidentgenerator.config.GeneratorProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class CopilotAlertHttpClient implements AlertIntakeClient {

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final URI alertIntakeUri;
    private final UUID tenantId;
    private final Duration timeout;

    @Autowired
    public CopilotAlertHttpClient(JsonMapper jsonMapper, GeneratorProperties properties) {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(properties.requestTimeout())
                        .build(),
                jsonMapper,
                properties.copilotApiBaseUrl(),
                properties.tenantId(),
                properties.requestTimeout());
    }

    CopilotAlertHttpClient(HttpClient httpClient, JsonMapper jsonMapper, URI baseUri, UUID tenantId, Duration timeout) {
        this.httpClient = httpClient;
        this.jsonMapper = jsonMapper;
        this.alertIntakeUri = baseUri.resolve("/api/alerts");
        this.tenantId = tenantId;
        this.timeout = timeout;
    }

    @Override
    public AlertIntakeResponse submit(AlertIntakeRequest request) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(alertIntakeUri)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-Synthetic-Tenant-Id", tenantId.toString())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(request)))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 && response.statusCode() != 201) {
                throw new AlertIntakeException("Copilot alert intake returned HTTP " + response.statusCode() + ".");
            }
            CopilotAlertResponse body = jsonMapper.readValue(response.body(), CopilotAlertResponse.class);
            return new AlertIntakeResponse(body.incidentId(), body.incidentType(), body.status(), body.receivedAt());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AlertIntakeException("Copilot alert intake was interrupted.", exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new AlertIntakeException("Copilot alert intake could not be reached or decoded.", exception);
        }
    }

    private record CopilotAlertResponse(
            UUID incidentId,
            UUID tenantId,
            String externalAlertId,
            String incidentType,
            String severity,
            String status,
            String title,
            String description,
            Instant detectedAt,
            Instant receivedAt) {}
}
