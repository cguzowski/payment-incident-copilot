---
{"classification": "Internal - Synthetic Demo", "documentId": "7178d74f-541e-41b2-935a-76237cdf70b1", "effectiveDate": "2026-10-01", "families": ["SETTLEMENT_DELAY", "RECONCILIATION_MISMATCH"], "filename": "rb-107-settlement-checkpoints-and-receipt-gaps.pdf", "key": "RB-107", "owner": "Settlement Owner", "related": ["PL-103", "RB-109", "RB-111"], "risks": ["R12", "R13", "R16", "R19"], "sources": ["E04", "E06", "E07", "E09", "E10", "E11"], "stages": ["capture", "refund", "settlement", "reconciliation"], "status": "APPROVED", "tenantId": "8b860d80-d17f-4e6b-8c48-af35f26a4d61", "title": "Settlement checkpoints and receipt gaps", "type": "RUNBOOK", "version": "1.0.0"}
---
# Settlement checkpoints and receipt gaps

## Purpose
Investigate synthetic batch delays or disagreements between receipts and local
batch status. A delayed checkpoint is a symptom, not proof of loss or failed
bank movement. This system models no real transfers.

The Settlement Owner owns batch membership and receipts. Reconciliation Owner
compares complete snapshots; Platform Owner supplies effective cutoffs and clock
observations. No batch resubmission or balance change is authorized here.
<!-- page -->
# Establish the expected checkpoint
## Membership and timing
Request E07 batch identity, closure version, membership, submission record and any
downstream receipt. Preserve OPEN versus CLOSED state: a changing open membership
is not comparable with a closed receipt. Check E10's effective cutoff and expected
checkpoint, including timezone interpretation. Missing configuration prevents an
overdue conclusion; do not supply a conventional payment-network deadline.

Compare E04/E06 confirmed operation membership with E07, and obtain E09 snapshot
coverage at the same cutoff. Separate a missing local receipt, a missing downstream
confirmation and a membership mismatch. Event arrival order may differ from source
order; E11 clock uncertainty must remain part of the comparison.

## Interpretation table
| Condition | Investigation direction |
|---|---|
| Batch still OPEN or not yet due | Confirm effective rule; do not classify checkpoint as failed |
| Submitted, receipt unavailable | Request exact batch confirmation; retain UNKNOWN outcome |
| Receipt confirmed, local status stale | Compare source/projection versions; no automatic resubmission |
| Comparable complete snapshots differ | Trace membership and operation lineage using RB-109 |

Negative checks include omitted pages, unsupported filters and changed closure
versions. An empty receipt query outside retained scope is not evidence that the
batch never completed. A receipt confirms its listed batch, not unlisted captures
or later refunds. Contradictory receipts require source-owner review.
<!-- page -->
# Controlled recovery and closure
## Conditional direction
The Settlement Owner first recovers confirmation for the original batch. Advice
to resubmit requires a known outcome, duplicate-protection review and a separately
authorized procedure. Do not repeat submission solely because a local timer fired.
For confirmed receipt/local projection disagreement, request reconciliation of the
projection while preserving the receipt and closure identity.

## Verification and escalation
Retain batch closure/version, exact membership digest or bounded list, receipt IDs,
effective rule, comparable cutoffs and incomplete scopes. Ask for the smallest
missing source that distinguishes delayed confirmation from actual failure.
Escalate unresolved timing/record disagreement to Settlement and Platform Owners.

After an authorized correction, compare original membership and receipt with the
new view, and check unrelated batches remain outside the change scope. A local
configuration rollback cannot undo a confirmed synthetic settlement outcome;
any successor operation remains separately attributable.

## Current capability
Only aggregate service-error evidence is available via MCP. E04/E06/E07/E09-E11
are future source requirements. A report can identify missing checkpoint evidence
but must not invent transfers, balances or deadlines. Q6 degraded/empty evidence
continues to require LOW confidence and null cause/recommendation.

## Related guidance and revision
Related documents: PL-103, RB-109, RB-111.

Version 1.0.0, effective 2026-10-01. Initial independent risk-derived issue.
Review cadence: annual, or before a material domain/source/control change.
