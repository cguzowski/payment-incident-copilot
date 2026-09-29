package com.cguzowski.syntheticincidentgenerator.scenario;

import java.util.List;
import java.util.Optional;

public interface ScenarioOracleCatalog {
    String version();

    List<ScenarioOracleEntry> all();

    Optional<ScenarioOracleEntry> findByCode(String code);
}
