---
{"classification": "Internal - Synthetic Demo", "documentId": "2dd8bb43-0597-46b2-9705-8ee715035771", "effectiveDate": "2026-10-01", "families": ["REFUND_FAILURE_SPIKE"], "filename": "rb-106-refund-progress-and-partial-amount-exposure.pdf", "key": "RB-106", "owner": "Refund Owner", "related": ["PL-102", "PL-103", "RB-103", "RB-109"], "risks": ["R10", "R11", "R03", "R17"], "sources": ["E02", "E04", "E06", "E09", "E10"], "stages": ["capture", "refund"], "status": "APPROVED", "tenantId": "8b860d80-d17f-4e6b-8c48-af35f26a4d61", "title": "Refund progress and partial amount exposure", "type": "RUNBOOK", "version": "1.0.0"}
---
# Refund progress and partial amount exposure

## Use and scope
Investigate missing, delayed, failed or partial synthetic refunds against a
confirmed capture. Refund submission is not final completion and never proves
a recipient-visible balance. Do not treat release of an uncaptured authorization
as a refund.

The Refund Owner confirms each linked refund result. The Capture Owner supplies
captured amount, and Reconciliation Owner checks bounded comparison discrepancies.
This guide cannot initiate a refund or contact any external party.
<!-- page -->
# Build the refund exposure register
## Sequence
1. Obtain E04 confirmed capture identity/amount and E02 request lineage. Check tenant,
unit, operation type and original intent. A refund cannot be assessed against
an authorization amount alone; a missing capture link is an evidence gap.
2. Obtain E06 all linked refund operations in the relevant covered window.
Group by logical operation identity, not repeated event delivery. Record confirmed,
pending/unknown and explicitly failed amounts separately with source IDs.
3. Compare confirmed plus unresolved exposure with confirmed captured units.
Partial refunds remain distinct operations. A delayed result reserves headroom;
it does not make that amount available for a replacement refund.
4. Use E10's effective completion expectation to assess delay. Without the rule,
event time or clock basis, describe elapsed observation only, not breach of a SLA.

## Distinguishing explanations
| Apparent problem | Distinguishing evidence |
|---|---|
| Local refund failure | E06 final downstream acknowledgement versus submission exception |
| Missing refund in a view | Source coverage/linkage and E09 compatible cutoff, not an assumed lost transfer |
| Apparent excess returned amount | Distinct operation IDs and confirmed/pending sums; repeated events differ from additional refunds |
| Delayed recipient display | E06 can establish refund result, not recipient account state |

Retain conflicting final acknowledgements. Do not automatically choose the newest
timestamp or remove a refund from the sum because its result is inconvenient.
<!-- page -->
# Advice depends on the original outcome
## Recovery review
For a known failed operation, the Refund Owner may review controlled resubmission
only after PL-102 identity, payload and headroom checks. A pending/unknown refund
supports confirmation recovery, not a new-key replacement. If confirmed refunds
already consume captured units, advise owner review of the discrepancy rather
than another refund or direct adjustment.

A confirmed result can be described only for the linked synthetic refund. Do not
promise when a customer will see funds or introduce fees, FX or bank behavior;
the domain does not model those facts. General guidance may identify what external
confirmation would be needed, but the copilot has no such source or contact authority.

## Validate, escalate and retain
Escalation includes original capture ID/amount, refund register, unresolved exposure,
effective timing rule, competing explanations and exact missing source/window.
After separately authorized recovery, confirm both original and successor operations
and recompute exposure. Rollback of local configuration cannot undo a completed
refund; do not delete its immutable history.

E02/E04/E06/E09/E10 are future read-only requirements, not current UI controls.
Aggregate refund-service errors cannot establish a refund's final outcome.
Degraded/empty report evidence retains Q6 LOW/null behavior.

## Related guidance and revision
Related documents: PL-102, PL-103, RB-103, RB-109.

Version 1.0.0, effective 2026-10-01. Initial independent risk-derived issue.
Review cadence: annual, or before a material domain/source/control change.
