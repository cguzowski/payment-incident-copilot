---
{"classification": "Internal - Synthetic Demo", "documentId": "837bf69f-1ea7-4cc6-8d51-2e5ef19cfabd", "effectiveDate": "2026-10-01", "families": ["AUTHORIZATION_TIMEOUT_SPIKE", "AUTHORIZATION_DECLINE_RATE_SPIKE"], "filename": "rb-102-uncertain-authorization-outcomes.pdf", "key": "RB-102", "owner": "Gateway Owner", "related": ["RB-103", "RB-105", "PL-102", "PL-103"], "risks": ["R02", "R03", "R09", "R19"], "sources": ["E02", "E03", "E05", "E10", "E11"], "stages": ["authorization", "release"], "status": "APPROVED", "tenantId": "8b860d80-d17f-4e6b-8c48-af35f26a4d61", "title": "Uncertain authorization outcomes", "type": "RUNBOOK", "version": "1.0.0"}
---
# Uncertain authorization outcomes

## When to use this guide
A missing timely response does not establish failure. A downstream authorization
may have completed while its acknowledgement was lost. Investigate the exact
attempt before proposing another request, cancellation or release.

The Gateway Owner owns confirmation recovery. Payment Operations records what is
known, what is unknown and whether any active hold is independently confirmed.
This guide addresses uncertain authorization outcomes, not refund completion.
<!-- page -->
# Build a bounded outcome timeline
## Identity before elapsed time
Start with E02: tenant, intent, request, idempotency key and canonical payload
identity. Keep attempts separate even if their display titles match. Preserve
source event time, received time and collection time; order within a source by
its supported version/sequence, not transport arrival.

Obtain E03 for each affected request and E05 for any linked hold. Record explicit
approval/decline and remaining held amount separately. E05 can confirm a hold
without explaining why a local response was missing. Missing E05 does not prove
that no hold exists. Compare E10's effective deadline with the source event time.
If that rule or E11 clock uncertainty is absent, do not assert overdue or expired.

## Outcome branches
| Evidence available | Investigation direction |
|---|---|
| Correlated approval | Treat that attempt as approved; inspect linked hold before any recovery |
| Correlated explicit decline | Retain the decline; check other attempts before considering another submission |
| No final correlated response | Keep UNKNOWN; request source recovery rather than retrying blindly |
| Conflicting final records | Preserve both IDs/versions; investigate identity and mapping with owners |

Check negative evidence scope: a complete empty E03 query may use the wrong
operation/window or lack retained history. An acknowledgement for a different
request is not confirmation for this attempt. Two holds do not establish two
confirmed captures; compare lifecycle stages before calling an outcome duplicate.
<!-- page -->
# Recovery must wait for a known boundary
## Conditional action review
Do not advise a new-key retry while a prior outcome remains unknown. PL-102
requires original-operation confirmation and idempotency review. A confirmed
approval may need a separately authorized hold-release procedure if the intent
was cancelled; RB-105 distinguishes release from refund of captured units.
A confirmed decline may support a bounded next diagnostic step, but does not
authorize bypassing a card condition or repeating a changed payload under one key.

## Confirmation package
Ask the Gateway Owner for the exact request's final response, linked hold record,
applicable deadline and source coverage. Include the earliest/latest retained
versions and any clock uncertainty. If unavailable, record the attempted scope
and owner checkpoint without inventing a completion time.

After separately authorized recovery, verify both the original attempt and any
new operation with distinct correlated acknowledgements. Rollback of local
configuration cannot undo an approved authorization; any hold release remains
a separate operation with its own authority and confirmation.

## Product limit and closure
Current MCP provides only aggregate E01 errors. Item-level records referenced here
are future source requirements. Close with confirmed bounded outcome or explicitly
unresolved uncertainty; degraded/empty evidence keeps Q6 LOW/null report fields.

## Related guidance and revision
Related documents: RB-103, RB-105, PL-102, PL-103.

Version 1.0.0, effective 2026-10-01. Initial independent risk-derived issue.
Review cadence: annual, or before a material domain/source/control change.
