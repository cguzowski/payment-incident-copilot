---
documentId: 66a84fed-3d77-4e7e-9a1b-e25ff37e2280
tenantId: 8b860d80-d17f-4e6b-8c48-af35f26a4d61
type: RUNBOOK
title: Authorization Decline Runbook
version: 1.0.0
incidentFamily: AUTHORIZATION_DECLINE_RATE_SPIKE
appliesTo: Card authorization
approvalStatus: APPROVED
approvedBy: 7b636625-53d1-46f7-92a9-9c8c27a243d1
approvedAt: 2026-08-20T10:00:00Z
effectiveAt: 2026-08-21T00:00:00Z
---
# Authorization Decline Runbook

## Initial assessment

Confirm that the alert concerns the synthetic card-authorization path and note
the detected and received times. Compare the incident window with the retrieved
service-error window before drawing a conclusion. Treat missing partitions,
timeouts, and unavailable sources as investigation limitations. Do not infer a
payment outcome from an alert alone, and do not treat a lack of retrieved errors
as proof that the authorization service was healthy.

Record the evidence identifier, tool-call identifier, source system, retrieval
status, and observation window used for the assessment. Continue only with
approved synthetic evidence. If evidence belongs to another tenant, incident,
or investigation, stop and report the mismatch instead of reusing it.

## Gateway Failures

### Diagnosis

When `GATEWAY_TIMEOUT` or `UPSTREAM_CONNECTION_RESET` observations rise during
the decline-rate window, compare their timestamps and aggregate counts with the
alert. A close temporal match supports investigating gateway connectivity, but
it does not independently prove that every decline came from the gateway.

Check whether the evidence is `AVAILABLE` or `PARTIAL`. Partial coverage means
the absent partitions may contain confirming or contradictory observations.
Preserve that limitation in the investigation record and avoid presenting a
single partition as the complete service state.

### Safe next checks

Review the approved gateway-response categories and recent deployment history
when those evidence sources become available. Confirm whether timeouts were
isolated to one synthetic route or observed across the authorization service.
Do not retry, reroute, disable, or modify a gateway from this runbook; all
operational actions remain outside the copilot and require separate authority.

## Negative evidence

An available observation window with no `GATEWAY_TIMEOUT` or
`UPSTREAM_CONNECTION_RESET` entries is successful negative evidence. It lowers
support for a service-error explanation within that exact window but does not
rule out gateway declines, configuration changes, issuer responses, or evidence
outside the retrieved interval.

Document the empty result with its source and retrieval timestamp. Do not
rewrite it as unavailable evidence, and do not manufacture an error category to
make the alert appear explained.

## Escalation information

If the available evidence shows a sustained cluster of gateway failures, prepare
an escalation summary containing the incident identifier, bounded observation
window, exact error categories, aggregate counts, and evidence identifiers.
State whether the source was complete or partial. The summary is advisory input
for a human operator and must not trigger a remediation action automatically.
