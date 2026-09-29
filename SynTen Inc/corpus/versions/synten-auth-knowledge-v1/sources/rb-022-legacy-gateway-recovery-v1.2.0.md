---
documentKey: RB-022
documentId: 2ddd45e5-f729-4d86-a15b-39a4789159c3
tenantId: 8b860d80-d17f-4e6b-8c48-af35f26a4d61
type: RUNBOOK
title: Legacy Gateway Recovery Runbook
version: 1.2.0
incidentFamily: AUTHORIZATION_DECLINE_RATE_SPIKE
appliesTo: Gateway transport failures
approvalStatus: SUPERSEDED
approvedBy: 7b636625-53d1-46f7-92a9-9c8c27a243d1
approvedAt: 2026-06-01T08:00:00Z
effectiveAt: 2026-06-02T09:00:00Z
ownerRole: Gateway Integration
classification: Internal - Synthetic Demo
replacement: RB-002 and RB-006
requiredCodes: GATEWAY_TIMEOUT,REGIONAL_FAILOVER_LOOP,RETRY_BUDGET_EXHAUSTED,UPSTREAM_CONNECTION_RESET
relatedDocuments: RB-002, RB-006
generatorVersion: synten-pdf-generator/v1
---
# Legacy Gateway Recovery Runbook

[[PAGEBREAK]]
## Document control

| Control | Value |
|---|---|
| Owner | Gateway Integration |
| Audience | Payment Operations Analyst, Incident Commander, and named technical owner |
| Review cadence | Every 180 days and after a material synthetic scenario change |
| Related documents | RB-002, RB-006 |
| Retrieval role | Hard negative with obsolete failover and retry language; superseded by RB-002 and RB-006. |
| Classification | Internal - Synthetic Demo |

### Revision history

| Version | Status | Date | Summary |
|---|---|---|---|
| 1.2.0 | SUPERSEDED | 2026-08-30 | Controlled corpus version for authorization-decline investigation. |

## 1. Purpose and scope

This runbook guides a human analyst investigating gateway transport failures. Its operational purpose is: Hard negative with obsolete failover and retry language; superseded by RB-002 and RB-006. It applies only to persisted SynTen Inc synthetic evidence for the current tenant and investigation.

The runbook does not establish that a described failure occurred. An alert is a signal; approved knowledge is guidance; probable cause remains an inference that must be supported by cited evidence.

## 2. Entry conditions and authority

- Confirm the incident is INVESTIGATING and the tenant and investigation identifiers match the evidence request.
- Record the alert window, evidence observation window, collection status, source, and retrieval timestamp before interpretation.
- Stop and preserve the gap when evidence is unavailable, malformed, stale, cross-tenant, or outside the incident window.
- Use this document to diagnose and prepare an escalation. The copilot may not execute recovery, approve a report, or contact an owner.

## 3. Evidence prerequisites

| Evidence | Minimum check | Why it matters |
|---|---|---|
| Service-error observation | Persisted status, service name, bounded UTC window, exact codes, and aggregate counts | Separates observed facts from a generic runbook match. |
| Alert context | Detected time, received time, duration, affected opaque route or cohort, and severity | Establishes the comparison window and operational scope. |
| Knowledge context | Approved document version, chunk identity, and retrieval status | Makes later recommendations traceable without treating guidance as proof. |
| Independent confirmation | Named source or owner when the runbook calls for it | Prevents a single synthetic signal from becoming an unsupported conclusion. |

## 4. Signal interpretation

Exact machine codes in scope:

- `GATEWAY_TIMEOUT`
- `REGIONAL_FAILOVER_LOOP`
- `RETRY_BUDGET_EXHAUSTED`
- `UPSTREAM_CONNECTION_RESET`

| Exact signal | Bounded interpretation | Required caution |
|---|---|---|
| `GATEWAY_TIMEOUT` | The primary payment gateway became intermittently unreachable, producing a concentrated timeout and connection-reset burst during the alert window. | Treat as observed only when the persisted window contains the code; the cause remains an inference. |
| `REGIONAL_FAILOVER_LOOP` | Competing regional health decisions created a failover loop while the gateway was timing out. | Treat as observed only when the persisted window contains the code; the cause remains an inference. |
| `RETRY_BUDGET_EXHAUSTED` | A gateway timeout burst triggered excessive retries that exhausted the authorization retry budget and amplified load. | Treat as observed only when the persisted window contains the code; the cause remains an inference. |
| `UPSTREAM_CONNECTION_RESET` | The primary payment gateway became intermittently unreachable, producing a concentrated timeout and connection-reset burst during the alert window. | Treat as observed only when the persisted window contains the code; the cause remains an inference. |

## 5. Diagnostic procedure

1. Anchor the analysis to the alert and observation windows. Reject a causal ordering that the timestamps do not support.
2. Confirm the service is `payment-authorization` and group counts by exact code, route or cohort, and observation time.
3. Compare co-occurring signals before preferring a single-component explanation; preserve absent expected signals as negative evidence.
4. Check whether the evidence status is AVAILABLE or PARTIAL and identify any missing partition, region, route, or instance group.
5. Request the named independent owner or approved source needed to confirm the candidate cause; do not manufacture unavailable context.
6. Record alternative explanations whose expected signals are missing, weak, or contradictory.

## 6. Scenario decision matrix

| Scenario | Severity | Observed signal | Candidate explanation and reviewer checkpoint |
|---|---|---|---|
| S001 | CRITICAL | GATEWAY_TIMEOUT, UPSTREAM_CONNECTION_RESET | The primary payment gateway became intermittently unreachable, producing a concentrated timeout and connection-reset burst during the alert window. Confirm the stated required evidence before using this as probable cause. |
| S006 | CRITICAL | RETRY_BUDGET_EXHAUSTED, GATEWAY_TIMEOUT | A gateway timeout burst triggered excessive retries that exhausted the authorization retry budget and amplified load. Confirm the stated required evidence before using this as probable cause. |
| S107 | CRITICAL | REGIONAL_FAILOVER_LOOP, GATEWAY_TIMEOUT | Competing regional health decisions created a failover loop while the gateway was timing out. Confirm the stated required evidence before using this as probable cause. |

## 7. Failure and uncertainty handling

- AVAILABLE with an empty error list is valid negative evidence for that exact source and window; it is not an unavailable result.
- PARTIAL means missing partitions may contain confirming or contradictory observations. State the missing scope and use LOW confidence unless independent evidence closes it.
- UNAVAILABLE or TIMED_OUT means no technical cause can be concluded from this source. Preserve the attempt and seek an approved independent source.
- Contradictory timestamps, routes, instance groups, or error ordering must appear in the report as contradictions, not be averaged away.
- A retrieved weak match may suggest a check, but it cannot override stronger observed evidence or an approval-status filter.

## 8. Escalation package

Escalate to Gateway Integration with the incident and investigation identifiers, bounded UTC window, service name, exact error codes and counts, affected opaque route or cohort, evidence status, selected knowledge references, contradictions, and the next requested human confirmation.

Do not include credentials, raw provider payloads, private endpoints, plausible merchant data, or an unreviewed model conclusion. Record the receiving owner and next checkpoint; sending the package remains a human action outside the copilot.

## 9. Validation and closure checks

1. Recollect the same approved evidence source only through a separately authorized operator action and compare equivalent windows.
2. Confirm whether the named error categories and decline-rate signal have returned toward the synthetic baseline.
3. Record any separately authorized change, its owner, validation result, and rollback decision without attributing it to the model.
4. Generate a report only from persisted evidence and retrieval snapshots; review every citation before approval or rejection.
5. Retain failed attempts, negative evidence, and superseded hypotheses in the audit timeline.

## 10. Related documents

Use RB-002, RB-006 for adjacent diagnostic or governance requirements. Only approved and effective versions are retrieval eligible.

## 11. Required investigation record

Complete the following record in the incident workspace before requesting review. Use explicit `Not observed`, `Unavailable`, or `Not applicable` values instead of leaving fields blank.

| Record group | Required entry | Reviewer checkpoint |
|---|---|---|
| Correlation | Tenant ID, incident ID, investigation ID, alert ID, and UTC observation window | All identifiers resolve to the same tenant-scoped investigation. |
| Evidence | Source, collection status, retrieval time, exact codes, aggregate counts, and missing partitions | Observations are distinguishable from unavailable or partial evidence. |
| Knowledge | Document key, version, approval status, retrieval query, and selected chunk references | Every cited item was approved and effective at retrieval time. |
| Assessment | Observed facts, candidate inference, confidence, alternatives, contradictions, and limitations | The inference does not exceed the cited evidence. |
| Handoff | Receiving owner, requested confirmation, next checkpoint, and escalation time | The request is bounded and no operational action is attributed to the copilot. |
| Review | Handoff state (`READY FOR REVIEW`, `INSUFFICIENT EVIDENCE`, or `ESCALATED FOR CONFIRMATION`), reviewer identity and time, accepted or rejected citations, disposition, and follow-up owner | Approval and operational action remain human decisions outside this runbook. |
