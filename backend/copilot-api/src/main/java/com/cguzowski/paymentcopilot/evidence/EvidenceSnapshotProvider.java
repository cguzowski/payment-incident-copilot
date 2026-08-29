package com.cguzowski.paymentcopilot.evidence;

import java.util.Optional;
import java.util.UUID;

public interface EvidenceSnapshotProvider {

    Optional<EvidenceSnapshot> findByTenantIdAndInvestigationId(UUID tenantId, UUID investigationId);
}
