package com.cguzowski.paymentcopilot.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.ThinkOption;
import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

class SpringAiReportModelTest {
    private static final String MODEL_ID = "test-report-model";

    private SpringAiReportModel model(ChatModel provider) {
        return new SpringAiReportModel(
                Optional.of(provider),
                MODEL_ID,
                new ReportPromptFactory(JsonMapper.builder().build()));
    }

    private ChatResponse fragment(String text, String finishReason) {
        return new ChatResponse(List.of(new Generation(
                new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason(finishReason).build())));
    }

    @Test
    void callsOllamaOnceWithDeterministicSchemaConstrainedOptionsAndNoTools() {
        ChatModel provider = mock(ChatModel.class);
        when(provider.stream(any(Prompt.class)))
                .thenReturn(Flux.just(fragment("{\"result\":", null), fragment("true}", "stop")));
        String outputSchema = "{\"type\":\"object\"}";
        ReportModelResponse result = model(provider).generate("prompt", outputSchema);
        assertThat(result.output()).isEqualTo("{\"result\":true}");
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(provider).stream(prompt.capture());
        verify(provider, never()).call(any(Prompt.class));
        assertThat(prompt.getValue().getContents()).isEqualTo("prompt");
        OllamaChatOptions options = (OllamaChatOptions) prompt.getValue().getOptions();
        assertThat(options.getModel()).isEqualTo(MODEL_ID);
        assertThat(options.getTemperature()).isZero();
        assertThat(options.getMaxTokens()).isEqualTo(1536);
        assertThat(options.getNumCtx()).isEqualTo(8192);
        assertThat(options.getNumBatch()).isEqualTo(128);
        assertThat(options.getToolCallbacks()).isNullOrEmpty();
        assertThat(options.getThinkOption()).isEqualTo(ThinkOption.ThinkBoolean.DISABLED);
        assertThat(options.getOutputSchema()).isEqualTo(outputSchema);
    }

    @Test
    void sendsConfiguredGpuPlacementWithoutChangingModelOrContext() {
        ChatModel provider = mock(ChatModel.class);
        when(provider.stream(any(Prompt.class))).thenReturn(Flux.just(fragment("{}", "stop")));
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(SpringAiReportModel.class)
                .withBean(ChatModel.class, () -> provider)
                .withBean(
                        ReportPromptFactory.class,
                        () -> new ReportPromptFactory(JsonMapper.builder().build()))
                .withPropertyValues(
                        "spring.ai.ollama.chat.model=test-report-model",
                        "app.report.gpu-layers=35",
                        "app.report.batch-tokens=128")
                .run(context -> {
                    SpringAiReportModel model = context.getBean(SpringAiReportModel.class);
                    assertThat(model.settings().gpuLayers()).isEqualTo(35);
                    assertThat(model.settings().batchTokens()).isEqualTo(128);
                    model.generate("exact input", "{}");
                });
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(provider).stream(prompt.capture());
        OllamaChatOptions options = (OllamaChatOptions) prompt.getValue().getOptions();
        assertThat(options.getNumGPU()).isEqualTo(35);
        assertThat(options.getNumBatch()).isEqualTo(128);
        assertThat(options.getNumCtx()).isEqualTo(8192);
        assertThat(options.getMaxTokens()).isEqualTo(1536);
        assertThat(prompt.getValue().getContents()).isEqualTo("exact input");
    }

    @Test
    void rejectsInvalidExecutionSettings() {
        for (String setting : List.of("app.report.gpu-layers=-2", "app.report.batch-tokens=0")) {
            new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                    .withUserConfiguration(SpringAiReportModel.class)
                    .withBean(
                            ReportPromptFactory.class,
                            () -> new ReportPromptFactory(JsonMapper.builder().build()))
                    .withPropertyValues(setting)
                    .run(context -> assertThat(context).hasFailed());
        }
    }

    @Test
    void deadlineCancelsSubscriptionAndDiscardsPartialOutput() throws Exception {
        ChatModel provider = mock(ChatModel.class);
        CountDownLatch cancelled = new CountDownLatch(1);
        when(provider.stream(any(Prompt.class)))
                .thenReturn(Flux.concat(Flux.just(fragment("partial", null)), Flux.<ChatResponse>never())
                        .doOnCancel(cancelled::countDown));
        assertThatThrownBy(() ->
                        new ReportModelCallExecutor(Duration.ofMillis(200)).generate(model(provider), "prompt", "{}"))
                .isInstanceOf(ReportModelTimedOutException.class);
        assertThat(cancelled.await(2, TimeUnit.SECONDS)).isTrue();
        verify(provider).stream(any(Prompt.class));
    }

    @Test
    void prematureCompletionAndMidstreamFailureDiscardPartialOutput() {
        ChatModel provider = mock(ChatModel.class);
        when(provider.stream(any(Prompt.class))).thenReturn(Flux.just(fragment("partial", null)));
        assertThatThrownBy(() -> model(provider).generate("prompt", "{}"))
                .isInstanceOf(ReportModelUnavailableException.class)
                .hasMessage(null);
        when(provider.stream(any(Prompt.class)))
                .thenReturn(Flux.concat(
                        Flux.just(fragment("partial", null)),
                        Flux.error(new IllegalStateException("provider detail"))));
        assertThatThrownBy(() -> model(provider).generate("prompt", "{}"))
                .isInstanceOf(ReportModelUnavailableException.class)
                .hasMessage(null);
    }

    @Test
    void mapsMissingProviderAndTimeoutWithoutLeakingProviderDetails() {
        ReportPromptFactory prompts =
                new ReportPromptFactory(JsonMapper.builder().build());
        assertThatThrownBy(() -> new SpringAiReportModel(Optional.empty(), MODEL_ID, prompts).generate("prompt", "{}"))
                .isInstanceOf(ReportModelUnavailableException.class)
                .hasMessage(null);
        ChatModel provider = mock(ChatModel.class);
        when(provider.stream(any(Prompt.class)))
                .thenReturn(Flux.error(new RuntimeException(new SocketTimeoutException("provider detail"))));
        assertThatThrownBy(() -> model(provider).generate("prompt", "{}"))
                .isInstanceOf(ReportModelTimedOutException.class)
                .hasMessage(null);
    }
}
