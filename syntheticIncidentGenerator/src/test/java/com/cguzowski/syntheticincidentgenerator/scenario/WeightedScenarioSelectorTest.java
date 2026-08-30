package com.cguzowski.syntheticincidentgenerator.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class WeightedScenarioSelectorTest {

    @Test
    void mapsRarityRollsToDocumentedSeventyTwentyFiveFiveDistribution() {
        ScenarioDefinition common = scenario("S001", ScenarioRarity.COMMON);
        ScenarioDefinition uncommon = scenario("S101", ScenarioRarity.UNCOMMON);
        ScenarioDefinition rare = scenario("S201", ScenarioRarity.RARE);
        WeightedScenarioSelector selector = new WeightedScenarioSelector(catalog(common, uncommon, rare));

        assertThat(selector.selectForRolls(0, 0)).isEqualTo(common);
        assertThat(selector.selectForRolls(69, 0)).isEqualTo(common);
        assertThat(selector.selectForRolls(70, 0)).isEqualTo(uncommon);
        assertThat(selector.selectForRolls(94, 0)).isEqualTo(uncommon);
        assertThat(selector.selectForRolls(95, 0)).isEqualTo(rare);
        assertThat(selector.selectForRolls(99, 0)).isEqualTo(rare);
    }

    @Test
    void itemRollSelectsAcrossTheChosenRarityWithoutBiasingCatalogOrder() {
        ScenarioDefinition first = scenario("S001", ScenarioRarity.COMMON);
        ScenarioDefinition second = scenario("S002", ScenarioRarity.COMMON);
        WeightedScenarioSelector selector = new WeightedScenarioSelector(catalog(
                first, second, scenario("S101", ScenarioRarity.UNCOMMON), scenario("S201", ScenarioRarity.RARE)));

        assertThat(selector.selectForRolls(10, 0)).isEqualTo(first);
        assertThat(selector.selectForRolls(10, 1)).isEqualTo(second);
        assertThat(selector.selectForRolls(10, 3)).isEqualTo(second);
    }

    private static ScenarioCatalog catalog(ScenarioDefinition... scenarios) {
        List<ScenarioDefinition> values = List.of(scenarios);
        return new ScenarioCatalog() {
            @Override
            public List<ScenarioDefinition> all() {
                return values;
            }

            @Override
            public java.util.Optional<ScenarioDefinition> findByCode(String code) {
                return values.stream()
                        .filter(value -> value.code().equals(code))
                        .findFirst();
            }
        };
    }

    private static ScenarioDefinition scenario(String code, ScenarioRarity rarity) {
        return new ScenarioDefinition(
                code,
                rarity,
                "HIGH",
                "Title",
                "Description",
                new ScenarioEvidence(
                        EvidenceAvailability.AVAILABLE,
                        null,
                        "payment-authorization",
                        List.of(new ScenarioError("TEST_ERROR", 1, 30))),
                new ScenarioTruth(
                        "Root cause",
                        "PROPOSED",
                        "HIGH",
                        List.of("TEST_ERROR"),
                        "Escalate for review.",
                        "Approve if correct; otherwise reject."));
    }
}
