package com.cguzowski.paymentcopilot.report;

import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class SpringAiReportModel implements ReportModel {

    private final Optional<ChatModel> chatModel;
    private final String modelId;

    SpringAiReportModel(
            Optional<ChatModel> chatModel, @Value("${spring.ai.ollama.chat.model:unconfigured}") String modelId) {
        this.chatModel = chatModel;
        this.modelId = modelId;
    }

    @Override
    public String modelId() {
        return modelId;
    }

    @Override
    public ReportModelResponse generate(String promptText) {
        ChatModel provider = chatModel.orElseThrow(ReportModelUnavailableException::new);
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(modelId)
                .temperature(0.0)
                .maxTokens(4_096)
                .build();
        try {
            ChatResponse response = provider.call(new Prompt(promptText, options));
            String output = response == null || response.getResult() == null
                    ? null
                    : response.getResult().getOutput().getText();
            String requestId = response == null || response.getMetadata() == null
                    ? null
                    : response.getMetadata().getId();
            return new ReportModelResponse(output, requestId);
        } catch (RuntimeException exception) {
            if (hasTimeoutCause(exception)) {
                throw new ReportModelTimedOutException();
            }
            throw new ReportModelUnavailableException();
        }
    }

    private static boolean hasTimeoutCause(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof TimeoutException || current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
