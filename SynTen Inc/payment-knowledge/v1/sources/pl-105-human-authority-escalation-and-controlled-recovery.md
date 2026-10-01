---
{"classification": "Internal - Synthetic Demo", "documentId": "3bb18135-be1c-4a38-8737-72a24b1eab50", "effectiveDate": "2026-10-01", "families": ["AUTHORIZATION_DECLINE_RATE_SPIKE", "AUTHORIZATION_TIMEOUT_SPIKE", "CAPTURE_FAILURE_SPIKE", "REFUND_FAILURE_SPIKE", "SETTLEMENT_DELAY", "WEBHOOK_DELIVERY_FAILURE", "RECONCILIATION_MISMATCH"], "filename": "pl-105-human-authority-escalation-and-controlled-recovery.pdf", "key": "PL-105", "owner": "Incident Commander", "related": ["PL-101", "PL-102", "PL-104", "RB-110", "RB-111"], "risks": ["R24", "R18", "R21", "R23"], "sources": ["E01", "E10", "E11"], "stages": ["cross-stage"], "status": "APPROVED", "tenantId": "8b860d80-d17f-4e6b-8c48-af35f26a4d61", "title": "Human authority, escalation and controlled recovery", "type": "POLICY", "version": "1.0.0"}
---
# Human authority, escalation and controlled recovery

## Purpose
Keep report review separate from operational authorization and execution.
The copilot assembles evidence and advisory guidance; the operator decides on
the exact report. Approval/rejection does not execute or authorize remediation.

Incident Commander coordinates owner review. Stage, Platform and Security Owners
remain responsible for separately authorized actions within their normal control
boundaries. All actors, systems and operational records are fictional.
<!-- page -->
# Mandatory authority and recovery controls
## Human report decision
An explicit attributable approval/rejection must bind to the exact immutable
report and retain UTC timestamp and reason. Model content must not choose terminal
workflow state, approve itself or trigger an irreversible action. A schema-valid
report remains advisory; valid citations do not guarantee supported conclusions.

## Operational authorization
Any separately controlled retry, replay, release, refund, configuration correction
or projection repair must name the responsible owner, operation/change scope,
preconditions, human authorization, validation checkpoint and rollback boundary.
Advice must not imply that the action has already occurred. A generic instruction
to fix the incident is not a bounded approval for a payment operation.

Original outcome and applicable policy must be established before recovery review.
Unknown or conflicting records require source-owner clarification. Security
controls must not be bypassed. Amount/idempotency prerequisites in PL-102 remain
mandatory; a report decision cannot waive them.

## Escalation and communication
Unresolved investigations must name the exact missing source, responsible owner,
affected tenant/operation/window and distinguishing question. Incident Commander
coordinates checkpoints without inventing severity, deadlines or regulatory claims.
The copilot must not contact owners or external parties automatically.

Rollback plans must distinguish configuration/projection changes from completed
downstream operations. A configuration rollback cannot undo a capture, refund,
release or delivered event. Compensating operations require their own review.
<!-- page -->
# Governance and verification
## Responsibilities
| Role | Authority boundary |
|---|---|
| Payment Operations Analyst | Review exact report, evidence and gaps; record attributable decision |
| Incident Commander | Coordinate owners, escalation and checkpoints; no blanket execution authority |
| Stage / Platform / Security Owner | Review separately controlled actions and their validation/rollback scope |
| Knowledge Approver | Review versions and changes without approving an incident report |

## Exceptions and audit
There is no automatic-action exception. A missing source, deadline or approval
must remain unresolved rather than assumed. An unsafe recommendation requires
human rejection/review with the original report retained; do not mutate history
to make earlier advice compliant.

Retain model/prompt/evidence/retrieval metadata, exact report and human decision,
owner authorization references where separately available, validation outcome
and rollback limits. Never invent an action record to complete an audit timeline.

## Review triggers and current capability
Review this policy when model/report behavior, source authority or controlled
recovery procedures change. New execution capabilities would require a separate
explicit decision; this package authorizes none. E10/E11 are future sources and
current MCP is read-only aggregate evidence. Q6 LOW/null remains unchanged for
degraded/empty report evidence. Fictional APPROVED document metadata identifies
knowledge governance only, never the operator's terminal report decision.

## Related guidance and revision
Related documents: PL-101, PL-102, PL-104, RB-110, RB-111.

Version 1.0.0, effective 2026-10-01. Initial independent risk-derived issue.
Review cadence: annual, or before a material domain/source/control change.
