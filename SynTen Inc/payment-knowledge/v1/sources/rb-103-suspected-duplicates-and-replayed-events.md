---
{"classification": "Internal - Synthetic Demo", "documentId": "ace233b4-5dd8-4ae6-89de-c39be100c5ce", "effectiveDate": "2026-10-01", "families": ["AUTHORIZATION_DECLINE_RATE_SPIKE", "AUTHORIZATION_TIMEOUT_SPIKE", "CAPTURE_FAILURE_SPIKE", "REFUND_FAILURE_SPIKE", "SETTLEMENT_DELAY", "WEBHOOK_DELIVERY_FAILURE", "RECONCILIATION_MISMATCH"], "filename": "rb-103-suspected-duplicates-and-replayed-events.pdf", "key": "RB-103", "owner": "Payment Lifecycle Owner", "related": ["PL-101", "PL-102", "RB-105", "RB-108"], "risks": ["R03", "R04", "R11", "R14"], "sources": ["E02", "E03", "E04", "E05", "E06", "E08"], "stages": ["authorization", "capture", "refund", "delivery"], "status": "APPROVED", "tenantId": "8b860d80-d17f-4e6b-8c48-af35f26a4d61", "title": "Suspected duplicates and replayed events", "type": "RUNBOOK", "version": "1.0.0"}
---
# Suspected duplicates and replayed events

## Purpose
Separate repeated presentation, duplicated messages, repeated requests and distinct
confirmed operations. Two entries in a view are not sufficient evidence of
duplicate processing. This guide applies across authorization, capture, refund
and delivery, with the responsible stage owner confirming its own operation.

Payment Operations builds the identity comparison. The Lifecycle Owner owns intent
lineage; stage owners own final acknowledgements. No corrective payment, refund
or event replay is executed by the copilot.
<!-- page -->
# Compare operations rather than appearances
## Comparison sequence
1. Obtain E02 for the same tenant and intent. Group by logical operation type,
request and idempotency key; preserve canonical payload identity. The same key
with a changed payload is a conflict, not a legitimate second operation.
2. Compare final downstream IDs from E03, E04 or E06, according to stage. Several
local retries may refer to one downstream operation. Different operation IDs
require intent linkage before concluding that duplication was unintended.
3. For displayed holds, obtain E05 and linked E04 capture confirmations. Distinct
active holds, consumed authorization and confirmed captures have different effects.
4. For notifications, use E08's event ID, operation link and recipient acknowledgement.
A duplicate event may be a repeated delivery of one operation; arrival order does
not identify which payment state is authoritative.

## Discriminating checks
| Comparison | Direction |
|---|---|
| Same operation ID, repeated event ID | Investigate delivery/projection duplication; do not infer another capture |
| Same intent, distinct confirmed stage operations | Compare requests and amounts; investigate unintended repetition |
| Two holds without capture confirmation | Investigate hold/attempt linkage, not completed duplicate payments |
| Missing canonical payload or acknowledgement | Preserve uncertainty; request the missing identity/result source |

Exclude foreign tenants, unrelated intents and partial snapshots. Do not deduplicate
by amount/time similarity alone: two legitimate partial operations may have equal
amounts, while one repeated operation may appear with inconsistent projections.
<!-- page -->
# Containment and owner review
## Conditional response
For projection-only duplication, the Delivery Owner reviews consumer state and
acknowledgement behavior using RB-108. Do not replay events until the original
recipient result is known. For confirmed unintended repeated operations, the
stage owner reviews separately authorized recovery with PL-102 amount headroom.
Never automatically refund the apparent excess or delete a source record.

If a new-key retry preceded an uncertain first outcome, identify both operations
and suspend further advisory retry direction pending owner confirmation. Advice
must state which comparison distinguishes one repeated delivery from two actual
operations. Missing linkage remains a gap rather than an inferred common intent.

## Validate and retain
Retain the identity matrix, source snapshot IDs, coverage, confirmed/pending sums,
alternative explanations and operator review. Validation after any authorized
correction compares affected operations and recipient state separately. A cleaned
display is not evidence that a payment operation was reversed. Preserve immutable
history; rollback of a projection change must not remove earlier acknowledgements.

## Source availability
E02-E08 here are future read-only requirements. Current aggregate errors cannot
prove duplication, and an alert title is not identity evidence. Degraded/empty
report evidence retains LOW/null Q6 behavior.

## Related guidance and revision
Related documents: PL-101, PL-102, RB-105, RB-108.

Version 1.0.0, effective 2026-10-01. Initial independent risk-derived issue.
Review cadence: annual, or before a material domain/source/control change.
