package com.cguzowski.syntheticincidentgenerator.comparison;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
final class ComparisonArtifactStore {
    private final JsonMapper mapper;
    private final ComparisonProperties properties;

    ComparisonArtifactStore(JsonMapper mapper, ComparisonProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    void save(UUID tenantId, ComparisonService.Artifact artifact) {
        Path directory = properties.artifactDirectory().resolve(tenantId.toString());
        Path temporary = null;
        try {
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, "comparison-", ".tmp");
            Files.writeString(temporary, mapper.writeValueAsString(artifact), StandardCharsets.UTF_8);
            // Unique attempt IDs; never replace an earlier retained comparison.
            Files.move(temporary, directory.resolve(artifact.response().comparisonId() + ".json"));
        } catch (IOException exception) {
            throw new IllegalStateException("Comparison artifact could not be retained", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    /* Preserve original failure. */
                }
            }
        }
    }

    static String hash(String input) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
