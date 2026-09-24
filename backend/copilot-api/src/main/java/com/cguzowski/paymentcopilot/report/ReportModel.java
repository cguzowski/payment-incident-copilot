package com.cguzowski.paymentcopilot.report;

interface ReportModel {

    String modelId();

    ReportModelResponse generate(String prompt);

    default ReportModelResponse generate(String prompt, String outputSchema) {
        return generate(prompt);
    }
}
