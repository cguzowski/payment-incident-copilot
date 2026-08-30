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
    void configuresOneAuditableBedrockModelCallAndABoundedReportDeadline() throws IOException {
        List<PropertySource<?>> localSources =
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        assertThat(localSources).hasSize(1);
        PropertySource<?> local = localSources.getFirst();

        assertThat(local.getProperty("spring.ai.model.chat")).isEqualTo("${AI_MODEL_CHAT:none}");
        assertThat(local.getProperty("spring.ai.model.embedding")).isEqualTo("${AI_MODEL_EMBEDDING:none}");
        assertThat(local.getProperty("spring.ai.retry.max-attempts")).isEqualTo(1);
        assertThat(local.getProperty("app.report.generation-timeout")).isEqualTo("${REPORT_GENERATION_TIMEOUT:2m}");
        assertThat(local.getProperty("spring.ai.bedrock.aws.region")).isEqualTo("${AWS_REGION:eu-central-1}");
        assertThat(local.getProperty("spring.ai.bedrock.titan.embedding.model"))
                .isEqualTo("amazon.titan-embed-text-v2:0");
        assertThat(local.getProperty("spring.ai.bedrock.converse.chat.model"))
                .isEqualTo("${BEDROCK_CHAT_MODEL:global.amazon.nova-2-lite-v1:0}");
        assertThat(local.getProperty("spring.ai.bedrock.converse.chat.temperature"))
                .isEqualTo(0);
        assertThat(local.getProperty("spring.ai.bedrock.converse.chat.max-tokens"))
                .isEqualTo(4096);
        assertThat(local.getProperty("app.report.smoke-test.enabled")).isEqualTo(false);

        String pom = Files.readString(Path.of(System.getProperty("basedir"), "pom.xml"));
        assertThat(pom).contains("spring-ai-starter-model-bedrock");
        assertThat(pom).contains("spring-ai-starter-model-bedrock-converse");
        assertThat(pom).doesNotContain("spring-ai-starter-model-ollama");

        Properties testOverrides =
                PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
        assertThat(testOverrides.getProperty("spring.ai.model.chat")).isEqualTo("none");
        assertThat(testOverrides.getProperty("spring.ai.model.embedding")).isEqualTo("none");
    }
}
