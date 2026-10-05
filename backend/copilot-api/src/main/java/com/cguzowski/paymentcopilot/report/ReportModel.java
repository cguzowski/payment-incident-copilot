package com.cguzowski.paymentcopilot.report;

interface ReportModel {

    String modelId();

    default ReportModelSettings settings() {
        return ReportGenerationSettings.MODEL_SETTINGS;
    }

    ReportModelResponse generate(String prompt);

    default ReportModelResponse generate(String prompt, String outputSchema) {
        return generate(prompt);
    }
}
