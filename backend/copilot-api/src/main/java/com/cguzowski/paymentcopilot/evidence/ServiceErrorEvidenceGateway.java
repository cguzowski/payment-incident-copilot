package com.cguzowski.paymentcopilot.evidence;

import com.cguzowski.paymentcopilot.incident.EvidenceCollectionContext;
import java.util.UUID;

interface ServiceErrorEvidenceGateway {

    EvidenceSourceResult collect(EvidenceCollectionContext context, UUID toolCallId);
}
