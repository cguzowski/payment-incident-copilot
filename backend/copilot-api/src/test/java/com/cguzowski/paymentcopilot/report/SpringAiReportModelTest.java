package com.cguzowski.paymentcopilot.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.ThinkOption;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

class SpringAiReportModelTest {

    private static final String MODEL_ID = "test-report-model";

    @Test
    void callsOllamaOnceWithDeterministicSchemaConstrainedOptionsAndNoTools() throws JacksonException {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("{\"result\":true}")))));
        ReportPromptFactory prompts =
                new ReportPromptFactory(JsonMapper.builder().build());
        SpringAiReportModel model = new SpringAiReportModel(Optional.of(chatModel), MODEL_ID, prompts);

        String outputSchema = "{\"type\":\"object\"}";
        ReportModelResponse result = model.generate("prompt", outputSchema);

        assertThat(result.output()).isEqualTo("{\"result\":true}");
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        assertThat(prompt.getValue().getContents()).isEqualTo("prompt");
        OllamaChatOptions options = (OllamaChatOptions) prompt.getValue().getOptions();
        assertThat(options.getModel()).isEqualTo(MODEL_ID);
        assertThat(options.getTemperature()).isZero();
        assertThat(options.getMaxTokens()).isEqualTo(1536);
        assertThat(options.getToolCallbacks()).isNullOrEmpty();
        assertThat(options.getThinkOption()).isEqualTo(ThinkOption.ThinkBoolean.DISABLED);
        assertThat(options.getOutputSchema()).isEqualTo(outputSchema);
    }

    @Test
    void mapsMissingProviderAndTimeoutWithoutLeakingProviderDetails() {
        ReportPromptFactory prompts =
                new ReportPromptFactory(JsonMapper.builder().build());
        assertThatThrownBy(() -> new SpringAiReportModel(Optional.empty(), MODEL_ID, prompts).generate("prompt", "{}"))
                .isInstanceOf(ReportModelUnavailableException.class)
                .hasMessage(null);

        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new RuntimeException(new SocketTimeoutException("provider detail")));

        assertThatThrownBy(() ->
                        new SpringAiReportModel(Optional.of(chatModel), MODEL_ID, prompts).generate("prompt", "{}"))
                .isInstanceOf(ReportModelTimedOutException.class)
                .hasMessage(null);
    }
}
