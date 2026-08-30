package com.cguzowski.syntheticincidentgenerator.scenario;

public record ScenarioError(String errorCode, int count, int secondsBeforeDetection) {}
