package com.cguzowski.paymentcopilot.report;

final class ReportGenerationSettings {

    static final int TEMPERATURE = 0;
    static final int MAX_OUTPUT_TOKENS = 1_536;
    static final int CONTEXT_TOKENS = 8_192;
    static final ReportModelSettings MODEL_SETTINGS =
            new ReportModelSettings("report-model-settings/v2", CONTEXT_TOKENS, true, false, false);

    private ReportGenerationSettings() {}
}
