package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;

class SynTenCorpusSourceRepositorySpringContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("app.knowledge.pdf-catalog.corpus-root=C:/synthetic-corpus")
            .withBean(JsonMapper.class, () -> JsonMapper.builder().build())
            .withUserConfiguration(RepositoryConfiguration.class);

    @Test
    void createsTheRepositoryWithItsConfiguredProductionConstructor() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SynTenCorpusSourceRepository.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(SynTenCorpusSourceRepository.class)
    static class RepositoryConfiguration {}
}
