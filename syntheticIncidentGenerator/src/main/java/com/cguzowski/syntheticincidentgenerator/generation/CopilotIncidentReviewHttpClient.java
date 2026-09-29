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
final class CopilotIncidentReviewHttpClient implements CopilotIncidentReviewClient {

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final URI baseUri;
    private final UUID tenantId;
    private final Duration timeout;

    @Autowired
    CopilotIncidentReviewHttpClient(JsonMapper jsonMapper, GeneratorProperties properties) {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(properties.requestTimeout())
                        .build(),
                jsonMapper,
                properties.copilotApiBaseUrl(),
                properties.tenantId(),
                properties.requestTimeout());
    }

    CopilotIncidentReviewHttpClient(
            HttpClient httpClient, JsonMapper jsonMapper, URI baseUri, UUID tenantId, Duration timeout) {
        this.httpClient = httpClient;
        this.jsonMapper = jsonMapper;
        this.baseUri = baseUri;
        this.tenantId = tenantId;
        this.timeout = timeout;
    }

    @Override
    public IncidentReviewState find(UUID incidentId) {
        try {
            URI uri = baseUri.resolve("/api/incidents/" + incidentId);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("Accept", "application/json")
                    .header("X-Synthetic-Tenant-Id", tenantId.toString())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new AnswerKeyUnavailableException();
            }
            CopilotIncidentDetail body = jsonMapper.readValue(response.body(), CopilotIncidentDetail.class);
            if (body == null
                    || !incidentId.equals(body.incidentId())
                    || body.externalAlertId() == null
                    || body.externalAlertId().isBlank()
                    || body.status() == null
                    || body.status().isBlank()) {
                throw new AnswerKeyUnavailableException();
            }
            return new IncidentReviewState(body.incidentId(), body.externalAlertId(), body.status());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AnswerKeyUnavailableException(exception);
        } catch (IOException | IllegalArgumentException exception) {
            throw new AnswerKeyUnavailableException(exception);
        }
    }

    private record CopilotIncidentDetail(
            UUID incidentId,
            String externalAlertId,
            String incidentType,
            String severity,
            String status,
            String title,
            String description,
            Instant detectedAt,
            Instant receivedAt,
            UUID activeInvestigationId) {}
}
