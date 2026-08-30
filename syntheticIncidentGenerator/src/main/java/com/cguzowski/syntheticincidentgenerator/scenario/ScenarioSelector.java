package com.cguzowski.syntheticincidentgenerator.scenario;

@FunctionalInterface
public interface ScenarioSelector {

    ScenarioDefinition select();
}
