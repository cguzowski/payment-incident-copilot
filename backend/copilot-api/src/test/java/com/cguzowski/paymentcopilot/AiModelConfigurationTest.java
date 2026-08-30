package com.cguzowski.paymentcopilot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

class AiModelConfigurationTest {

    @Test
    void configuresOneAuditableModelCallAndABoundedReportDeadline() throws IOException {
        List<PropertySource<?>> localSources =
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        assertThat(localSources).hasSize(1);
        PropertySource<?> local = localSources.getFirst();

        assertThat(local.getProperty("spring.ai.model.chat")).isEqualTo("ollama");
        assertThat(local.getProperty("spring.ai.model.embedding")).isEqualTo("ollama");
        assertThat(local.getProperty("spring.ai.ollama.base-url")).isEqualTo("http://localhost:11434");
        assertThat(local.getProperty("spring.ai.ollama.chat.model")).isEqualTo("qwen3.5:4b");
        assertThat(local.getProperty("spring.ai.ollama.chat.temperature")).isEqualTo(0);
        assertThat(local.getProperty("spring.ai.ollama.embedding.model")).isEqualTo("nomic-embed-text");
        assertThat(local.getProperty("spring.ai.retry.max-attempts")).isEqualTo(1);
        assertThat(local.getProperty("app.report.generation-timeout")).isEqualTo("${REPORT_GENERATION_TIMEOUT:2m}");
        assertThat(local.getProperty("spring.ai.bedrock.aws.region")).isNull();
        assertThat(local.getProperty("app.report.smoke-test.enabled")).isEqualTo(false);

        String pom = Files.readString(Path.of(System.getProperty("basedir"), "pom.xml"));
        assertThat(pom).contains("spring-ai-starter-model-ollama");
        assertThat(pom).doesNotContain("spring-ai-starter-model-bedrock-converse");

        Properties testOverrides =
                PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
        assertThat(testOverrides.getProperty("spring.ai.model.chat")).isEqualTo("none");
        assertThat(testOverrides.getProperty("spring.ai.model.embedding")).isEqualTo("none");
    }
}
