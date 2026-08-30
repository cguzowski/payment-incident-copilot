package com.cguzowski.syntheticincidentgenerator.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BatchLauncherContractTest {

    @Test
    void startsGeneratorLastFromRootLauncherWaitsForHealthAndOpensDefaultBrowser() throws IOException {
        Path workingDirectory = Path.of("").toAbsolutePath();
        Path repositoryRoot = Files.isDirectory(workingDirectory.resolve("syntheticIncidentGenerator"))
                ? workingDirectory
                : workingDirectory.getParent();
        Path separateLauncher = repositoryRoot.resolve("syntheticIncidentGenerator/start-generator.bat");
        String launcher = Files.readString(repositoryRoot.resolve("start-local.bat"));

        assertThat(Files.exists(separateLauncher)).isFalse();
        assertThat(launcher)
                .contains("scripts\\start-local.ps1")
                .contains("if /I \"%~1\"==\"--CheckOnly\" goto startup_succeeded")
                .contains("pushd \"%~dp0syntheticIncidentGenerator\"")
                .contains("set \"GENERATOR_URL=http://localhost:8082/\"")
                .contains("..\\mvnw.cmd")
                .contains("-f .\\pom.xml spring-boot:run")
                .contains("actuator/health")
                .contains("start \"\" \"%GENERATOR_URL%\"")
                .doesNotContain("C:\\Users\\", "POSTGRES_PASSWORD", "SPRING_DATASOURCE_PASSWORD");
        assertThat(launcher.indexOf("scripts\\start-local.ps1"))
                .isLessThan(launcher.indexOf("set \"GENERATOR_URL=http://localhost:8082/\""));
    }
}
