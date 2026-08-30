package com.cguzowski.syntheticincidentgenerator.config;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("generator")
public record GeneratorProperties(URI copilotApiBaseUrl, UUID tenantId, Duration requestTimeout) {}
