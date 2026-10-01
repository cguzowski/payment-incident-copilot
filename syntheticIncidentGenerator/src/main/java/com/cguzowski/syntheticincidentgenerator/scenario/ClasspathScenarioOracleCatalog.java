package com.cguzowski.syntheticincidentgenerator.scenario;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class ClasspathScenarioOracleCatalog implements ScenarioOracleCatalog {

    private static final String ORACLE_PATH = "scenarios/oracle.json";
    private static final String ORACLE_VERSION = "scenario-oracle/v1";
    private static final Set<String> DISPOSITIONS = Set.of("PROPOSED", "INSUFFICIENT_EVIDENCE");
    private static final Set<String> CONFIDENCES = Set.of("LOW", "MEDIUM", "HIGH");

    private final List<ScenarioOracleEntry> entries;
    private final Map<String, ScenarioOracleEntry> entriesByCode;

    public ClasspathScenarioOracleCatalog(JsonMapper jsonMapper) {
        this.entries = java.util.stream.Stream.concat(
                        load(jsonMapper, ORACLE_PATH).stream(),
                        load(jsonMapper, "scenarios/multi-incidents/v1/oracle.json").stream())
                .toList();
        this.entriesByCode =
                entries.stream().collect(Collectors.toUnmodifiableMap(ScenarioOracleEntry::code, entry -> entry));
    }

    @Override
    public String version() {
        return ORACLE_VERSION;
    }

    @Override
    public List<ScenarioOracleEntry> all() {
        return entries;
    }

    @Override
    public Optional<ScenarioOracleEntry> findByCode(String code) {
        return Optional.ofNullable(entriesByCode.get(code));
    }

    private static List<ScenarioOracleEntry> load(JsonMapper jsonMapper, String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            FixtureDocument document = jsonMapper.readValue(input, FixtureDocument.class);
            require(document != null && ORACLE_VERSION.equals(document.version()), "invalid version");
            require(document.scenarios() != null && !document.scenarios().isEmpty(), "no scenarios");
            List<ScenarioOracleEntry> entries = document.scenarios().stream()
                    .map(ClasspathScenarioOracleCatalog::entry)
                    .toList();
            entries.stream()
                    .collect(Collectors.toUnmodifiableMap(
                            ScenarioOracleEntry::code, entry -> entry, rejectingDuplicates()));
            return List.copyOf(entries);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Could not load synthetic incident oracle.", exception);
        }
    }

    private static ScenarioOracleEntry entry(FixtureScenario fixture) {
        require(fixture.code() != null && fixture.code().matches("S[0-9]{3}"), "invalid scenario code");
        FixtureTruth truth = fixture.truth();
        require(truth != null, "missing truth");
        require(bounded(truth.rootCause(), 500), "invalid root cause");
        require(DISPOSITIONS.contains(truth.expectedDisposition()), "invalid disposition");
        require(CONFIDENCES.contains(truth.expectedConfidence()), "invalid confidence");
        require(
                truth.requiredEvidence() != null
                        && !truth.requiredEvidence().isEmpty()
                        && truth.requiredEvidence().stream().allMatch(item -> bounded(item, 500)),
                "invalid required evidence");
        require(bounded(truth.recommendation(), 1000), "invalid recommendation");
        String decisionRule = "Approve only if the proposed report matches this root cause, disposition, confidence, "
                + "required evidence, and safe recommendation; otherwise reject it.";
        return new ScenarioOracleEntry(
                fixture.code(),
                new ScenarioTruth(
                        truth.rootCause(),
                        truth.expectedDisposition(),
                        truth.expectedConfidence(),
                        List.copyOf(truth.requiredEvidence()),
                        truth.recommendation(),
                        decisionRule));
    }

    private static BinaryOperator<ScenarioOracleEntry> rejectingDuplicates() {
        return (first, second) -> {
            throw new IllegalStateException("Synthetic incident oracle contains a duplicate scenario code.");
        };
    }

    private static boolean bounded(String value, int maximum) {
        return value != null && !value.isBlank() && value.length() <= maximum;
    }

    private static void require(boolean condition, String detail) {
        if (!condition) {
            throw new IllegalStateException("Synthetic incident oracle has " + detail + ".");
        }
    }

    private record FixtureDocument(String version, List<FixtureScenario> scenarios) {}

    private record FixtureScenario(String code, FixtureTruth truth) {}

    private record FixtureTruth(
            String rootCause,
            String expectedDisposition,
            String expectedConfidence,
            List<String> requiredEvidence,
            String recommendation) {}
}
