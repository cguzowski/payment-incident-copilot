---
{"classification": "Internal - Synthetic Demo", "documentId": "56f911d1-ca67-46db-87d6-d46cf163e20e", "effectiveDate": "2026-10-01", "families": ["CAPTURE_FAILURE_SPIKE"], "filename": "rb-104-capture-acknowledgement-and-amount-checks.pdf", "key": "RB-104", "owner": "Capture Owner", "related": ["PL-102", "RB-103", "RB-109"], "risks": ["R05", "R06", "R07", "R17"], "sources": ["E02", "E03", "E04", "E05", "E09", "E10"], "stages": ["authorization", "capture"], "status": "APPROVED", "tenantId": "8b860d80-d17f-4e6b-8c48-af35f26a4d61", "title": "Capture acknowledgement and amount checks", "type": "RUNBOOK", "version": "1.0.0"}
---
# Capture acknowledgement and amount checks

## Use and preconditions
Investigate capture submission failures, missing confirmations or apparent amount
breaches. Capture requires a confirmed authorization relationship; submission
does not prove capture, and confirmed capture does not prove settlement.

The Capture Owner confirms stage results. The Authorization Owner supplies amount
and hold history. No recapture or balance correction follows automatically from
an error. All amounts use integer SYN_UNIT in the fictional domain.
<!-- page -->
# Reconstruct authorization and capture lineage
## Procedure
Request E03 approval and E05 hold/expiry records linked to the E02 intent and
request. Compare authorization amount and effective timing rule in E10. An absent
expiry record cannot be replaced by elapsed local time. If approval or linkage
is missing, record the precondition as unestablished rather than guessing failure.

Obtain E04 capture requests and final acknowledgements with operation IDs. List
CONFIRMED, PENDING/UNKNOWN and explicitly FAILED operations separately. A local
submission exception may coexist with downstream confirmation; do not recapture
until that distinction is resolved.

Compute confirmed capture sum and unresolved exposure by distinct logical
operations. Repeated acknowledgement delivery must not be counted twice. Compare
confirmed plus pending exposure with the confirmed authorization amount. Reserve
pending headroom; do not treat it as available simply because confirmation is late.

## Alternative explanations
| Symptom | Distinguishing check |
|---|---|
| Capture shown failed locally | E04 explicit downstream result versus local exception |
| Amount exceeds authorization | E03/E04 same scope, units and operation identity; remove repeated events only with identity proof |
| Capture appears after expiry | E05 confirmed expiry and E10 timing, not collection/arrival order |
| Capture missing from comparison | E09 snapshot completeness/cutoff versus actual absent confirmation |

Conflicting final results or a confirmed invariant breach require owner review.
Do not explain them away using split tender, incremental authorization or FX;
those mechanisms are outside this synthetic domain.
<!-- page -->
# Safe diagnostic and recovery direction
## Conditional decisions
A confirmed capture should be retained even when the local request view failed.
The owner may separately authorize projection reconciliation after confirming
identity and audit scope. A confirmed failed capture may support controlled
resubmission only after PL-102 preconditions, headroom and idempotency checks.
Unknown results support an acknowledgement request, not a new-key retry.

For amount conflicts, the Capture Owner coordinates with the Lifecycle and
Reconciliation Owners using RB-109. A refund is a separate linked operation,
not an automatic correction to a failed comparison.

## Validation and rollback boundary
After an authorized configuration/projection correction, compare original E04
acknowledgements, current projection and unchanged authorization history. Define
the validation window and owner checkpoint. Rolling back configuration does not
undo a confirmed capture; any recovery operation needs separate authorization,
identity and final confirmation.

Retain approval/hold IDs, capture matrix, arithmetic, pending exposure, effective
rule and missing records. Only aggregate service errors are available now; richer
sources are future requirements. Incomplete evidence must remain visible and
degraded/empty reports retain Q6 LOW/null cause and recommendation.

## Related guidance and revision
Related documents: PL-102, RB-103, RB-109.

Version 1.0.0, effective 2026-10-01. Initial independent risk-derived issue.
Review cadence: annual, or before a material domain/source/control change.
