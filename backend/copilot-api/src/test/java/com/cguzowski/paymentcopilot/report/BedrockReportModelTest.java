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
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class BedrockReportModelTest {

    private static final String MODEL_ID = "global.amazon.nova-2-lite-v1:0";

    @Test
    void callsConverseOnceWithDeterministicBoundedOptionsAndNoTools() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("{\"result\":true}")))));
        BedrockReportModel model = new BedrockReportModel(Optional.of(chatModel), MODEL_ID);

        ReportModelResponse result = model.generate("prompt");

        assertThat(result.output()).isEqualTo("{\"result\":true}");
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        assertThat(prompt.getValue().getContents()).isEqualTo("prompt");
        BedrockChatOptions options = (BedrockChatOptions) prompt.getValue().getOptions();
        assertThat(options.getModel()).isEqualTo(MODEL_ID);
        assertThat(options.getTemperature()).isZero();
        assertThat(options.getMaxTokens()).isEqualTo(4096);
        assertThat(options.getToolCallbacks()).isNullOrEmpty();
        assertThat(options.getOutputSchema()).isNull();
    }

    @Test
    void mapsMissingProviderAndTimeoutWithoutLeakingProviderDetails() {
        assertThatThrownBy(() -> new BedrockReportModel(Optional.empty(), MODEL_ID).generate("prompt"))
                .isInstanceOf(ReportModelUnavailableException.class)
                .hasMessage(null);

        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new RuntimeException(new SocketTimeoutException("provider detail")));

        assertThatThrownBy(() -> new BedrockReportModel(Optional.of(chatModel), MODEL_ID).generate("prompt"))
                .isInstanceOf(ReportModelTimedOutException.class)
                .hasMessage(null);
    }
}
