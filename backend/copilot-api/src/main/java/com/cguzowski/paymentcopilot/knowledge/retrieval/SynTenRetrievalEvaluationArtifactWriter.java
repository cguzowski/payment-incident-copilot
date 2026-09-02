package com.cguzowski.paymentcopilot.knowledge.retrieval;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingClient;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
class SynTenRetrievalEvaluationArtifactWriter {

    private static final int MAXIMUM_ARTIFACT_BYTES = 4_000_000;
    private static final List<String> FORBIDDEN_JSON = List.of(
            "\"embeddingInput\"",
            "\"embedding_input\"",
            "\"rawContent\"",
            "\"databaseUrl\"",
            "\"stackTrace\"",
            "jdbc:postgresql:");

    private final Path outputDirectory;
    private final JsonMapper jsonMapper;

    @Autowired
    SynTenRetrievalEvaluationArtifactWriter(
            @Value("${app.knowledge.retrieval-evaluation.output-directory:}") String configuredDirectory,
            JsonMapper jsonMapper) {
        this(
                configuredDirectory == null || configuredDirectory.isBlank() ? null : Path.of(configuredDirectory),
                jsonMapper);
    }

    SynTenRetrievalEvaluationArtifactWriter(Path outputDirectory, JsonMapper jsonMapper) {
        this.outputDirectory = outputDirectory == null
                ? null
                : outputDirectory.toAbsolutePath().normalize();
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    Path write(SynTenRetrievalEvaluationRun run) {
        validateComplete(run);
        byte[] json = serialize(run);
        validateSerialized(json);
        if (outputDirectory == null) {
            throw invalid("SynTen retrieval evaluation output directory is not configured.");
        }
        String status = run.grade().passed() ? "PASS" : "FAIL";
        Path finalPath = outputDirectory.resolve(run.runId() + "-" + status + ".json");
        Path temporary = null;
        try {
            if (Files.exists(finalPath)) {
                throw invalid("SynTen retrieval evaluation result already exists: " + finalPath + ".");
            }
            Files.createDirectories(outputDirectory);
            temporary = Files.createTempFile(outputDirectory, "." + run.runId() + "-", ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.write(ByteBuffer.wrap(json));
                channel.force(true);
            }
            Files.move(temporary, finalPath, StandardCopyOption.ATOMIC_MOVE);
            temporary = null;
            return finalPath;
        } catch (AtomicMoveNotSupportedException exception) {
            throw invalid("Atomic evaluation result publication is not supported by the output filesystem.", exception);
        } catch (IOException exception) {
            throw invalid("SynTen retrieval evaluation result could not be published.", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The final artifact was not published. Do not hide the original write failure.
                }
            }
        }
    }

    private byte[] serialize(SynTenRetrievalEvaluationRun run) {
        try {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(run);
        } catch (JacksonException exception) {
            throw invalid("SynTen retrieval evaluation result could not be serialized.", exception);
        }
    }

    private static void validateSerialized(byte[] json) {
        if (json.length == 0 || json.length > MAXIMUM_ARTIFACT_BYTES) {
            throw invalid("SynTen retrieval evaluation result exceeds its bounded artifact contract.");
        }
        String value = new String(json, java.nio.charset.StandardCharsets.UTF_8);
        for (String forbidden : FORBIDDEN_JSON) {
            if (value.contains(forbidden)) {
                throw invalid("SynTen retrieval evaluation result contains a forbidden field or value.");
            }
        }
    }

    private static void validateComplete(SynTenRetrievalEvaluationRun run) {
        if (run == null
                || !SynTenRetrievalEvaluationRun.SCHEMA_VERSION.equals(run.schemaVersion())
                || run.runId() == null
                || !run.runId().matches("[0-9a-f]{32}")
                || !SynTenRetrievalEvaluationContractRepository.EVALUATION_VERSION.equals(run.evaluationVersion())
                || !SynTenRetrievalEvaluationContractRepository.CORPUS_VERSION.equals(run.corpusVersion())
                || run.catalogFingerprint() == null
                || !run.catalogFingerprint().matches("[0-9a-f]{64}")
                || !"pdfbox-text-pages/v1".equals(run.extractionVersion())
                || !"pdf-page-sections/v1".equals(run.chunkingVersion())
                || !KnowledgeEmbeddingClient.MODEL_ID.equals(run.embeddingModel())
                || run.embeddingDimensions() != KnowledgeEmbeddingClient.DIMENSIONS
                || run.evaluatedAt() == null
                || run.completedAt() == null
                || run.completedAt().isBefore(run.evaluatedAt())
                || !SynTenRetrievalEvaluationContractRepository.TENANT_ID.equals(run.tenantId())
                || run.thresholds() == null
                || run.grade() == null
                || run.variants().size() != 37
                || run.grade().cases().size() != 23
                || run.grade().variants().size() != 37
                || run.grade().aggregate().actualVariantCount() != 37
                || run.grade().aggregate().expectedVariantCount() != 37) {
            throw invalid("SynTen retrieval evaluation result is not complete.");
        }
        validateThresholds(run.thresholds());
        Set<String> variantKeys = new HashSet<>();
        String prior = null;
        for (SynTenRetrievalEvaluationVariantRecord variant : run.variants()) {
            String key = variant.caseId() + "/" + variant.variantId();
            if (!variantKeys.add(key)
                    || (prior != null && prior.compareTo(key) >= 0)
                    || !variant.caseId().equals(variant.result().caseId())
                    || !variant.variantId().equals(variant.result().variantId())
                    || !variant.caseId().equals(variant.assertions().caseId())
                    || !variant.variantId().equals(variant.assertions().variantId())
                    || variant.scenarioReference() == null
                    || variant.incidentId() == null
                    || variant.investigationId() == null
                    || variant.evidenceId() == null
                    || variant.filters() == null
                    || variant.queryEmbedding() == null
                    || variant.result().derivedQuery() == null
                    || variant.result().derivedQuery().isBlank()
                    || variant.result().candidates().size()
                            > (long) variant.result().candidateDepth() * 4) {
                throw invalid("SynTen retrieval evaluation result is not complete or deterministically ordered.");
            }
            prior = key;
        }
    }

    private static void validateThresholds(EvaluationThresholds thresholds) {
        if (thresholds.primaryRunbookRequired() != 22
                || thresholds.supportingPolicyRequired() != 20
                || thresholds.supportingPolicyApplicable() != 22
                || thresholds.primaryOutranksWeakPercentRequired() != 90
                || thresholds.ineligibleCandidateMaximum() != 0
                || !thresholds.partialSemanticsRequired()
                || !thresholds.unavailableSemanticsRequired()
                || !thresholds.supersededExclusionRequired()) {
            throw invalid("SynTen retrieval evaluation thresholds have drifted.");
        }
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private static IllegalArgumentException invalid(String message, Exception cause) {
        return new IllegalArgumentException(message, cause);
    }
}
