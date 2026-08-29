package com.cguzowski.paymentcopilot.report;

public enum ReportGenerationStatus {
    STARTED,
    AVAILABLE,
    UNAVAILABLE,
    TIMED_OUT,
    MALFORMED;

    boolean isTerminal() {
        return this != STARTED;
    }
}
