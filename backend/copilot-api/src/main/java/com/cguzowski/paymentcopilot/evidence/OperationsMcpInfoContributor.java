package com.cguzowski.paymentcopilot.evidence;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
class OperationsMcpInfoContributor implements InfoContributor {
    private final String baseUrl;

    OperationsMcpInfoContributor(
            @Value("${spring.ai.mcp.client.streamable-http.connections.operations.url}") String configuredUrl) {
        URI uri = URI.create(configuredUrl);
        try {
            baseUrl = new URI(
                            uri.getScheme(),
                            null,
                            uri.getHost(),
                            uri.getPort(),
                            uri.getPath().replaceAll("/+$", ""),
                            null,
                            null)
                    .toString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid operations MCP endpoint.");
        }
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("operationsMcp", Map.of("baseUrl", baseUrl));
    }
}
