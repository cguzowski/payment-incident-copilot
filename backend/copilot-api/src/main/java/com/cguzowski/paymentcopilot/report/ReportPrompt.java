package com.cguzowski.paymentcopilot.report;

record ReportPrompt(
        String text,
        String promptVersion,
        String promptHash,
        String schemaVersion,
        String schemaHash,
        String outputSchema) {

    ReportPrompt(String text, String promptVersion, String promptHash, String schemaVersion, String schemaHash) {
        this(text, promptVersion, promptHash, schemaVersion, schemaHash, "{}");
    }
}
