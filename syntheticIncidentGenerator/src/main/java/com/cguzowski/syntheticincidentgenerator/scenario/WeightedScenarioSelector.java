package com.cguzowski.syntheticincidentgenerator.scenario;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class WeightedScenarioSelector implements ScenarioSelector {

    private final Map<ScenarioRarity, List<ScenarioDefinition>> scenariosByRarity;

    public WeightedScenarioSelector(ScenarioCatalog catalog) {
        EnumMap<ScenarioRarity, List<ScenarioDefinition>> grouped = new EnumMap<>(ScenarioRarity.class);
        for (ScenarioRarity rarity : ScenarioRarity.values()) {
            List<ScenarioDefinition> scenarios = catalog.all().stream()
                    .filter(scenario -> scenario.rarity() == rarity)
                    .toList();
            if (scenarios.isEmpty()) {
                throw new IllegalStateException("Scenario catalog has no " + rarity + " scenarios.");
            }
            grouped.put(rarity, scenarios);
        }
        this.scenariosByRarity = Map.copyOf(grouped);
    }

    @Override
    public ScenarioDefinition select() {
        return selectForRolls(
                ThreadLocalRandom.current().nextInt(100),
                ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));
    }

    ScenarioDefinition selectForRolls(int rarityRoll, int itemRoll) {
        if (rarityRoll < 0 || rarityRoll >= 100) {
            throw new IllegalArgumentException("Rarity roll must be between 0 and 99.");
        }
        ScenarioRarity rarity = rarityRoll < 70
                ? ScenarioRarity.COMMON
                : rarityRoll < 95 ? ScenarioRarity.UNCOMMON : ScenarioRarity.RARE;
        List<ScenarioDefinition> candidates = scenariosByRarity.get(rarity);
        return candidates.get(Math.floorMod(itemRoll, candidates.size()));
    }
}
