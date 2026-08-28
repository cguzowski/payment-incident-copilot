package com.cguzowski.paymentcopilot.mcp;

record RecentServiceErrorScenario(
        EvidenceAvailabilityStatus status,
        String statusDetail,
        RecentServiceErrorsContent content) {
}
