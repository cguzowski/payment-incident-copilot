# Synthetic alert intake

`POST /api/alerts` uses `X-Synthetic-Tenant-Id` for tenant context.
The JSON body contains externalAlertId (1-120 characters), severity
(LOW/MEDIUM/HIGH/CRITICAL), detectedAt (ISO-8601 UTC), title (1-500 characters),
description (1-2000 characters), and optional incidentType.

Supported incidentType values:

- AUTHORIZATION_DECLINE_RATE_SPIKE
- AUTHORIZATION_TIMEOUT_SPIKE
- CAPTURE_FAILURE_SPIKE
- REFUND_FAILURE_SPIKE
- SETTLEMENT_DELAY
- WEBHOOK_DELIVERY_FAILURE
- RECONCILIATION_MISMATCH

An omitted or null type defaults to AUTHORIZATION_DECLINE_RATE_SPIKE for legacy
clients. An unrecognized type returns a structured HTTP 400 response without
persisting an incident. The API does not classify by title or external reference.

Example body (all fields are synthetic):

```json
{
  "externalAlertId": "synthetic-refund-alert-001",
  "incidentType": "REFUND_FAILURE_SPIKE",
  "severity": "HIGH",
  "detectedAt": "2026-09-30T12:00:00Z",
  "title": "Refund failure spike",
  "description": "Synthetic refund submissions recorded failures in the alert window."
}
```

A new alert returns HTTP 201 with incidentId, the persisted incidentType,
status NEW and receivedAt. Replaying the same tenant/externalAlertId returns
HTTP 200 and the original record, retaining its original type and fields even
if the replay changes the type. Queue, detail and investigation projections
retain the persisted family. Cross-tenant resources remain indistinguishable
from not found. No schema migration is required: existing family storage is
VARCHAR and existing records retain the original decline value.

For generated alerts, use the generator so its opaque reference has matching
MCP evidence. An arbitrary externalAlertId does not synthesize evidence merely
by declaring a family. See [family package and preparation](../../SynTen%20Inc/multi-incidents/v1/README.md).
All downstream evidence/retrieval/report/decision/timeline HTTP contracts are
unchanged, including synthetic operator headers on operator mutations.
