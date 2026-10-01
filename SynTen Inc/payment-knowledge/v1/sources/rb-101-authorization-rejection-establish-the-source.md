---
{"classification": "Internal - Synthetic Demo", "documentId": "9abe0787-effc-433a-992c-a28cf64c27ad", "effectiveDate": "2026-10-01", "families": ["AUTHORIZATION_DECLINE_RATE_SPIKE"], "filename": "rb-101-authorization-rejection-establish-the-source.pdf", "key": "RB-101", "owner": "Authorization Owner", "related": ["PL-101", "RB-102", "RB-110"], "risks": ["R01", "R18", "R20"], "sources": ["E01", "E02", "E03", "E10", "E11"], "stages": ["authorization"], "status": "APPROVED", "tenantId": "8b860d80-d17f-4e6b-8c48-af35f26a4d61", "title": "Authorization rejection: establish the source", "type": "RUNBOOK", "version": "1.0.0"}
---
# Authorization rejection: establish the source

## Use and boundaries
Use when an authorization is described as rejected or a rejection rate changes.
Distinguish local validation, routing/transport errors and explicit downstream
declines before interpreting a reason. A locked card, insufficient funds or expired
card explanation requires an explicit correlated synthetic issuer response.
An aggregate error count or alert title does not establish an individual condition.

The analyst gathers evidence; the Authorization Owner interprets reason semantics.
Gateway and Platform Owners investigate mapping/configuration discrepancies.
This guide does not change routing, retry a payment or make a card/account claim.
<!-- page -->
# Establish the rejection boundary
## Read-only sequence
1. Preserve the E01 service/window/status and error counts. Record whether the
window is complete; a count is not a measured decline rate without a denominator.
2. Request E02 for the exact tenant, intent and authorization request. Compare
submission state with any local validation result. Do not infer submission from
an alert description. Retain missing identifiers as gaps.
3. Request E03 with the correlated request and downstream operation identity.
Check explicit outcome, reason and source version. A generic decline supports
only generic rejection; absent acknowledgement means the outcome is unknown.
4. Compare effective response-mapping/routing configuration in E10. Use E11 to
check bounded dependency/access failures only if the observed window overlaps.
An overlapping change is a hypothesis, not proof of causation.

## Interpretation and negative checks
| Observed boundary | Supported reading | Required exclusion |
|---|---|---|
| Local validation stopped submission | Local request rejection | E02 must not show a later correlated submission |
| Explicit downstream decline/reason | Downstream rejection with that reason | Confirm request identity and mapping version |
| Transport error without final response | Uncertain outcome | Request authoritative response; do not call it a decline |

Different routes or observation windows may explain apparently conflicting counts.
Do not merge them into one cause. If E03 disagrees with E02, retain both snapshots
and escalate the mapping/identity conflict before presenting a final conclusion.
<!-- page -->
# Advice and verification
## Conditional direction
For an explicit downstream reason, describe the bounded observation and cite its
record. The owner may review the applicable synthetic response policy; the copilot
must not recommend overriding issuer controls. Generic rejection needs better
reason evidence before any specific card explanation.

For transport uncertainty, use RB-102. For a suspected mapping/configuration fault,
use RB-110 and retain the before/after version IDs. A human-controlled correction
must name its owner, approval, validation window and rollback configuration;
report approval alone authorizes none of those changes.

## Escalation and closure
Send the Authorization Owner the request identity, exact source/window, explicit
reason or its absence, mapping version, competing explanation and missing source.
Do not include raw sensitive payloads. Closure requires consistent correlated
records or an explicitly unresolved result with an accountable evidence request.
Compare subsequent observations with the same scope; absence of fresh errors is
not proof that earlier requests succeeded.

## Current product capability
Only E01 aggregate errors are exposed by current MCP. E02-E11 are future read-only
source requirements; request them from the named fictional owner, not a nonexistent
console control. Degraded/empty report evidence retains Q6 LOW/null behavior.

## Related guidance and revision
Related documents: PL-101, RB-102, RB-110.

Version 1.0.0, effective 2026-10-01. Initial independent risk-derived issue.
Review cadence: annual, or before a material domain/source/control change.
