package com.cguzowski.paymentcopilot.incident;

import java.util.Map;
import java.util.UUID;

@FunctionalInterface
interface RecentServiceErrorsClient {

    Map<String, Object> call(EvidenceCollectionContext context, UUID toolCallId);
}
