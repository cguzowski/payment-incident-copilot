package com.cguzowski.paymentcopilot.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class SynTenRetrievalEvaluationSeedRepositoryTest {

    private static final Path SYNTEN_ROOT =
            Path.of("..", "..", "SynTen Inc").toAbsolutePath().normalize();
    private static final Instant EVALUATED_AT = Instant.parse("2026-09-01T12:00:00Z");
    private static final JsonMapper JSON =
            JsonMapper.builder().findAndAddModules().build();
    private static final SynTenRetrievalEvaluationContract CONTRACT = new SynTenRetrievalEvaluationContractRepository(
                    SYNTEN_ROOT.resolve("evaluation/retrieval-cases.md"),
                    Path.of(
                                    "..",
                                    "..",
                                    "syntheticIncidentGenerator",
                                    "src",
                                    "main",
                                    "resources",
                                    "scenarios",
                                    "catalog.json")
                            .toAbsolutePath()
                            .normalize(),
                    SYNTEN_ROOT.resolve("corpus"),
                    JSON)
            .load(EVALUATED_AT);

    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsAndValidatesTheExact37PersistedVariantMappings() throws Exception {
        Path path = writeManifest(idealMappings(), Map.of());

        SynTenRetrievalEvaluationSeed seed = new SynTenRetrievalEvaluationSeedRepository(path, JSON).load();
        seed.validateAgainst(CONTRACT);

        assertThat(seed.schemaVersion()).isEqualTo("synten-retrieval-eval-seed/v1");
        assertThat(seed.evaluationDatabaseName()).isEqualTo("payment_copilot_k4_eval_test");
        assertThat(seed.mappings()).hasSize(37);
        assertThat(seed.mappings().getFirst().caseId()).isEqualTo("KQ-001");
        assertThat(seed.mappings().getLast().variantId()).isEqualTo("EXCLUSION");
    }

    @Test
    void failsClosedForMissingDuplicatedUnknownOrDriftedMappings() throws Exception {
        List<Map<String, Object>> missing = idealMappings();
        missing.removeLast();
        List<Map<String, Object>> duplicated = idealMappings();
        duplicated.add(new LinkedHashMap<>(duplicated.getFirst()));
        List<Map<String, Object>> unknown = idealMappings();
        unknown.getFirst().put("VariantId", "S999");

        assertThatThrownBy(() -> loadAndValidate(missing, Map.of())).hasMessageContaining("37");
        assertThatThrownBy(() -> loadAndValidate(duplicated, Map.of())).hasMessageContaining("duplicate");
        assertThatThrownBy(() -> loadAndValidate(unknown, Map.of())).hasMessageContaining("mapping");
        assertThatThrownBy(
                        () -> loadAndValidate(idealMappings(), Map.of("evaluationVersion", "synten-retrieval-eval/v2")))
                .hasMessageContaining("contract");
    }

    @Test
    void rejectsUnsafeDatabaseNamesWrongTenantAndMalformedIdentifiers() throws Exception {
        assertThatThrownBy(() -> loadAndValidate(idealMappings(), Map.of("evaluationDatabaseName", "payment_copilot")))
                .hasMessageContaining("dedicated");
        assertThatThrownBy(() -> loadAndValidate(
                        idealMappings(), Map.of("tenantId", UUID.randomUUID().toString())))
                .hasMessageContaining("tenant");
        List<Map<String, Object>> malformed = idealMappings();
        malformed.getFirst().put("InvestigationId", "not-a-uuid");
        assertThatThrownBy(() -> loadAndValidate(malformed, Map.of())).hasMessageContaining("malformed");
    }

    private void loadAndValidate(List<Map<String, Object>> mappings, Map<String, Object> overrides) throws Exception {
        SynTenRetrievalEvaluationSeed seed =
                new SynTenRetrievalEvaluationSeedRepository(writeManifest(mappings, overrides), JSON).load();
        seed.validateAgainst(CONTRACT);
    }

    private Path writeManifest(List<Map<String, Object>> mappings, Map<String, Object> overrides) throws Exception {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schemaVersion", "synten-retrieval-eval-seed/v1");
        manifest.put("evaluationVersion", "synten-retrieval-eval/v1");
        manifest.put("corpusVersion", "synten-auth-knowledge/v1");
        manifest.put("runId", "0123456789abcdef0123456789abcdef");
        manifest.put("createdAt", "2026-09-01T12:01:00Z");
        manifest.put("evaluatedAt", EVALUATED_AT.toString());
        manifest.put("tenantId", SynTenRetrievalEvaluationContractRepository.TENANT_ID.toString());
        manifest.put("evaluationDatabaseName", "payment_copilot_k4_eval_test");
        manifest.put("mappings", mappings);
        manifest.putAll(overrides);
        Path path = temporaryDirectory.resolve(UUID.randomUUID() + ".json");
        Files.writeString(path, JSON.writeValueAsString(manifest));
        return path;
    }

    private static List<Map<String, Object>> idealMappings() {
        AtomicLong ids = new AtomicLong(1);
        List<Map<String, Object>> mappings = new ArrayList<>();
        for (RetrievalEvaluationCase evaluationCase : CONTRACT.cases()) {
            List<String> variantIds =
                    evaluationCase.caseId().equals("KQ-023") ? List.of("EXCLUSION") : evaluationCase.scenarioIds();
            for (String variantId : variantIds) {
                long id = ids.getAndIncrement();
                Map<String, Object> mapping = new LinkedHashMap<>();
                mapping.put("CaseId", evaluationCase.caseId());
                mapping.put("VariantId", variantId);
                mapping.put(
                        "ScenarioReference",
                        variantId.equals("EXCLUSION")
                                ? "sig-v1-S999-1788264000-0123456789ab"
                                : "sig-v1-" + variantId + "-1788264000-0123456789ab");
                mapping.put("IncidentId", new UUID(1, id).toString());
                mapping.put("InvestigationId", new UUID(2, id).toString());
                mapping.put("EvidenceId", new UUID(3, id).toString());
                mapping.put("EvidenceStatus", evidenceStatus(evaluationCase.caseId()));
                mappings.add(mapping);
            }
        }
        return mappings;
    }

    private static String evidenceStatus(String caseId) {
        return switch (caseId) {
            case "KQ-020" -> "PARTIAL";
            case "KQ-022" -> "UNAVAILABLE";
            case "KQ-023" -> "NOT_FOUND";
            default -> "AVAILABLE";
        };
    }
}
