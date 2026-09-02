package com.cguzowski.paymentcopilot.report;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ReportGenerationSmokeTestCommandTest {

    private static final UUID EVIDENCE_ID = ReportGenerationSmokeTestCommand.EVIDENCE_ID;

    @Test
    void validatesOnePromptGuidedOllamaReportWithoutPersistence() {
        ReportModel model = mock(ReportModel.class);
        when(model.modelId()).thenReturn("test-report-model");
        when(model.generate(contains("report-v1")))
                .thenReturn(new ReportModelResponse(validInsufficientReportJson(), "safe-request-id"));
        JsonMapper mapper = JsonMapper.builder().build();
        ReportPromptFactory prompts = new ReportPromptFactory(mapper);

        assertThatCode(() -> new ReportGenerationSmokeTestCommand(
                                model, prompts, new ReportOutputParser(mapper, prompts))
                        .run(null))
                .doesNotThrowAnyException();
    }

    @Test
    void failsSafelyWhenProviderOutputDoesNotValidate() {
        ReportModel model = mock(ReportModel.class);
        when(model.generate(contains("report-v1"))).thenReturn(new ReportModelResponse("untrusted", null));
        JsonMapper mapper = JsonMapper.builder().build();
        ReportPromptFactory prompts = new ReportPromptFactory(mapper);

        assertThatThrownBy(() -> new ReportGenerationSmokeTestCommand(
                                model, prompts, new ReportOutputParser(mapper, prompts))
                        .run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Report generation smoke test failed safely.");
    }

    private static String validInsufficientReportJson() {
        return """
                {
                  "disposition":"INSUFFICIENT_EVIDENCE",
                  "summary":{"statement":"The synthetic inputs are insufficient.","evidenceIds":["%s"],"knowledgeChunkIds":[]},
                  "observations":[{"statement":"A synthetic timeout was observed.","evidenceIds":["%s"],"knowledgeChunkIds":[]}],
                  "inferences":[],"probableCause":null,
                  "confidence":{"level":"LOW","rationale":"Only one synthetic observation is available.","evidenceIds":["%s"]},
                  "recommendation":null,"contradictions":[],
                  "evidenceGaps":[{"description":"No approved knowledge matched."}]
                }
                """.formatted(EVIDENCE_ID, EVIDENCE_ID, EVIDENCE_ID);
    }
}
