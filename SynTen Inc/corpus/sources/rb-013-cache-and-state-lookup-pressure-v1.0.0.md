---
documentKey: RB-013
documentId: 2359d4c4-ec5f-4e97-993a-07dbc18a71c9
tenantId: 8b860d80-d17f-4e6b-8c48-af35f26a4d61
type: RUNBOOK
title: Cache and State-Lookup Pressure Runbook
version: 1.0.0
incidentFamily: AUTHORIZATION_DECLINE_RATE_SPIKE
appliesTo: Cache stampedes and state lookup latency
approvalStatus: APPROVED
approvedBy: 7b636625-53d1-46f7-92a9-9c8c27a243d1
approvedAt: 2026-08-30T08:00:00Z
effectiveAt: 2026-08-30T09:00:00Z
ownerRole: Authorization Service
classification: Internal - Synthetic Demo
replacement: None
requiredCodes: AUTHORIZATION_STATE_LOOKUP_TIMEOUT,CACHE_STAMPEDE
relatedDocuments: RB-004, RB-007, PL-002
generatorVersion: synten-pdf-generator/v1
---
# Cache and State-Lookup Pressure Runbook

[[PAGEBREAK]]
## Document control

| Control | Value |
|---|---|
| Owner | Authorization Service |
| Audience | Payment Operations Analyst, Incident Commander, and named technical owner |
| Review cadence | Every 180 days and after a material synthetic scenario change |
| Related documents | RB-004, RB-007, PL-002 |
| Retrieval role | Correlate cache misses with downstream state pressure. |
| Classification | Internal - Synthetic Demo |

### Revision history

| Version | Status | Date | Summary |
|---|---|---|---|
| 1.0.0 | APPROVED | 2026-08-30 | Controlled corpus version for authorization-decline investigation. |

## 1. Purpose and scope

This runbook guides a human analyst investigating cache stampedes and state lookup latency. Its operational purpose is: Correlate cache misses with downstream state pressure. It applies only to persisted SynTen Inc synthetic evidence for the current tenant and investigation.

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

- `AUTHORIZATION_STATE_LOOKUP_TIMEOUT`
- `CACHE_STAMPEDE`

| Exact signal | Bounded interpretation | Required caution |
|---|---|---|
| `AUTHORIZATION_STATE_LOOKUP_TIMEOUT` | Concurrent cache misses created a cache stampede and overloaded authorization state lookups. | Treat as observed only when the persisted window contains the code; the cause remains an inference. |
| `CACHE_STAMPEDE` | Concurrent cache misses created a cache stampede and overloaded authorization state lookups. | Treat as observed only when the persisted window contains the code; the cause remains an inference. |

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
| S014 | HIGH | CACHE_STAMPEDE, AUTHORIZATION_STATE_LOOKUP_TIMEOUT | Concurrent cache misses created a cache stampede and overloaded authorization state lookups. Confirm the stated required evidence before using this as probable cause. |

## 7. Failure and uncertainty handling

- AVAILABLE with an empty error list is valid negative evidence for that exact source and window; it is not an unavailable result.
- PARTIAL means missing partitions may contain confirming or contradictory observations. State the missing scope and use LOW confidence unless independent evidence closes it.
- UNAVAILABLE or TIMED_OUT means no technical cause can be concluded from this source. Preserve the attempt and seek an approved independent source.
- Contradictory timestamps, routes, instance groups, or error ordering must appear in the report as contradictions, not be averaged away.
- A retrieved weak match may suggest a check, but it cannot override stronger observed evidence or an approval-status filter.

## 8. Escalation package

Escalate to Authorization Service with the incident and investigation identifiers, bounded UTC window, service name, exact error codes and counts, affected opaque route or cohort, evidence status, selected knowledge references, contradictions, and the next requested human confirmation.

Do not include credentials, raw provider payloads, private endpoints, plausible merchant data, or an unreviewed model conclusion. Record the receiving owner and next checkpoint; sending the package remains a human action outside the copilot.

## 9. Validation and closure checks

1. Recollect the same approved evidence source only through a separately authorized operator action and compare equivalent windows.
2. Confirm whether the named error categories and decline-rate signal have returned toward the synthetic baseline.
3. Record any separately authorized change, its owner, validation result, and rollback decision without attributing it to the model.
4. Generate a report only from persisted evidence and retrieval snapshots; review every citation before approval or rejection.
5. Retain failed attempts, negative evidence, and superseded hypotheses in the audit timeline.

## 10. Related documents

Use RB-004, RB-007, PL-002 for adjacent diagnostic or governance requirements. Only approved and effective versions are retrieval eligible.

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
