---
documentKey: PL-003
documentId: 2fb692f9-ed4e-4a76-bce0-e1be090285d4
tenantId: 8b860d80-d17f-4e6b-8c48-af35f26a4d61
type: POLICY
title: Human Review and Operational Action Authority Policy
version: 2.0.0
incidentFamily: AUTHORIZATION_DECLINE_RATE_SPIKE
appliesTo: AI-assisted reports and operational decisions
approvalStatus: APPROVED
approvedBy: 7b636625-53d1-46f7-92a9-9c8c27a243d1
approvedAt: 2026-08-30T08:00:00Z
effectiveAt: 2026-08-30T09:00:00Z
ownerRole: Operational Risk
classification: Internal - Synthetic Demo
replacement: None
requiredCodes: None
relatedDocuments: PL-001, PL-005
generatorVersion: synten-pdf-generator/v1
---
# Human Review and Operational Action Authority Policy

[[PAGEBREAK]]
## Document control

| Control | Value |
|---|---|
| Owner | Operational Risk |
| Audience | Payment Operations, Incident Management, technical owners, and reviewers |
| Review cadence | Every 180 days and after a material control or corpus change |
| Related documents | PL-001, PL-005 |
| Retrieval role | Prohibits model approval or automatic remediation and defines human checkpoints. |
| Classification | Internal - Synthetic Demo |

### Revision history

| Version | Status | Date | Summary |
|---|---|---|---|
| 2.0.0 | APPROVED | 2026-08-30 | Controlled corpus version for authorization-decline governance. |

## 1. Purpose

This policy establishes mandatory SynTen Inc controls for ai-assisted reports and operational decisions. It supports auditable investigation of the synthetic authorization-decline incident family while preserving evidence integrity and human authority.

## 2. Scope

The policy applies to synthetic alerts, evidence attempts, approved knowledge, retrieval context, AI-assisted reports, human decisions, escalation records, and audit projections associated with the tenant ID shown on the cover.

It does not authorize real payment processing, access to real customer or merchant data, production remediation, external communication, or model approval of an operational decision.

## 3. Governing principles

- Observed facts, inference, approved guidance, recommendation, and human decision must remain distinguishable.
- Tenant, source, version, time, status, model, retrieval, and decision metadata must remain reviewable.
- Missing or contradictory information must reduce confidence rather than invite fabrication.
- No recommendation executes automatically; human authority remains outside the model boundary.

## 4. Mandatory controls

1. The copilot may propose observations, inferences, probable cause, confidence, and a recommendation; it may not approve any of them.
2. No model output may retry a payment, reroute traffic, change configuration, rotate credentials, restart a service, or contact a third party.
3. A human reviewer must compare citations with the displayed evidence and approved knowledge before approval or rejection.
4. Rejected reports remain immutable and auditable; rejection must not be rewritten as model failure or silent regeneration.

## 5. Roles and responsibilities

| Role | Responsible | Accountable | Consulted or informed |
|---|---|---|---|
| Operational Risk | Maintain this policy and control evidence. | Approve content through the synthetic role account. | Incident Management and affected technical owners. |
| Payment Operations Analyst | Apply the policy during investigation and record limitations. | Accountable for the submitted human review decision. | Incident Commander and evidence owners. |
| Technical owner | Confirm component-specific facts and authorize separate actions. | Accountable for changes within that owner's controlled system. | Payment Operations and security where applicable. |
| Knowledge Approver | Verify version, status, classification, and retrieval eligibility. | Accountable for approved/effective publication. | Document owner and Operational Assurance. |

## 6. Evidence and records

Retain the authoritative incident, evidence attempt, retrieval attempt, selected chunk references, report attempt, model and prompt metadata, human decision, and chronological audit projection. Do not replace a failed or superseded record with a cleaner narrative.

Control evidence must use opaque synthetic identifiers and UTC timestamps. Raw credentials, private endpoints, plausible personal or payment data, and unbounded provider payloads are prohibited.

## 7. Exceptions and non-compliance

There is no exception allowing a model to approve a report, execute a recommendation, use cross-tenant data, or conceal missing evidence. A human may document an operational exception outside the copilot only through the separately governed process and must preserve its owner, rationale, scope, and expiry.

A detected policy breach must be recorded as a limitation, escalated to the owner, and excluded from trusted report support until corrected. Do not silently relabel an ineligible document or malformed source as approved evidence.

## 8. Monitoring and review

Review retrieval eligibility, source/version hashes, unexplained ranking changes, missing citations, rejected reports, evidence gaps, and attempted automatic actions. Material findings require policy review and a new version rather than an in-place rewrite.

## 9. Related documents

Apply this policy with PL-001, PL-005. Where guidance conflicts, use only the approved and effective version and preserve the conflict for audit.
