package com.cguzowski.paymentcopilot.report;

interface ReportModel {

    String modelId();

    ReportModelResponse generate(String prompt);
}
