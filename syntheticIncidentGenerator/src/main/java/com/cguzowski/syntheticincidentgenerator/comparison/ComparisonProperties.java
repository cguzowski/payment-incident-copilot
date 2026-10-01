package com.cguzowski.syntheticincidentgenerator.comparison;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("generator.comparison")
public record ComparisonProperties(URI baseUrl, String model, Duration timeout, Path artifactDirectory) {
    public ComparisonProperties {
        if (baseUrl == null
                || !java.util.Set.of("http", "https").contains(baseUrl.getScheme())
                || baseUrl.getHost() == null
                || baseUrl.getUserInfo() != null
                || baseUrl.getQuery() != null
                || baseUrl.getFragment() != null
                || model == null
                || model.isBlank()
                || timeout == null
                || timeout.isNegative()
                || timeout.isZero()
                || artifactDirectory == null) {
            throw new IllegalArgumentException("Invalid comparison configuration");
        }
    }
}
