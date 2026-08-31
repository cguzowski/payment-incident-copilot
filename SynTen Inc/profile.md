# SynTen Inc operating profile

Status: Approved for corpus authoring
Profile version: 1.0
Tenant ID: `8b860d80-d17f-4e6b-8c48-af35f26a4d61`
Incident family: `AUTHORIZATION_DECLINE_RATE_SPIKE`
Data classification: Internal — Synthetic Demo

## Company summary

SynTen Inc is a fictional payment-technology company used only by this
repository. It operates a synthetic card-authorization orchestration service
for fictional merchant cohorts and routes authorization requests to simulated
external gateways and issuer connections. It does not capture or settle funds,
store real cardholder data, or represent a real legal entity.

The corpus should read like the controlled operational library of a mature
24-hour payment service while remaining explicit that every system, event,
identifier, measurement, role, and procedure is synthetic.

## Demonstration business context

- Product surface: synthetic card authorization only.
- Primary service: `payment-authorization`.
- Supported scenario: an abnormal authorization-decline rate requiring human
  investigation.
- Operating footprint: two fictional service regions and multiple simulated
  gateway routes; documents use opaque labels such as `region-a`, `region-b`,
  `route-blue`, and `route-green`.
- Customer and merchant references: opaque synthetic cohort identifiers only,
  such as `merchant-cohort-07`; never use plausible personal or payment data.
- External parties: simulated gateways, issuer connections, certificate
  services, network paths, and security dependencies.
- Decision model: the copilot assembles evidence and advice; a human operator
  approves or rejects the report and separately authorizes any operational act.

No document may imply that SynTen Inc moves real money, holds a regulated
license, has real customers, or has a real-world corporate history.

## Synthetic platform map

| Capability | Fictional component | Operational responsibility |
|---|---|---|
| Authorization orchestration | `payment-authorization` | Validate and route synthetic authorization requests. |
| Gateway connectivity | `gateway-connector` | Maintain simulated upstream connections and response mapping. |
| Routing configuration | `route-config` | Resolve approved routes for opaque merchant and issuer cohorts. |
| Authorization state | `authorization-state` | Persist synthetic request state in PostgreSQL-backed storage. |
| Hot state cache | `authorization-cache` | Reduce repeated state lookups without becoming source of record. |
| Token dependency | `token-vault-sim` | Supply opaque synthetic token references. |
| Cryptographic dependency | `hsm-sim` | Simulate signing, quorum, and protocol behavior. |
| Reference data | `issuer-reference-data` | Supply synthetic BIN and issuer routing metadata. |
| Fraud decision input | `fraud-decision-sim` | Return synthetic rule outcomes for test cohorts. |
| Observability | `ops-observability` | Return bounded aggregate service-error evidence through MCP. |

The names above are corpus vocabulary, not new deployable services authorized
for implementation.

## Operating roles and authority

| Role | Corpus responsibility | Authority boundary |
|---|---|---|
| Payment Operations Analyst | Triage alerts, collect approved evidence, review retrieved guidance, and submit a human report decision. | May not let the copilot execute remediation. |
| Incident Commander | Coordinate severity, communication, ownership, and decision checkpoints. | Authorizes coordination, not technical changes outside normal access controls. |
| Authorization Service Owner | Assess service capacity, database, cache, deadline, and application behavior. | Executes changes only through separate controlled procedures. |
| Gateway Integration Owner | Assess gateway transport, rate limits, maintenance, credentials, and response compatibility. | Must not disclose secrets or raw sensitive payloads. |
| Network Operations | Assess packet loss, DNS, BGP, and regional paths. | Routing changes require independent human authorization. |
| Platform Security | Assess TLS, OCSP, time, signature, entropy, and credential conditions. | The copilot never rotates keys, changes clocks, or weakens validation. |
| Cryptographic Services Owner | Assess synthetic HSM latency, quorum, and protocol compatibility. | HSM failover, restart, firmware, or quorum actions are human-controlled. |
| Knowledge Approver | Review document accuracy, classification, version, and effective status. | Only approved and effective versions are retrieval-eligible. |

The existing synthetic operator and knowledge-approval identifier is
`7b636625-53d1-46f7-92a9-9c8c27a243d1`. It identifies a fictional role account,
not a person or authenticated production identity.

## Incident and evidence model

The corpus supports all 36 scenarios in
`syntheticIncidentGenerator/src/main/resources/scenarios/catalog.json`. The
scenarios remain one incident family even though they exercise common,
uncommon, and rare technical causes.

Runbooks may interpret only persisted, tenant-scoped evidence. Current MCP
evidence is a bounded observation window containing:

- service name;
- observation start and end time;
- aggregate error codes and counts; and
- `AVAILABLE`, `PARTIAL`, `UNAVAILABLE`, `TIMED_OUT`, or malformed outcomes.

Retrieved knowledge describes how to investigate those signals. It is not
proof that a condition occurred. A missing, partial, empty, stale, or
contradictory source must remain visible and reduce confidence.

## Severity and lifecycle language

Documents use the repository's existing synthetic incident severities and
lifecycle states. They do not establish a new severity engine.

- `NEW`: received and not yet under investigation.
- `INVESTIGATING`: evidence collection and knowledge retrieval are in progress.
- `AWAITING_REVIEW`: a schema-valid advisory report is ready for human review.
- `APPROVED` or `REJECTED`: a human recorded the terminal report decision.

The corpus may describe escalation urgency, but it must not state that a model
changed severity, approved a report, contacted an external party, or executed a
technical action.

## Document-control model

Every source and PDF uses the metadata contract in `corpus/inventory.md` and
displays:

- SynTen Inc and `Internal — Synthetic Demo`;
- document title, stable document ID, version, type, owner role, approval
  status, effective date, and incident family;
- page numbering and total page count;
- revision history and related-document references; and
- an explicit synthetic-data notice.

Runbooks are task-oriented diagnostic and escalation guides. Policies state
mandatory governance and authority requirements. The two types may reference
one another but must not duplicate whole sections merely to increase corpus
size.

## Naming and content rules

- Use opaque synthetic identifiers instead of realistic account, card,
  merchant, certificate, or endpoint values.
- Use UTC timestamps in examples and label every example `Synthetic example`.
- Use exact repository error codes in monospace when the code itself is the
  retrieval signal.
- Describe observed evidence separately from hypotheses, probable causes, and
  recommended human next steps.
- Procedures may name a controlled console or owner role but may not contain
  live credentials, private endpoints, executable destructive commands, or
  instructions to bypass security controls.
- Do not cite a real company's policy, regulator-specific obligation, service
  level, outage, or organizational structure as a SynTen Inc fact.

## Explicit non-facts

The profile deliberately does not define real jurisdictions, card schemes,
banks, processors, cloud accounts, domains, IP addresses, employee names,
customer names, contractual limits, or regulatory attestations. Later work must
not fill those gaps by borrowing real data; any additional fact requires an
explicit synthetic design decision recorded under `SynTen Inc/`.
