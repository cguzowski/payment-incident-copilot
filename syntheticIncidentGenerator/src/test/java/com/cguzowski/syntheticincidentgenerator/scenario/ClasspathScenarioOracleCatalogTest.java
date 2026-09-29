package com.cguzowski.syntheticincidentgenerator.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ClasspathScenarioOracleCatalogTest {

    @Test
    void loadsACompleteVersionedOracleSeparateFromObservableFixtures() {
        ScenarioOracleCatalog oracle = new ClasspathScenarioOracleCatalog(
                JsonMapper.builder().findAndAddModules().build());

        assertThat(oracle.version()).isEqualTo("scenario-oracle/v1");
        assertThat(oracle.all()).hasSizeGreaterThanOrEqualTo(36);
        Set<String> codes = oracle.all().stream().map(ScenarioOracleEntry::code).collect(Collectors.toSet());
        assertThat(codes).hasSameSizeAs(oracle.all());
        assertThat(oracle.all()).allSatisfy(entry -> {
            assertThat(entry.code()).matches("S[0-9]{3}");
            assertThat(entry.truth().rootCause()).isNotBlank();
            assertThat(entry.truth().expectedDisposition()).isIn("PROPOSED", "INSUFFICIENT_EVIDENCE");
            assertThat(entry.truth().expectedConfidence()).isIn("LOW", "MEDIUM", "HIGH");
            assertThat(entry.truth().requiredEvidence()).isNotEmpty();
            assertThat(entry.truth().recommendation()).isNotBlank();
            assertThat(entry.truth().decisionRule()).contains("Approve").contains("reject");
        });
    }
}
