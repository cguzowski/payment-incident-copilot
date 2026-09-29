package com.cguzowski.syntheticincidentgenerator.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScenarioOracleDependencyTest {

    @Test
    void onlyTheOracleLoaderAndTerminalRevealServiceDependOnTheOracleCatalog() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java");
        Set<String> dependents;
        try (var sources = Files.walk(sourceRoot)) {
            dependents = sources.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> read(path).contains("ScenarioOracleCatalog"))
                    .map(path -> sourceRoot.relativize(path).toString().replace('\\', '/'))
                    .collect(java.util.stream.Collectors.toSet());
        }

        assertThat(dependents)
                .containsExactlyInAnyOrder(
                        "com/cguzowski/syntheticincidentgenerator/scenario/ScenarioOracleCatalog.java",
                        "com/cguzowski/syntheticincidentgenerator/scenario/ClasspathScenarioOracleCatalog.java",
                        "com/cguzowski/syntheticincidentgenerator/generation/AnswerKeyRevealService.java");
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
