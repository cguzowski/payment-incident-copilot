package com.cguzowski.syntheticincidentgenerator.comparison;

record ComparisonGrade(
        int disposition,
        int confidence,
        int rootCause,
        String rootCauseReason,
        int recommendation,
        String recommendationReason,
        int reportScore,
        String reportBand,
        String expectedDecision,
        String actualDecision,
        int decisionScore,
        String decisionBand) {}
