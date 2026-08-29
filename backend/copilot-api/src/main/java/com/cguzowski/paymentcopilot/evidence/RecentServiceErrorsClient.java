package com.cguzowski.paymentcopilot.evidence;

import com.cguzowski.paymentcopilot.incident.EvidenceCollectionContext;
import java.util.Map;
import java.util.UUID;

@FunctionalInterface
interface RecentServiceErrorsClient {

    Map<String, Object> call(EvidenceCollectionContext context, UUID toolCallId);
}
