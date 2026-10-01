---
{"classification": "Internal - Synthetic Demo", "documentId": "c5e24c02-071c-44de-95b4-e4e1f3eefd47", "effectiveDate": "2026-10-01", "families": ["AUTHORIZATION_DECLINE_RATE_SPIKE", "AUTHORIZATION_TIMEOUT_SPIKE", "CAPTURE_FAILURE_SPIKE", "REFUND_FAILURE_SPIKE", "SETTLEMENT_DELAY", "WEBHOOK_DELIVERY_FAILURE", "RECONCILIATION_MISMATCH"], "filename": "pl-103-time-freshness-and-comparable-cutoff-policy.pdf", "key": "PL-103", "owner": "Platform Owner", "related": ["RB-107", "RB-109", "RB-110", "PL-101"], "risks": ["R09", "R12", "R13", "R14", "R15", "R16", "R19", "R20"], "sources": ["E07", "E08", "E09", "E10", "E11"], "stages": ["cross-stage"], "status": "APPROVED", "tenantId": "8b860d80-d17f-4e6b-8c48-af35f26a4d61", "title": "Time, freshness and comparable-cutoff policy", "type": "POLICY", "version": "1.0.0"}
---
# Time, freshness and comparable-cutoff policy

## Purpose
Prevent elapsed time, arrival order or mismatched cutoffs from becoming invented
payment outcomes. Timing expectations are versioned synthetic configuration,
not assumptions copied from real payment networks.

Platform Owner owns effective configuration/clock semantics. Settlement, Delivery
and Reconciliation Owners own their bounded comparisons. This policy applies
across stages wherever a conclusion depends on deadlines or freshness.
<!-- page -->
# Mandatory timing and comparison rules
## Time provenance
Records must preserve UTC source event time, received time and snapshot collection
time separately. Source sequence/version orders records only within its defined
stream. Cross-source timestamps alone must not resolve final-result disagreement.
Known clock uncertainty must remain explicit; missing clock data is not zero offset.

Authorization deadlines, hold expiry, refund expectations, retry budgets and batch
cutoffs must reference effective configuration identity and interval. V1 sets no
universal duration. If the applicable rule or event-time basis is absent, analysts
must not assert overdue, expired or safe repeat. Crossing an expected time can
justify requesting confirmation but must not create a terminal payment outcome.

## Freshness and completeness
Source-specific freshness rules must be stated rather than silently applying one
cache timeout to every source. A stale snapshot establishes historical facts only.
Complete-empty results are bounded to the covered query; truncated pages, retention
loss and unsupported filters must prevent completeness claims.

## Comparable inputs
Reconciliation must compare the same tenant, SYN_UNIT, covered cutoff and operation
membership. A changing OPEN batch cannot be compared as if it were CLOSED. Receipt
and local snapshot versions must identify the same membership scope. Comparable
totals without matching membership do not establish correctness.

Delayed/reordered messages must remain distinguishable from late source events.
An acknowledgement establishes recipient receipt, not the freshness or correctness
of its view. An exhausted retry budget does not prove payment failure.
<!-- page -->
# Owners and unresolved timing
## Required responsibility
| Role | Responsibility |
|---|---|
| Platform Owner | Supply effective configuration, timezone/cutoff interpretation and clock uncertainty |
| Source Owner | Identify retained coverage, sequence semantics and snapshot freshness |
| Reconciliation / Settlement Owner | Verify comparable membership/cutoffs before discrepancy conclusions |
| Payment Operations | Record absent rules and distinguish observation from timing inference |

## Exceptions and changes
Missing timing rules must result in an evidence request and unresolved assertion,
not an invented deadline. A change to cutoffs, clock interpretation or retention
must be versioned and reviewed for affected historical comparisons. Past event
timestamps must not be rewritten to make an investigation timeline consistent.

## Retained evidence and review
Retain effective rule IDs, source timestamps/versions, coverage, membership and
before/after comparison scopes. Owners must review these controls after timing,
delivery or reconciliation contract changes. Validation after an authorized
configuration correction must use compatible windows and preserve past snapshots.
Rollback changes future configuration only; it does not invalidate retained facts.

E07-E11 are future source requirements, not current console capabilities. Current
MCP aggregate windows cannot establish item-level deadlines or batch correctness.
Incomplete/degraded report evidence retains Q6 LOW/null fields.

## Related guidance and revision
Related documents: RB-107, RB-109, RB-110, PL-101.

Version 1.0.0, effective 2026-10-01. Initial independent risk-derived issue.
Review cadence: annual, or before a material domain/source/control change.
