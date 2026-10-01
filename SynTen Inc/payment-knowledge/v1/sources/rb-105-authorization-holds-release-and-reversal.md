---
{"classification": "Internal - Synthetic Demo", "documentId": "6bceca26-e563-4c49-b953-e7f114d3e4fa", "effectiveDate": "2026-10-01", "families": ["AUTHORIZATION_DECLINE_RATE_SPIKE", "AUTHORIZATION_TIMEOUT_SPIKE", "CAPTURE_FAILURE_SPIKE"], "filename": "rb-105-authorization-holds-release-and-reversal.pdf", "key": "RB-105", "owner": "Gateway Owner", "related": ["RB-102", "RB-106", "PL-102", "PL-103"], "risks": ["R04", "R08", "R09", "R19"], "sources": ["E02", "E03", "E04", "E05", "E10", "E11"], "stages": ["authorization", "release", "capture"], "status": "APPROVED", "tenantId": "8b860d80-d17f-4e6b-8c48-af35f26a4d61", "title": "Authorization holds, release and reversal", "type": "RUNBOOK", "version": "1.0.0"}
---
# Authorization holds, release and reversal

## Purpose and responsibility
Use for cancelled purchases, remaining holds or uncertain reversal status.
Reversal releases unconsumed authorization; refund addresses confirmed capture.
Neither an intent cancellation nor an elapsed expiry expectation proves release.

The Gateway Owner confirms hold/reversal records; the Capture Owner establishes
consumed units. Payment Operations separates the two before advising a next step.
No release, refund or customer account update is executed by this guide.
<!-- page -->
# Determine what remains authorized
## Read-only checks
Obtain E02 cancellation/submission history, E03 authorization confirmation and E04
distinct capture acknowledgements. Calculate confirmed consumption in SYN_UNIT.
List unresolved capture operations separately; they reserve headroom and may
prevent a reliable conclusion about the remaining releasable amount.

Request E05 for the exact authorization and any reversal request. Compare active,
release-pending, released and explicitly expired records using their source
versions. Source collection time or a local cancelled flag is not confirmation.
Use E10's effective expiry rule and E11 clock uncertainty when comparing timing.
If these are missing, retain the expiry question as unresolved.

## Branches and negative checks
| Established fact | Direction and limitation |
|---|---|
| No capture and active confirmed hold | Ask Gateway Owner to review separately authorized release eligibility |
| Confirmed capture consumes authorization | Release cannot return captured units; investigate refund separately |
| Reversal requested without final result | Keep release outcome unknown; request E05 acknowledgement |
| Explicit release/expiry but stale display | Compare projection freshness; do not infer a second live hold |

Check whether two visible entries refer to one authorization, different attempts
or repeated events. A complete empty release query must use the right authorization
and retained window. Missing release evidence is not proof of a failed reversal.
If capture and release confirmations appear inconsistent, preserve both and their
versions rather than treating the newest display as the true state.
<!-- page -->
# Human review and validation
## Conditional recovery
Any release recommendation requires confirmed unconsumed amount, known original
outcome, applicable policy and a separate human authorization. Pending capture
exposure or source conflict blocks that conclusion. A refund against a confirmed
capture has its own operation identity and is reviewed using RB-106, not relabeled
as reversal.

For a stale display, the owner may inspect projection lineage and notification
history without changing the authoritative hold record. Validation after an
authorized projection correction compares the same E05 authorization/version
with the displayed state. Clearing a display does not prove downstream release.

## Escalation record
Retain authorization identity, confirmed/pending capture totals, effective expiry
rule, release attempt/result, clock uncertainty and source coverage. Request the
specific missing confirmation from the Gateway Owner. An unresolved cancellation
must state what remains unknown and the next evidence checkpoint.

Rollback of a local change does not recreate or undo a downstream hold; record
that boundary in any separately authorized action plan. Current MCP exposes no
item-level hold/reversal source. E02-E11 are future requirements, and degraded/
empty reports must retain Q6 LOW/null cause and recommendation.

## Related guidance and revision
Related documents: RB-102, RB-106, PL-102, PL-103.

Version 1.0.0, effective 2026-10-01. Initial independent risk-derived issue.
Review cadence: annual, or before a material domain/source/control change.
