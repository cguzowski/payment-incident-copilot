package com.cguzowski.paymentcopilot.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

class SynTenRetrievalEvaluationContractRepositoryTest {

    private static final Path SYNTEN_ROOT =
            Path.of("..", "..", "SynTen Inc").toAbsolutePath().normalize();
    private static final Path CASES = SYNTEN_ROOT.resolve("evaluation/retrieval-cases.md");
    private static final Path CORPUS = SYNTEN_ROOT.resolve("corpus");
    private static final Path SCENARIOS = Path.of(
                    "..", "..", "syntheticIncidentGenerator", "src", "main", "resources", "scenarios", "catalog.json")
            .toAbsolutePath()
            .normalize();
    private static final Instant EVALUATED_AT = Instant.parse("2026-09-01T12:00:00Z");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void loadsTheOneReviewedContractAcrossAllThreeAuthoritativeSources() {
        SynTenRetrievalEvaluationContract contract =
                repository(CASES, SCENARIOS, CORPUS).load(EVALUATED_AT);

        assertThat(contract.evaluationVersion()).isEqualTo("synten-retrieval-eval/v1");
        assertThat(contract.corpusVersion()).isEqualTo("synten-auth-knowledge/v1");
        assertThat(contract.evaluatedAt()).isEqualTo(EVALUATED_AT);
        assertThat(contract.cases())
                .extracting(RetrievalEvaluationCase::caseId)
                .containsExactlyElementsOf(expectedCaseIds());
        assertThat(contract.scenarios().keySet()).containsExactlyElementsOf(expectedScenarioIds());
        assertThat(contract.documents()).hasSize(30);
        assertThat(contract.documents().values())
                .filteredOn(document -> document.approvalStatus().equals("SUPERSEDED"))
                .extracting(EvaluationDocument::key)
                .containsExactlyInAnyOrder("RB-022", "PL-007", "PL-008");
        assertThat(contract.cases().stream().flatMap(item -> item.scenarioIds().stream()))
                .containsExactlyInAnyOrderElementsOf(expectedScenarioIds());

        RetrievalEvaluationCase partial = contract.caseById("KQ-020");
        assertThat(partial.scenarioIds()).containsExactly("S111");
        assertThat(partial.primaryRunbookKey()).isEqualTo("RB-020");
        assertThat(partial.supportingPolicyKey()).isEqualTo("PL-002");
        RetrievalEvaluationCase unavailable = contract.caseById("KQ-022");
        assertThat(unavailable.scenarioIds()).containsExactly("S211");
        assertThat(unavailable.weakApprovedMatchKey()).isNull();
        assertThat(contract.caseById("KQ-023").scenarioIds()).isEmpty();
    }

    @Test
    void rejectsADuplicatedOrMissingCaseBeforeEvaluation() throws IOException {
        Path changedCases = copyWithReplacement(CASES, "| KQ-022 | S211 |", "| KQ-021 | S211 |");

        assertThatThrownBy(() -> repository(changedCases, SCENARIOS, CORPUS).load(EVALUATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly KQ-001 through KQ-023");
    }

    @Test
    void rejectsScenarioCatalogDriftBeforeEvaluation() throws IOException {
        Path changedScenarios = copyWithReplacement(SCENARIOS, "\"code\": \"S211\"", "\"code\": \"S999\"");

        assertThatThrownBy(() -> repository(CASES, changedScenarios, CORPUS).load(EVALUATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scenario coverage");
    }

    @Test
    void rejectsAnUnknownExpectedDocumentKeyBeforeEvaluation() throws IOException {
        Path changedCases = copyWithReplacement(CASES, "| RB-002 | PL-006 | RB-006 |", "| RB-999 | PL-006 | RB-006 |");

        assertThatThrownBy(() -> repository(changedCases, SCENARIOS, CORPUS).load(EVALUATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown primary runbook");
    }

    @Test
    void rejectsManifestAndMaintainedSourceEligibilityDisagreement() throws IOException {
        Path changedCorpus = temporaryDirectory.resolve("corpus");
        Files.createDirectories(changedCorpus);
        Files.copy(CORPUS.resolve("validation-manifest.json"), changedCorpus.resolve("validation-manifest.json"));
        Files.createDirectories(changedCorpus.resolve("sources"));
        try (var sources = Files.list(CORPUS.resolve("sources"))) {
            for (Path source : sources.toList()) {
                Files.copy(source, changedCorpus.resolve("sources").resolve(source.getFileName()));
            }
        }
        String manifest = Files.readString(changedCorpus.resolve("validation-manifest.json"));
        Files.writeString(
                changedCorpus.resolve("validation-manifest.json"),
                manifest.replaceFirst("\"approvalStatus\": \"APPROVED\"", "\"approvalStatus\": \"SUPERSEDED\""));

        assertThatThrownBy(() -> repository(CASES, SCENARIOS, changedCorpus).load(EVALUATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manifest and maintained source disagree");
    }

    private Path copyWithReplacement(Path source, String expected, String replacement) throws IOException {
        String content = Files.readString(source);
        assertThat(content).contains(expected);
        Path copy = temporaryDirectory.resolve(source.getFileName());
        Files.writeString(copy, content.replace(expected, replacement));
        return copy;
    }

    private static SynTenRetrievalEvaluationContractRepository repository(Path cases, Path scenarios, Path corpus) {
        JsonMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        return new SynTenRetrievalEvaluationContractRepository(cases, scenarios, corpus, mapper);
    }

    private static Set<String> expectedCaseIds() {
        Set<String> values = new LinkedHashSet<>();
        IntStream.rangeClosed(1, 23).forEach(number -> values.add("KQ-%03d".formatted(number)));
        return values;
    }

    private static Set<String> expectedScenarioIds() {
        Set<String> values = new LinkedHashSet<>();
        IntStream.rangeClosed(1, 14).forEach(number -> values.add("S%03d".formatted(number)));
        IntStream.rangeClosed(101, 111).forEach(number -> values.add("S%03d".formatted(number)));
        IntStream.rangeClosed(201, 211).forEach(number -> values.add("S%03d".formatted(number)));
        return values;
    }
}
