package com.cguzowski.syntheticincidentgenerator.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ClasspathScenarioCatalogTest {

    @Test
    void loadsBroadReviewedCatalogWithValidDeterministicTruthAndEvidence() {
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
            assertThat(scenario.truth().rootCause()).isNotBlank();
            assertThat(scenario.truth().expectedDisposition()).isIn("PROPOSED", "INSUFFICIENT_EVIDENCE");
            assertThat(scenario.truth().expectedConfidence()).isIn("LOW", "MEDIUM", "HIGH");
            assertThat(scenario.truth().requiredEvidence()).isNotEmpty();
            assertThat(scenario.truth().recommendation()).isNotBlank();
            assertThat(scenario.truth().decisionRule()).contains("Approve").contains("reject");
            assertThat(scenario.evidence().errors()).hasSizeLessThanOrEqualTo(100);
            assertThat(scenario.evidence().errors()).allSatisfy(error -> {
                assertThat(error.errorCode()).isNotBlank().hasSizeLessThanOrEqualTo(120);
                assertThat(error.count()).isBetween(1, 1_000_000);
                assertThat(error.secondsBeforeDetection()).isBetween(0, 300);
            });
        });
    }
}
