package com.cguzowski.syntheticincidentgenerator.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BatchLauncherContractTest {

    @Test
    void startsGeneratorBeforeCopilotWithItsMcpUrlAndOpensDefaultBrowser() throws IOException {
        Path workingDirectory = Path.of("").toAbsolutePath();
        Path repositoryRoot = Files.isDirectory(workingDirectory.resolve("syntheticIncidentGenerator"))
                ? workingDirectory
                : workingDirectory.getParent();
        Path separateLauncher = repositoryRoot.resolve("syntheticIncidentGenerator/start-generator.bat");
        String launcher = Files.readString(repositoryRoot.resolve("start-local.bat"));
        String powershellLauncher = Files.readString(repositoryRoot.resolve("scripts/start-local.ps1"));

        assertThat(Files.exists(separateLauncher)).isFalse();
        assertThat(launcher)
                .contains("scripts\\start-local.ps1")
                .contains("if /I \"%~1\"==\"--CheckOnly\" goto startup_succeeded")
                .contains("pushd \"%~dp0syntheticIncidentGenerator\"")
                .contains("set \"GENERATOR_URL=http://localhost:8082/\"")
                .contains("set \"OPERATIONS_MCP_BASE_URL=http://localhost:8082\"")
                .contains("scripts\\start-local.ps1\" -UseGeneratorMcp")
                .contains("..\\mvnw.cmd")
                .contains("-f .\\pom.xml spring-boot:run")
                .contains("actuator/health")
                .contains("start \"\" \"%GENERATOR_URL%\"")
                .doesNotContain("C:\\Users\\", "POSTGRES_PASSWORD", "SPRING_DATASOURCE_PASSWORD");
        assertThat(launcher.indexOf("set \"GENERATOR_URL=http://localhost:8082/\""))
                .isLessThan(launcher.indexOf("scripts\\start-local.ps1"));
        assertThat(launcher.indexOf("set \"OPERATIONS_MCP_BASE_URL=http://localhost:8082\""))
                .isLessThan(launcher.indexOf("scripts\\start-local.ps1"));
        assertThat(powershellLauncher)
                .contains("[switch] $UseGeneratorMcp")
                .contains("if ($UseGeneratorMcp)")
                .contains(
                        "[Environment]::SetEnvironmentVariable('OPERATIONS_MCP_BASE_URL', 'http://localhost:8082', 'Process')");
        assertThat(powershellLauncher.indexOf("Import-DotEnv -Path"))
                .isLessThan(powershellLauncher.indexOf("if ($UseGeneratorMcp)"));
    }
}
