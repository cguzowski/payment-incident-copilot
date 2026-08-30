package com.cguzowski.syntheticincidentgenerator.scenario;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
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
public class ClasspathScenarioCatalog implements ScenarioCatalog {

    private static final String CATALOG_PATH = "scenarios/catalog.json";
    private static final Set<String> SEVERITIES = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final Set<String> DISPOSITIONS = Set.of("PROPOSED", "INSUFFICIENT_EVIDENCE");
    private static final Set<String> CONFIDENCES = Set.of("LOW", "MEDIUM", "HIGH");

    private final List<ScenarioDefinition> scenarios;
    private final Map<String, ScenarioDefinition> scenariosByCode;

    public ClasspathScenarioCatalog(JsonMapper jsonMapper) {
        this.scenarios = load(jsonMapper);
        this.scenariosByCode = scenarios.stream()
                .collect(Collectors.toUnmodifiableMap(ScenarioDefinition::code, scenario -> scenario));
    }

    @Override
    public List<ScenarioDefinition> all() {
        return scenarios;
    }

    @Override
    public Optional<ScenarioDefinition> findByCode(String code) {
        return Optional.ofNullable(scenariosByCode.get(code));
    }

    private static List<ScenarioDefinition> load(JsonMapper jsonMapper) {
        try (InputStream input = new ClassPathResource(CATALOG_PATH).getInputStream()) {
            FixtureDocument document = jsonMapper.readValue(input, FixtureDocument.class);
            if (document == null
                    || document.scenarios() == null
                    || document.scenarios().isEmpty()) {
                throw new IllegalStateException("Synthetic incident catalog has no scenarios.");
            }
            List<ScenarioDefinition> scenarios = document.scenarios().stream()
                    .map(ClasspathScenarioCatalog::scenario)
                    .toList();
            scenarios.stream()
                    .collect(Collectors.toUnmodifiableMap(
                            ScenarioDefinition::code, scenario -> scenario, rejectingDuplicates()));
            EnumSet<ScenarioRarity> rarities = scenarios.stream()
                    .map(ScenarioDefinition::rarity)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(ScenarioRarity.class)));
            if (!rarities.equals(EnumSet.allOf(ScenarioRarity.class))) {
                throw new IllegalStateException("Synthetic incident catalog must cover every rarity.");
            }
            return List.copyOf(scenarios);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Could not load synthetic incident catalog.", exception);
        }
    }

    private static BinaryOperator<ScenarioDefinition> rejectingDuplicates() {
        return (first, second) -> {
            throw new IllegalStateException("Synthetic incident catalog contains a duplicate scenario code.");
        };
    }

    private static ScenarioDefinition scenario(FixtureScenario fixture) {
        require(fixture.code() != null && fixture.code().matches("S[0-9]{3}"), "invalid scenario code");
        require(fixture.rarity() != null, "missing rarity");
        require(SEVERITIES.contains(fixture.severity()), "invalid severity");
        require(bounded(fixture.title(), 500), "invalid title");
        require(bounded(fixture.description(), 2000), "invalid description");
        require(fixture.evidence() != null, "missing evidence");
        require(fixture.truth() != null, "missing truth");

        ScenarioEvidence evidence = evidence(fixture.evidence());
        FixtureTruth fixtureTruth = fixture.truth();
        require(bounded(fixtureTruth.rootCause(), 500), "invalid root cause");
        require(DISPOSITIONS.contains(fixtureTruth.expectedDisposition()), "invalid disposition");
        require(CONFIDENCES.contains(fixtureTruth.expectedConfidence()), "invalid confidence");
        require(
                fixtureTruth.requiredEvidence() != null
                        && !fixtureTruth.requiredEvidence().isEmpty()
                        && fixtureTruth.requiredEvidence().stream().allMatch(item -> bounded(item, 500)),
                "invalid required evidence");
        require(bounded(fixtureTruth.recommendation(), 1000), "invalid recommendation");
        String decisionRule = "Approve only if the proposed report matches this root cause, disposition, confidence, "
                + "required evidence, and safe recommendation; otherwise reject it.";
        ScenarioTruth truth = new ScenarioTruth(
                fixtureTruth.rootCause(),
                fixtureTruth.expectedDisposition(),
                fixtureTruth.expectedConfidence(),
                List.copyOf(fixtureTruth.requiredEvidence()),
                fixtureTruth.recommendation(),
                decisionRule);
        return new ScenarioDefinition(
                fixture.code(),
                fixture.rarity(),
                fixture.severity(),
                fixture.title(),
                fixture.description(),
                evidence,
                truth);
    }

    private static ScenarioEvidence evidence(FixtureEvidence fixture) {
        require(fixture.availability() != null, "missing evidence availability");
        boolean hasContent = fixture.availability() == EvidenceAvailability.AVAILABLE
                || fixture.availability() == EvidenceAvailability.PARTIAL;
        require(!hasContent || bounded(fixture.serviceName(), 120), "invalid evidence service");
        require(hasContent || fixture.serviceName() == null, "unexpected unavailable evidence content");
        require(fixture.statusDetail() == null || bounded(fixture.statusDetail(), 500), "invalid status detail");
        require(fixture.errors() != null && fixture.errors().size() <= 100, "invalid evidence errors");
        require(hasContent || fixture.errors().isEmpty(), "unexpected unavailable evidence errors");
        List<ScenarioError> errors =
                fixture.errors().stream().map(ClasspathScenarioCatalog::error).toList();
        return new ScenarioEvidence(
                fixture.availability(), fixture.statusDetail(), fixture.serviceName(), List.copyOf(errors));
    }

    private static ScenarioError error(FixtureError fixture) {
        require(bounded(fixture.errorCode(), 120), "invalid error code");
        require(fixture.count() >= 1 && fixture.count() <= 1_000_000, "invalid error count");
        require(
                fixture.secondsBeforeDetection() >= 0 && fixture.secondsBeforeDetection() <= 300,
                "invalid error timestamp offset");
        return new ScenarioError(fixture.errorCode(), fixture.count(), fixture.secondsBeforeDetection());
    }

    private static boolean bounded(String value, int maximum) {
        return value != null && !value.isBlank() && value.length() <= maximum;
    }

    private static void require(boolean condition, String detail) {
        if (!condition) {
            throw new IllegalStateException("Synthetic incident catalog has " + detail + ".");
        }
    }

    private record FixtureDocument(List<FixtureScenario> scenarios) {}

    private record FixtureScenario(
            String code,
            ScenarioRarity rarity,
            String severity,
            String title,
            String description,
            FixtureEvidence evidence,
            FixtureTruth truth) {}

    private record FixtureEvidence(
            EvidenceAvailability availability, String statusDetail, String serviceName, List<FixtureError> errors) {}

    private record FixtureError(String errorCode, int count, int secondsBeforeDetection) {}

    private record FixtureTruth(
            String rootCause,
            String expectedDisposition,
            String expectedConfidence,
            List<String> requiredEvidence,
            String recommendation) {}
}
