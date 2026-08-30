package com.cguzowski.syntheticincidentgenerator.scenario;

import java.util.List;
import java.util.Optional;

public interface ScenarioCatalog {

    List<ScenarioDefinition> all();

    Optional<ScenarioDefinition> findByCode(String code);
}
