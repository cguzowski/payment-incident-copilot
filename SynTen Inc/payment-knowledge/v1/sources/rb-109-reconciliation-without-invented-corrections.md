---
{"classification": "Internal - Synthetic Demo", "documentId": "4f536824-87bf-4bd0-bad3-04bcbacff40f", "effectiveDate": "2026-10-01", "families": ["RECONCILIATION_MISMATCH", "CAPTURE_FAILURE_SPIKE", "REFUND_FAILURE_SPIKE", "SETTLEMENT_DELAY"], "filename": "rb-109-reconciliation-without-invented-corrections.pdf", "key": "RB-109", "owner": "Reconciliation Owner", "related": ["PL-101", "PL-102", "PL-103", "RB-111"], "risks": ["R07", "R11", "R13", "R16", "R17", "R21"], "sources": ["E02", "E04", "E06", "E07", "E09", "E10"], "stages": ["capture", "refund", "settlement", "reconciliation"], "status": "APPROVED", "tenantId": "8b860d80-d17f-4e6b-8c48-af35f26a4d61", "title": "Reconciliation without invented corrections", "type": "RUNBOOK", "version": "1.0.0"}
---
# Reconciliation without invented corrections

## Use and limits
Investigate a mismatch only after establishing comparable, complete inputs.
Differences may result from cutoff, membership, duplicated events, omitted records,
pending outcomes or genuinely conflicting confirmations. A mismatch never
authorizes a balance correction.

The Reconciliation Owner owns the comparison; stage owners confirm their records.
Every amount is synthetic SYN_UNIT. Do not add fees, exchange or real bank
settlement assumptions to make two totals agree.
<!-- page -->
# Establish comparability before totals
## Comparison sequence
1. Obtain E09 input source/snapshot IDs, tenant, covered interval, cutoff, membership,
pagination and completion status. If any input is partial or missing, classify
the comparison as incomplete; retain the observed difference without claiming
a confirmed financial discrepancy.
2. Use E10 effective cutoff/unit rules. Compare the same closed E07 membership
version when settlement is involved. Different cutoffs or open membership may
explain a difference and must be excluded before causal interpretation.
3. Trace E02 intent/request lineage into E04 captures and E06 refunds. Count unique
logical operations; preserve confirmed versus pending/unknown exposure separately.
Two messages for one operation must not be counted as two payments.
4. Compare record-level membership before amount sums. Record missing/extra IDs
and inconsistent outcomes with both source references. Similar totals can hide
different membership; a zero difference is not sufficient evidence of correctness.

## Interpretation
| Finding | Required next evidence |
|---|---|
| Different cutoff or partial input | Complete aligned snapshots; no correction advice yet |
| Repeated events with same operation | Identity/consumer comparison, not refund of a presumed duplicate |
| Distinct confirmed operations differ | Stage-owner confirmation and amount/headroom review |
| Contradictory final outcomes | Both immutable versions plus responsible source-owner review |

Do not pick the newest timestamp automatically or force a balance by dropping
an inconvenient record. A complete empty source result remains scoped to that
query and does not prove absence from every system.
<!-- page -->
# Escalation and controlled resolution
## Owner package
Retain input IDs, membership versions, exact cutoffs, units, coverage, record-level
differences and confirmed/pending totals. Name competing mechanisms and the
smallest discriminating source. The Reconciliation Owner coordinates with Capture,
Refund or Settlement Owners rather than converting a comparison into payment truth.

## Conditional recovery
For projection/duplication defects, an owner may separately authorize a bounded
projection correction after confirming authoritative operation identity. For a
genuine source conflict, keep the result unresolved until the responsible owner
provides authoritative explanation. Direct balance changes, automatic refunds
and deletion of source history are outside the copilot's authority.

After authorized changes, rerun the comparison with documented compatible snapshots
and compare both membership and totals. Preserve the before/after runs. Rollback
must restore the projection/configuration scope without erasing confirmed operations;
new payment operations require separate authorization and identity.

## Capability and closure
E02/E04/E06/E07/E09/E10 are future read-only source requirements. Current aggregate
errors cannot establish a balanced ledger or a transaction discrepancy. Closure
may explicitly remain incomplete/conflicting; degraded/empty reports retain Q6
LOW confidence and null cause/recommendation.

## Related guidance and revision
Related documents: PL-101, PL-102, PL-103, RB-111.

Version 1.0.0, effective 2026-10-01. Initial independent risk-derived issue.
Review cadence: annual, or before a material domain/source/control change.
