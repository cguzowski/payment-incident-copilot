package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Repository
class SynTenRetrievalEvaluationSeedRepository {

    private final Path seedManifestPath;
    private final JsonMapper jsonMapper;

    @Autowired
    SynTenRetrievalEvaluationSeedRepository(
            @Value("${app.knowledge.retrieval-evaluation.seed-manifest-path:}") String configuredPath,
            JsonMapper jsonMapper) {
        this(configuredPath == null || configuredPath.isBlank() ? null : Path.of(configuredPath), jsonMapper);
    }

    SynTenRetrievalEvaluationSeedRepository(Path seedManifestPath, JsonMapper jsonMapper) {
        this.seedManifestPath = seedManifestPath == null
                ? null
                : seedManifestPath.toAbsolutePath().normalize();
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    SynTenRetrievalEvaluationSeed load() {
        if (seedManifestPath == null || !Files.isRegularFile(seedManifestPath)) {
            throw invalid("SynTen retrieval evaluation seed manifest is missing.");
        }
        RawSeed raw;
        try {
            raw = jsonMapper.readValue(Files.readString(seedManifestPath, StandardCharsets.UTF_8), RawSeed.class);
        } catch (IOException | JacksonException exception) {
            throw invalid("SynTen retrieval evaluation seed manifest is malformed.", exception);
        }
        if (raw == null || raw.mappings() == null) {
            throw invalid("SynTen retrieval evaluation seed manifest is malformed.");
        }
        try {
            return new SynTenRetrievalEvaluationSeed(
                    raw.schemaVersion(),
                    raw.evaluationVersion(),
                    raw.corpusVersion(),
                    raw.runId(),
                    Instant.parse(raw.createdAt()),
                    Instant.parse(raw.evaluatedAt()),
                    UUID.fromString(raw.tenantId()),
                    raw.evaluationDatabaseName(),
                    raw.mappings().stream()
                            .map(SynTenRetrievalEvaluationSeedRepository::mapping)
                            .toList());
        } catch (DateTimeException | IllegalArgumentException | NullPointerException exception) {
            throw invalid("SynTen retrieval evaluation seed manifest is malformed.", exception);
        }
    }

    private static SynTenRetrievalEvaluationSeedMapping mapping(RawMapping raw) {
        return new SynTenRetrievalEvaluationSeedMapping(
                raw.CaseId(),
                raw.VariantId(),
                raw.ScenarioReference(),
                UUID.fromString(raw.IncidentId()),
                UUID.fromString(raw.InvestigationId()),
                UUID.fromString(raw.EvidenceId()),
                raw.EvidenceStatus());
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private static IllegalArgumentException invalid(String message, Exception cause) {
        return new IllegalArgumentException(message, cause);
    }

    private record RawSeed(
            String schemaVersion,
            String evaluationVersion,
            String corpusVersion,
            String runId,
            String createdAt,
            String evaluatedAt,
            String tenantId,
            String evaluationDatabaseName,
            List<RawMapping> mappings) {}

    @SuppressWarnings("java:S116")
    private record RawMapping(
            String CaseId,
            String VariantId,
            String ScenarioReference,
            String IncidentId,
            String InvestigationId,
            String EvidenceId,
            String EvidenceStatus) {}
}
