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

    private final List<ScenarioDefinition> scenarios;
    private final Map<String, ScenarioDefinition> scenariosByCode;

    public ClasspathScenarioCatalog(JsonMapper jsonMapper) {
        this.scenarios = java.util.stream.Stream.concat(
                        load(jsonMapper, CATALOG_PATH).stream(),
                        load(jsonMapper, "scenarios/multi-incidents/v1/catalog.json").stream())
                .toList();
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

    private static List<ScenarioDefinition> load(JsonMapper jsonMapper, String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
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
            if (path.equals(CATALOG_PATH) && !rarities.equals(EnumSet.allOf(ScenarioRarity.class))) {
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

        require(
                fixture.incidentType() == null
                        || Set.of(
                                        "AUTHORIZATION_DECLINE_RATE_SPIKE",
                                        "AUTHORIZATION_TIMEOUT_SPIKE",
                                        "CAPTURE_FAILURE_SPIKE",
                                        "REFUND_FAILURE_SPIKE",
                                        "SETTLEMENT_DELAY",
                                        "WEBHOOK_DELIVERY_FAILURE",
                                        "RECONCILIATION_MISMATCH")
                                .contains(fixture.incidentType()),
                "invalid incident type");
        ScenarioEvidence evidence = evidence(fixture.evidence());
        return new ScenarioDefinition(
                fixture.code(),
                fixture.rarity(),
                fixture.severity(),
                fixture.title(),
                fixture.description(),
                evidence,
                fixture.incidentType() == null ? "AUTHORIZATION_DECLINE_RATE_SPIKE" : fixture.incidentType());
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
            String incidentType) {}

    private record FixtureEvidence(
            EvidenceAvailability availability, String statusDetail, String serviceName, List<FixtureError> errors) {}

    private record FixtureError(String errorCode, int count, int secondsBeforeDetection) {}
}
