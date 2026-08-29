package com.cguzowski.paymentcopilot.knowledge;

import com.cguzowski.paymentcopilot.incident.EvidenceCollectionStatus;
import com.cguzowski.paymentcopilot.incident.ServiceErrorEvidenceContent;
import java.util.UUID;

record KnowledgeEvidenceReference(
        UUID latestAttemptId,
        EvidenceCollectionStatus latestStatus,
        UUID applicableAttemptId,
        ServiceErrorEvidenceContent applicableContent) {
}
