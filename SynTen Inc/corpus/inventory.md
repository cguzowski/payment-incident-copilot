# SynTen Inc authorization knowledge corpus inventory

Status: Implementation-ready
Corpus version: `synten-auth-knowledge/v1`
Tenant ID: `8b860d80-d17f-4e6b-8c48-af35f26a4d61`
Incident family: `AUTHORIZATION_DECLINE_RATE_SPIKE`
Total planned PDFs: 30
Hard page limit: 15 pages per PDF, inclusive

## Distribution

| Type | Approved versions | Superseded challenge versions | Total PDFs |
|---|---:|---:|---:|
| Runbook | 21 | 1 | 22 |
| Policy | 6 | 2 | 8 |
| Total | 27 | 3 | 30 |

The 27 approved versions provide current operational guidance. The three
superseded versions are realistic historical records with deliberately
overlapping language. They exist to prove that approval/version filters exclude
tempting but ineligible content before lexical or vector ranking.

## Metadata contract

Every row below is a distinct document version. Unless a row says otherwise,
it has these exact values:

| Field | Value |
|---|---|
| `tenantId` | `8b860d80-d17f-4e6b-8c48-af35f26a4d61` |
| `incidentFamily` | `AUTHORIZATION_DECLINE_RATE_SPIKE` |
| `approvedBy` | `7b636625-53d1-46f7-92a9-9c8c27a243d1` |
| Approved `approvedAt` | `2026-08-30T08:00:00Z` |
| Approved `effectiveAt` | `2026-08-30T09:00:00Z` |
| Superseded `approvedAt` | `2026-06-01T08:00:00Z` |
| Superseded `effectiveAt` | `2026-06-02T09:00:00Z` |
| Classification | `Internal — Synthetic Demo` |
| Source format | Maintained Markdown rendered to a text-based PDF |
| Source path | `SynTen Inc/corpus/sources/<pdf-basename>.md` |
| PDF path | `SynTen Inc/corpus/pdfs/<pdf-filename>` |

`approvedBy` is the existing fictional role account defined in `../profile.md`.
The inventory is authoritative for membership and metadata. K2 records source
and PDF SHA-256 hashes only after the files exist; hashes must never be guessed.

Target page ranges are editorial budgets, not permission to exceed the hard
limit. A document may finish below its range when complete, but any PDF with
more than 15 pages fails generation acceptance.

## Runbooks

All rows in this section have `type: RUNBOOK`.

| Key | Document ID | PDF filename | Title | Version | Status | Applies to | Target pages | Retrieval role | Scenario coverage |
|---|---|---|---|---|---|---|---:|---|---|
| RB-001 | `66a84fed-3d77-4e7e-9a1b-e25ff37e2280` | `rb-001-authorization-decline-incident-triage-v2.0.0.pdf` | Authorization Decline Incident Triage Runbook | `2.0.0` | `APPROVED` | Card authorization incident triage | 7-9 | Initial assessment, evidence status, time-window comparison, and escalation entry point. | S001-S211 |
| RB-002 | `2ddd45e5-f729-4d86-a15b-39a4789159c3` | `rb-002-gateway-connectivity-and-timeouts-v2.0.0.pdf` | Gateway Connectivity and Timeout Runbook | `2.0.0` | `APPROVED` | Gateway transport failures | 8-10 | Primary guidance for timeout and connection-reset clusters. | S001, S002, S006 |
| RB-003 | `f6caf241-e194-4b9f-adc3-a914343f2052` | `rb-003-gateway-rate-limits-and-maintenance-v1.0.0.pdf` | Gateway Rate Limit and Maintenance Runbook | `1.0.0` | `APPROVED` | Gateway capacity and maintenance responses | 7-9 | Distinguish traffic limiting from planned gateway maintenance. | S003, S009 |
| RB-004 | `12c5326e-28b7-4a77-bdea-50836bbdc9ae` | `rb-004-authorization-state-database-v1.0.0.pdf` | Authorization State Database Runbook | `1.0.0` | `APPROVED` | Database pools, locks, and state operations | 9-11 | Correlate pool exhaustion or lock contention with state lookup/update failures. | S004, S108 |
| RB-005 | `0562dfb8-5d09-4cbe-a8d3-a27d21fdf238` | `rb-005-issuer-response-anomalies-v1.0.0.pdf` | Issuer Response Anomaly Runbook | `1.0.0` | `APPROVED` | Issuer response categories and malformed responses | 8-10 | Separate issuer-originated declines from transport or mapping failures. | S005, S110 |
| RB-006 | `7797ce64-f500-451c-8359-21757f3a6d26` | `rb-006-retry-and-circuit-breaker-failures-v1.0.0.pdf` | Retry and Circuit-Breaker Failure Runbook | `1.0.0` | `APPROVED` | Retry budgets and synchronized breaker behavior | 8-10 | Interpret causal ordering and amplification without changing retry controls. | S006, S210 |
| RB-007 | `2bee1115-f772-4922-be92-74ee6cbd2a1b` | `rb-007-service-capacity-and-deadlines-v1.0.0.pdf` | Authorization Service Capacity and Deadline Runbook | `1.0.0` | `APPROVED` | CPU saturation and request deadlines | 7-9 | Diagnose application saturation while avoiding automatic scaling or restart. | S007 |
| RB-008 | `3896ec1d-e07a-40a8-8bf3-feb5b26a6408` | `rb-008-route-configuration-and-feature-flags-v1.0.0.pdf` | Route Configuration and Feature-Flag Runbook | `1.0.0` | `APPROVED` | Route selection, configuration versions, and cohorts | 9-11 | Compare approved route configuration, flag assignment, and instance decisions. | S008, S105, S201 |
| RB-009 | `7f7664e5-06fb-4621-874b-c56aafdecd43` | `rb-009-merchant-profile-validation-v1.0.0.pdf` | Merchant Profile Validation Runbook | `1.0.0` | `APPROVED` | Opaque merchant configuration and normalization | 8-10 | Diagnose invalid or unparseable profiles without exposing profile data. | S010, S207 |
| RB-010 | `7c4b2647-b011-46a6-907d-c21a632559ac` | `rb-010-fraud-rejection-surge-v1.0.0.pdf` | Fraud Rejection Surge Runbook | `1.0.0` | `APPROVED` | Synthetic fraud-decision cohorts | 6-8 | Require rule-version evidence before attributing declines to fraud decisions. | S011 |
| RB-011 | `85493c0b-d4db-45bb-a6d4-cede414b4789` | `rb-011-network-dns-and-external-routing-v1.0.0.pdf` | Network, DNS, and External Routing Runbook | `1.0.0` | `APPROVED` | Packet loss, DNS, resolver policy, and BGP paths | 10-12 | Distinguish application health from regional or external path failure. | S012, S102, S202, S208 |
| RB-012 | `66c791ca-0b67-467e-a5b0-edfe5cd3335e` | `rb-012-gateway-credential-rejection-v1.0.0.pdf` | Gateway Credential Rejection Runbook | `1.0.0` | `APPROVED` | Opaque gateway connection credentials | 6-8 | Escalate credential state without exposing or rotating secrets. | S013 |
| RB-013 | `2359d4c4-ec5f-4e97-993a-07dbc18a71c9` | `rb-013-cache-and-state-lookup-pressure-v1.0.0.pdf` | Cache and State-Lookup Pressure Runbook | `1.0.0` | `APPROVED` | Cache stampedes and state lookup latency | 7-9 | Correlate cache misses with downstream state pressure. | S014 |
| RB-014 | `aae2e3b7-4245-4197-a062-c7d5e7e2d978` | `rb-014-tls-certificate-and-status-validation-v1.0.0.pdf` | TLS Certificate and Status Validation Runbook | `1.0.0` | `APPROVED` | TLS handshake, expiry, and OCSP validation | 9-11 | Separate certificate expiry from status-responder failure without bypassing checks. | S101, S203 |
| RB-015 | `b87eab70-680c-4336-9b63-bbb6441dd3aa` | `rb-015-time-signature-and-entropy-v1.0.0.pdf` | Time, Request Signature, and Entropy Runbook | `1.0.0` | `APPROVED` | Clock skew, signing deadlines, and secure randomness | 9-11 | Correlate timestamp, signature, or entropy signals while preserving security controls. | S103, S206 |
| RB-016 | `3bba9f8d-9cce-4e8f-b37a-bc0b44f7aee8` | `rb-016-hsm-signing-and-quorum-v1.0.0.pdf` | HSM Signing, Quorum, and Protocol Runbook | `1.0.0` | `APPROVED` | Synthetic HSM latency, quorum, and failover | 10-12 | Diagnose signing dependencies without automated failover, restart, or firmware action. | S104, S204, S209 |
| RB-017 | `92d42eda-924f-4532-a4e0-ea54ca16f839` | `rb-017-response-schema-and-mapping-v1.0.0.pdf` | Gateway and Issuer Response Mapping Runbook | `1.0.0` | `APPROVED` | Response schemas, malformed payload categories, and mappings | 8-10 | Distinguish upstream schema change from local mapping failure. | S106, S110 |
| RB-018 | `4885ba6d-5ada-4b31-9497-93cc3ba452e9` | `rb-018-regional-failover-and-config-consistency-v1.0.0.pdf` | Regional Failover and Configuration Consistency Runbook | `1.0.0` | `APPROVED` | Regional health decisions and configuration convergence | 9-11 | Diagnose failover loops and split-brain route decisions without forcing failover. | S107, S201 |
| RB-019 | `16b2705a-33f8-44c6-88f5-4aae0551891d` | `rb-019-bin-and-issuer-reference-routing-v1.0.0.pdf` | BIN and Issuer Reference Routing Runbook | `1.0.0` | `APPROVED` | Synthetic BIN tables and issuer route lookup | 7-9 | Verify reference-data version and refresh evidence before route attribution. | S109 |
| RB-020 | `6e09c65f-bd79-4df3-8caa-a5f8178aff9e` | `rb-020-token-vault-and-partial-observability-v1.0.0.pdf` | Token Vault and Partial Observability Runbook | `1.0.0` | `APPROVED` | Tokenized authorization and incomplete partitions | 8-10 | Preserve partial evidence and require independent confirmation. | S111 |
| RB-021 | `9701f124-9b67-4eb6-9bd9-18dcc5f3a768` | `rb-021-recurring-authorization-calendar-rules-v1.0.0.pdf` | Recurring Authorization Calendar-Rule Runbook | `1.0.0` | `APPROVED` | Date-sensitive recurring authorization rules | 7-9 | Diagnose bounded cohort and calendar-rule failures without replaying payments. | S205 |
| RB-022 | `2ddd45e5-f729-4d86-a15b-39a4789159c3` | `rb-022-legacy-gateway-recovery-v1.2.0.pdf` | Legacy Gateway Recovery Runbook | `1.2.0` | `SUPERSEDED` | Gateway transport failures | 5-7 | Hard negative with obsolete failover and retry language; superseded by RB-002 and RB-006. | S001, S006, S107 |

## Policies

All rows in this section have `type: POLICY`.

| Key | Document ID | PDF filename | Title | Version | Status | Applies to | Target pages | Retrieval role | Scenario coverage |
|---|---|---|---|---|---|---|---:|---|---|
| PL-001 | `f3e31211-e2ad-4a8e-b504-671f9be1a160` | `pl-001-payment-incident-response-governance-v2.0.0.pdf` | Payment Incident Response Governance Policy | `2.0.0` | `APPROVED` | Authorization incident lifecycle and review | 9-11 | Core investigation, evidence, report, decision, and audit requirements. | All scenarios |
| PL-002 | `06925372-3679-45b0-8be3-7cdb9f7a86ad` | `pl-002-evidence-integrity-and-provenance-v1.0.0.pdf` | Evidence Integrity and Source Provenance Policy | `1.0.0` | `APPROVED` | Evidence collection, gaps, contradiction, and citation | 9-11 | Mandatory treatment of available, partial, empty, unavailable, and conflicting sources. | All scenarios; especially S111 and S211 |
| PL-003 | `2fb692f9-ed4e-4a76-bce0-e1be090285d4` | `pl-003-human-review-and-action-authority-v2.0.0.pdf` | Human Review and Operational Action Authority Policy | `2.0.0` | `APPROVED` | AI-assisted reports and operational decisions | 8-10 | Prohibits model approval or automatic remediation and defines human checkpoints. | All scenarios |
| PL-004 | `1baf4e46-4833-48bc-aa6d-52ef8b2c4441` | `pl-004-knowledge-lifecycle-and-approval-v1.0.0.pdf` | Operational Knowledge Lifecycle and Approval Policy | `1.0.0` | `APPROVED` | Runbook/policy ownership, versions, and retrieval eligibility | 8-10 | Defines approval, effective version, supersession, review, and source provenance. | All scenarios |
| PL-005 | `0ab7da4b-81bc-45ff-b6ed-8f9e3cabface` | `pl-005-synthetic-data-handling-v1.0.0.pdf` | Synthetic Payment Data Handling Policy | `1.0.0` | `APPROVED` | Synthetic identifiers, examples, prompts, logs, and escalation records | 7-9 | Prevents real or sensitive data and limits payload disclosure. | All scenarios; especially S013, S110, S207 |
| PL-006 | `874ebdf7-024c-466a-904a-2aa3cdce16f9` | `pl-006-third-party-escalation-and-communications-v2.0.0.pdf` | Third-Party Escalation and Incident Communications Policy | `2.0.0` | `APPROVED` | Gateway, issuer, network, and security escalation | 9-11 | Defines bounded evidence packages, owners, communication, and confirmation. | S001-S003, S005, S009, S012-S013, S101-S104, S202-S204, S208-S209 |
| PL-007 | `874ebdf7-024c-466a-904a-2aa3cdce16f9` | `pl-007-legacy-emergency-routing-policy-v1.1.0.pdf` | Legacy Emergency Routing Policy | `1.1.0` | `SUPERSEDED` | Gateway and regional routing incidents | 6-8 | Hard negative with older emergency-routing terminology; superseded by PL-006. | S001, S009, S107, S202 |
| PL-008 | `2fb692f9-ed4e-4a76-bce0-e1be090285d4` | `pl-008-legacy-ai-incident-automation-policy-v1.0.0.pdf` | Legacy AI Incident Automation Policy | `1.0.0` | `SUPERSEDED` | AI-assisted investigation and action | 6-8 | Hard negative with obsolete automation language; superseded by PL-003. | All scenarios |

## Coverage map

The runbook set covers every generator scenario at least once with approved
primary guidance:

| Coverage group | Approved runbooks | Scenarios |
|---|---|---|
| Baseline triage and evidence status | RB-001 | S001-S211 |
| Gateway transport, capacity, and maintenance | RB-002, RB-003, RB-006 | S001-S003, S006, S009, S210 |
| Service state, capacity, and cache | RB-004, RB-007, RB-013 | S004, S007, S014, S108 |
| Issuer, response, and reference data | RB-005, RB-017, RB-019 | S005, S106, S109, S110 |
| Routing, flags, regions, and configuration | RB-008, RB-018 | S008, S105, S107, S201 |
| Merchant and fraud cohorts | RB-009, RB-010 | S010, S011, S207 |
| Network and gateway security | RB-011, RB-012, RB-014 | S012, S013, S101, S102, S202, S203, S208 |
| Signing and cryptographic services | RB-015, RB-016 | S103, S104, S204, S206, S209 |
| Tokenized and recurring authorization | RB-020, RB-021 | S111, S205 |
| Evidence unavailable | RB-001 with PL-002 | S211 |

Each approved runbook owns a distinct diagnostic decision or dependency family.
Cross-cutting policies define governance rather than restating technical steps.
RB-022, PL-007, and PL-008 add exclusion pressure but do not count as approved
coverage.

## Supersession relationships

| Superseded version | Current approved replacement | Required retrieval behavior |
|---|---|---|
| RB-022 `1.2.0` | RB-002 `2.0.0` and RB-006 `1.0.0` | Exclude RB-022 before ranking even on exact legacy phrases. |
| PL-007 `1.1.0` | PL-006 `2.0.0` | Exclude PL-007 before ranking for emergency routing queries. |
| PL-008 `1.0.0` | PL-003 `2.0.0` | Exclude PL-008 before ranking for AI action or approval queries. |

## Generation acceptance

K2 may generate only the 30 PDFs listed here. It must maintain one Markdown
source and one PDF per row, then record actual page count and SHA-256 hashes in
a generated validation manifest. Generation fails if any row is missing, any
extra PDF exists, metadata differs, a source/PDF pair is not reproducible, or a
PDF has fewer than one or more than 15 pages.
