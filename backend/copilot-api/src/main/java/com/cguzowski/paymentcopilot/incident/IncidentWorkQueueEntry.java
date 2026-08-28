package com.cguzowski.paymentcopilot.incident;

import java.util.UUID;

record IncidentWorkQueueEntry(Incident incident, UUID activeInvestigationId) {
}
