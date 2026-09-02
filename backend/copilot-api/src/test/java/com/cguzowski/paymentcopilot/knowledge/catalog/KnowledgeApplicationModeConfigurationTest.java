package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class KnowledgeApplicationModeConfigurationTest {

    private final KnowledgeIngestionService ingestionService = mock(KnowledgeIngestionService.class);
    private final SynTenPdfCatalogImportService catalogImportService = mock(SynTenPdfCatalogImportService.class);
    private final SynTenPdfEmbeddingService embeddingService = mock(SynTenPdfEmbeddingService.class);
    private final KnowledgeEmbeddingClient embeddingClient = mock(KnowledgeEmbeddingClient.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CommandConfiguration.class)
            .withBean(KnowledgeIngestionService.class, () -> ingestionService)
            .withBean(SynTenPdfCatalogImportService.class, () -> catalogImportService)
            .withBean(SynTenPdfEmbeddingService.class, () -> embeddingService)
            .withBean(KnowledgeEmbeddingClient.class, () -> embeddingClient);

    @Test
    void normalStartupWithModelProvidersNoneCreatesNoKnowledgeCommandAndMakesNoProviderCall() {
        contextRunner
                .withPropertyValues(
                        "spring.ai.model.chat=none",
                        "spring.ai.model.embedding=none",
                        "app.knowledge.ingestion.enabled=false",
                        "app.knowledge.pdf-catalog.enabled=false",
                        "app.knowledge.pdf-backfill.enabled=false",
                        "app.knowledge.retrieval-evaluation.enabled=false",
                        "app.knowledge.embedding-smoke-test.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(KnowledgeIngestionCommand.class);
                    assertThat(context).doesNotHaveBean(SynTenPdfCatalogImportCommand.class);
                    assertThat(context).doesNotHaveBean(SynTenPdfEmbeddingBackfillCommand.class);
                    assertThat(context).doesNotHaveBean(KnowledgeEmbeddingSmokeTestCommand.class);
                    verifyNoInteractions(ingestionService, catalogImportService, embeddingService, embeddingClient);
                });
    }

    @Test
    void createsOnlyTheExplicitlyEnabledBackfillCommand() {
        contextRunner
                .withPropertyValues("app.knowledge.pdf-backfill.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SynTenPdfEmbeddingBackfillCommand.class);
                    assertThat(context).doesNotHaveBean(KnowledgeIngestionCommand.class);
                    assertThat(context).doesNotHaveBean(SynTenPdfCatalogImportCommand.class);
                });
    }

    @Test
    void rejectsCatalogImportAndBackfillTogether() {
        contextRunner
                .withPropertyValues("app.knowledge.pdf-catalog.enabled=true", "app.knowledge.pdf-backfill.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasStackTraceContaining("Only one explicit knowledge command mode may be enabled");
                });
    }

    @Test
    void rejectsEvaluationAndBackfillTogether() {
        contextRunner
                .withPropertyValues(
                        "app.knowledge.retrieval-evaluation.enabled=true", "app.knowledge.pdf-backfill.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasStackTraceContaining("Only one explicit knowledge command mode may be enabled");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
        KnowledgeApplicationModeValidator.class,
        KnowledgeIngestionCommand.class,
        SynTenPdfCatalogImportCommand.class,
        SynTenPdfEmbeddingBackfillCommand.class,
        KnowledgeEmbeddingSmokeTestCommand.class
    })
    static class CommandConfiguration {}
}
