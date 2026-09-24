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
    private final ReportPromptFactory prompts;

    SpringAiReportModel(
            Optional<ChatModel> chatModel,
            @Value("${spring.ai.ollama.chat.model:unconfigured}") String modelId,
            ReportPromptFactory prompts) {
        this.chatModel = chatModel;
        this.modelId = modelId;
        this.prompts = prompts;
    }

    @Override
    public String modelId() {
        return modelId;
    }

    @Override
    public ReportModelResponse generate(String promptText) {
        return generate(promptText, prompts.schema());
    }

    @Override
    public ReportModelResponse generate(String promptText, String outputSchema) {
        ChatModel provider = chatModel.orElseThrow(ReportModelUnavailableException::new);
        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(modelId)
                .temperature(0.0)
                .maxTokens(1_536)
                .disableThinking()
                .outputSchema(outputSchema)
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
