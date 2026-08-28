package com.cguzowski.paymentcopilot.incident;

import java.util.UUID;

interface ServiceErrorEvidenceGateway {

    EvidenceSourceResult collect(EvidenceCollectionContext context, UUID toolCallId);
}
