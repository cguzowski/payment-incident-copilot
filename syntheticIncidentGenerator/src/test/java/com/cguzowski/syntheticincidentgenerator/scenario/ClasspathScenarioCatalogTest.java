package com.cguzowski.syntheticincidentgenerator.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ClasspathScenarioCatalogTest {

    @Test
    void addsSixFamiliesWithAvailableAndDegradedEvidence() throws Exception {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        ScenarioCatalog catalog = new ClasspathScenarioCatalog(mapper);
        ScenarioOracleCatalog oracle = new ClasspathScenarioOracleCatalog(mapper);
        for (int index = 1; index <= 6; index++) {
            var available = catalog.findByCode("S30" + index).orElseThrow();
            var degraded = catalog.findByCode("S31" + index).orElseThrow();
            assertThat(available.evidence().availability()).isEqualTo(EvidenceAvailability.AVAILABLE);
            assertThat(available.evidence().errors()).isNotEmpty();
            assertThat(degraded.evidence().availability())
                    .isIn(EvidenceAvailability.PARTIAL, EvidenceAvailability.UNAVAILABLE);
            assertThat(oracle.findByCode(available.code())).isPresent();
            assertThat(oracle.findByCode(degraded.code()).orElseThrow().truth().expectedDisposition())
                    .isEqualTo("INSUFFICIENT_EVIDENCE");
            var json = mapper.readTree(mapper.writeValueAsString(available));
            assertThat(json.get("incidentType").asText()).isNotEqualTo("AUTHORIZATION_DECLINE_RATE_SPIKE");
        }
        assertThat(catalog.all()).hasSize(48);
    }

    @Test
    void loadsBroadReviewedObservableCatalogWithoutOracleFields() throws Exception {
        ScenarioCatalog catalog = new ClasspathScenarioCatalog(
                JsonMapper.builder().findAndAddModules().build());

        assertThat(catalog.all()).hasSizeGreaterThanOrEqualTo(36);
        assertThat(catalog.all())
                .filteredOn(scenario -> scenario.rarity() == ScenarioRarity.COMMON)
                .hasSizeGreaterThanOrEqualTo(14);
        assertThat(catalog.all())
                .filteredOn(scenario -> scenario.rarity() == ScenarioRarity.UNCOMMON)
                .hasSizeGreaterThanOrEqualTo(10);
        assertThat(catalog.all())
                .filteredOn(scenario -> scenario.rarity() == ScenarioRarity.RARE)
                .hasSizeGreaterThanOrEqualTo(10);

        Set<String> codes = catalog.all().stream().map(ScenarioDefinition::code).collect(Collectors.toSet());
        assertThat(codes).hasSameSizeAs(catalog.all());

        assertThat(catalog.all()).allSatisfy(scenario -> {
            assertThat(scenario.code()).matches("S[0-9]{3}");
            assertThat(scenario.title()).isNotBlank().hasSizeLessThanOrEqualTo(500);
            assertThat(scenario.description()).isNotBlank().hasSizeLessThanOrEqualTo(2000);
            assertThat(scenario.evidence().errors()).hasSizeLessThanOrEqualTo(100);
            assertThat(scenario.evidence().errors()).allSatisfy(error -> {
                assertThat(error.errorCode()).isNotBlank().hasSizeLessThanOrEqualTo(120);
                assertThat(error.count()).isBetween(1, 1_000_000);
                assertThat(error.secondsBeforeDetection()).isBetween(0, 300);
            });
        });

        String resource = new String(getClass()
                .getClassLoader()
                .getResourceAsStream("scenarios/catalog.json")
                .readAllBytes());
        assertThat(resource)
                .doesNotContain(
                        "\"truth\"",
                        "\"rootCause\"",
                        "\"expectedDisposition\"",
                        "\"expectedConfidence\"",
                        "\"requiredEvidence\"",
                        "\"recommendation\"");
    }
}
