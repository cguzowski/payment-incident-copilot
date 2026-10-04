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
                .temperature((double) ReportGenerationSettings.TEMPERATURE)
                .maxTokens(ReportGenerationSettings.MAX_OUTPUT_TOKENS)
                .numCtx(ReportGenerationSettings.CONTEXT_TOKENS)
                .disableThinking()
                .outputSchema(outputSchema)
                .build();
        try {
            var fragments = provider.stream(new Prompt(promptText, options))
                    .collectList()
                    .block();
            if (fragments == null || fragments.isEmpty()) {
                throw new ReportModelUnavailableException();
            }
            ChatResponse terminal = fragments.getLast();
            if (terminal.getResult() == null
                    || !"stop"
                            .equalsIgnoreCase(terminal.getResult().getMetadata().getFinishReason())) {
                throw new ReportModelUnavailableException();
            }
            StringBuilder output = new StringBuilder();
            for (ChatResponse fragment : fragments) {
                if (fragment.getResult() != null
                        && fragment.getResult().getOutput().getText() != null) {
                    output.append(fragment.getResult().getOutput().getText());
                }
            }
            return new ReportModelResponse(
                    output.toString(), terminal.getMetadata().getId());
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
