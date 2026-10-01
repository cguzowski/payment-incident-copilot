---
{"classification": "Internal - Synthetic Demo", "documentId": "09815930-f605-4daf-bc33-2625abb3da25", "effectiveDate": "2026-10-01", "families": ["AUTHORIZATION_DECLINE_RATE_SPIKE", "AUTHORIZATION_TIMEOUT_SPIKE", "CAPTURE_FAILURE_SPIKE", "REFUND_FAILURE_SPIKE", "SETTLEMENT_DELAY", "WEBHOOK_DELIVERY_FAILURE", "RECONCILIATION_MISMATCH"], "filename": "pl-101-evidence-sufficiency-and-conflicting-source-policy.pdf", "key": "PL-101", "owner": "Knowledge Approver", "related": ["RB-111", "PL-103", "PL-104", "PL-105"], "risks": ["R01", "R20", "R21", "R22"], "sources": ["E01", "E02", "E03", "E04", "E05", "E06", "E07", "E08", "E09", "E10", "E11"], "stages": ["cross-stage"], "status": "APPROVED", "tenantId": "8b860d80-d17f-4e6b-8c48-af35f26a4d61", "title": "Evidence sufficiency and conflicting-source policy", "type": "POLICY", "version": "1.0.0"}
---
# Evidence sufficiency and conflicting-source policy

## Policy intent
Preserve the difference between observed fact, inference and operational guidance.
Every conclusion must be bounded by source authority and coverage. A plausible
explanation is not an observed outcome, and a schema-valid report can still be wrong.

This policy applies to every synthetic payment stage and investigation. Knowledge
Approver owns controlled guidance; Payment Operations owns source/citation review.
Responsible stage owners clarify their records. No role may fabricate confirmation
to make a report complete.
<!-- page -->
# Mandatory evidence controls
## Authority and provenance
Every observation and inference must reference supporting immutable evidence IDs.
Claims involving a comparison must retain both sources. Approved knowledge may
support investigation methods or conditional advice, but never proves an event
occurred. Alert text, model output and offline oracle truth are not substitutes
for authoritative operational records.

The investigator must retain tenant, source/version, operation linkage, requested
scope, coverage, event/received/collection time and result status. Missing fields
must remain missing. Authority is claim-specific: a request journal proves local
submission; a correlated stage acknowledgement proves only that stage's result.
Aggregate errors cannot prove item-level outcomes or specific issuer reasons.

## Sufficiency and disagreement
PARTIAL, UNAVAILABLE, TIMED_OUT, malformed, stale and complete-empty sources must
remain distinguishable. Complete-empty establishes no matching record in that
covered query only. Unknown outcome is a lifecycle value, not a transport status.
Several AVAILABLE sources do not establish completeness if none answers the claim.

Conflicting authoritative outcomes must be preserved with both versions and
citations. Investigators must not resolve them by choosing the latest timestamp,
majority vote or preferred narrative. Source scope/linkage/version semantics and
owner review are required. Unsupported explanations must not be promoted to facts.

## Report rule
Q6 degraded/empty evidence must retain INSUFFICIENT_EVIDENCE, LOW confidence and
null cause and recommendation. This policy introduces no report-schema exception.
Missing/contradictory evidence must be visible; human review remains mandatory.
<!-- page -->
# Roles, exceptions and review
## Accountability
| Role | Required responsibility |
|---|---|
| Payment Operations Analyst | Verify source bindings, coverage and citations; preserve gaps and competing explanations |
| Stage / Lifecycle Owner | Clarify claim-specific records without rewriting historical confirmations |
| Knowledge Approver | Review guidance relevance, capability limits and controlled versions |
| Incident Commander | Coordinate unresolved conflicts and retain human checkpoints |

## Exceptions and non-compliance
No exception permits fabricated evidence, foreign-tenant records, model approval
or automatic action. An unavailable source is documented as a limitation with
an owner request; it is not waived into sufficient evidence. A required contract
change must receive an explicit successor decision and tests before use.

If a report used unsupported confirmation, preserve the original attempt and
record the issue for human review. Do not edit a persisted historical report
to conceal the error. A later corrected attempt must have its own provenance.

## Records and review triggers
Retain evidence/retrieval/model/prompt/version metadata, exact cited snapshots,
decisions and attributable review. V1 sets no arbitrary retention duration;
deletion requires a separately agreed retention policy, never ad hoc cleanup.
Review when a source contract, authority mapping or report-sufficiency rule changes.
Only E01 is available via current MCP; E02-E11 remain future evidence requirements.
This synthetic knowledge approval does not approve any incident report.

## Related guidance and revision
Related documents: RB-111, PL-103, PL-104, PL-105.

Version 1.0.0, effective 2026-10-01. Initial independent risk-derived issue.
Review cadence: annual, or before a material domain/source/control change.
