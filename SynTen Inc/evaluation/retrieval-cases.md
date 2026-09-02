# SynTen Inc retrieval evaluation cases

Status: Approved evaluation design
Evaluation version: `synten-retrieval-eval/v1`
Corpus version: `synten-auth-knowledge/v1`

## Purpose

These human-labeled cases evaluate PDF-derived chunking, live
`nomic-embed-text` embeddings, PostgreSQL full-text search, pgvector similarity,
RRF fusion, and final context selection without treating model output as ground
truth. The labels come from the synthetic scenario catalog and the approved
corpus inventory.

K3 may use deterministic embeddings to test pipeline behavior. K4 and K5 run
these same labels through the configured live embedding model. K5 also proves
the operator-visible approved-knowledge action; live chat/report evaluation is
deferred. A plausible future report cannot compensate for failed retrieval
eligibility or missing required sources.

## Fixed eligibility rules

Every case applies these filters before ranking:

- tenant ID equals `8b860d80-d17f-4e6b-8c48-af35f26a4d61`;
- incident family equals `AUTHORIZATION_DECLINE_RATE_SPIKE`;
- approval status equals `APPROVED`;
- the version is effective at evaluation time; and
- document type remains available to the runbook/policy context allocation.

RB-022, PL-007, and PL-008 are globally ineligible because they are
`SUPERSEDED`. They must never appear as candidates or selected context, even
when a query exactly repeats their title or distinctive legacy terminology.

## Interpretation of labels

- **Primary runbook**: at least one chunk from this document must enter the
  selected context for a passing case.
- **Supporting policy**: at least one chunk from the named policy must enter the
  selected policy context unless the case explicitly says `none`.
- **Weak approved match**: overlapping content may be a candidate, but its best
  fused rank must not beat the primary runbook's best fused rank.
- **Expected report posture**: a future report-review label, not part of the
  K4 or K5 retrieval pass/fail calculation.

Exact evaluation queries must be produced by the application's versioned query
builder from the referenced synthetic scenario and persisted evidence. The
signal text below is the human-readable oracle and must appear in or be
semantically represented by that derived query; evaluators must not hand-tune a
different query just to improve ranking.

## Labeled cases

| Case | Scenarios | Query signals | Primary runbook | Supporting policy | Weak approved match | Expected report posture |
|---|---|---|---|---|---|---|
| KQ-001 | S001, S002 | `GATEWAY_TIMEOUT`, `UPSTREAM_CONNECTION_RESET`, intermittent gateway connectivity | RB-002 | PL-006 | RB-006 | Proposed cause only when the bounded error cluster overlaps the alert window. |
| KQ-002 | S003, S009 | `UPSTREAM_RATE_LIMITED` or `GATEWAY_MAINTENANCE_REJECTION`, route capacity, maintenance window | RB-003 | PL-006 | RB-002 | Distinguish rate limiting from maintenance and retain the exact route/window limitation. |
| KQ-003 | S004, S108 | `DATABASE_CONNECTION_POOL_EXHAUSTED`, `DATABASE_LOCK_TIMEOUT`, `AUTHORIZATION_STATE_LOOKUP_TIMEOUT`, `AUTHORIZATION_STATE_UPDATE_FAILED` | RB-004 | PL-002 | RB-013 | Proposed database cause requires co-occurring state-operation evidence. |
| KQ-004 | S005 | `ISSUER_DO_NOT_HONOR_SURGE`, several issuers, normal request volume | RB-005 | PL-002 | RB-017 | Medium confidence and request issuer/route breakdown before single-issuer attribution. |
| KQ-005 | S006, S210 | `RETRY_BUDGET_EXHAUSTED`, `CIRCUIT_BREAKER_SYNC_STORM`, `UPSTREAM_CALL_REJECTED`, causal ordering | RB-006 | PL-003 | RB-002 | Preserve amplification order and prohibit automatic retry or breaker changes. |
| KQ-006 | S007 | `AUTHORIZATION_CPU_SATURATION`, `REQUEST_DEADLINE_EXCEEDED`, service objective | RB-007 | PL-003 | RB-004 | Proposed saturation cause; scaling or restart remains separately authorized. |
| KQ-007 | S008, S105 | `ROUTE_CONFIGURATION_NOT_FOUND`, `FEATURE_FLAG_ROUTE_MISMATCH`, merchant cohort | RB-008 | PL-002 | RB-018 | Compare approved flag and route history before configuration attribution. |
| KQ-008 | S010, S207 | `MERCHANT_PROFILE_INVALID`, `UNICODE_NORMALIZATION_FAILURE`, `MERCHANT_PROFILE_PARSE_FAILED` | RB-009 | PL-005 | RB-019 | Use opaque cohort references and do not expose merchant profile content. |
| KQ-009 | S011 | `FRAUD_RULE_REJECTION_SURGE`, low-risk test cohort, rule version | RB-010 | PL-003 | RB-005 | Medium confidence until authorized fraud-rule evidence confirms the cause. |
| KQ-010 | S012, S202 | `NETWORK_PACKET_LOSS`, `BGP_ROUTE_UNREACHABLE`, gateway timeout/reset, regional path | RB-011 | PL-006 | RB-002 | Network attribution requires bounded path evidence and independent owner confirmation. |
| KQ-011 | S013 | `GATEWAY_CREDENTIAL_REJECTED`, configured connection, opaque identifier | RB-012 | PL-005 | RB-014 | Escalate without displaying, rotating, or replacing credentials. |
| KQ-012 | S014 | `CACHE_STAMPEDE`, `AUTHORIZATION_STATE_LOOKUP_TIMEOUT`, cache refresh interval | RB-013 | PL-002 | RB-004 | Medium confidence; retain ordering between cache misses and lookup pressure. |
| KQ-013 | S101, S203 | `TLS_CERTIFICATE_EXPIRED`, `TLS_HANDSHAKE_FAILED`, `OCSP_RESPONDER_UNAVAILABLE`, `CERTIFICATE_STATUS_CHECK_FAILED` | RB-014 | PL-006 | RB-012 | Separate certificate expiry from validation-path unavailability; never bypass checks. |
| KQ-014 | S102, S208 | `DNS_RESOLUTION_FAILED`, `DNS_SECURITY_POLICY_BLOCK`, resolver policy path | RB-011 | PL-006 | RB-002 | Distinguish resolver failure from general gateway unreachability and preserve security policy. |
| KQ-015 | S103, S206 | `CLOCK_SKEW_REJECTED`, `REQUEST_SIGNATURE_INVALID`, `SECURE_RANDOM_SOURCE_BLOCKED`, `REQUEST_SIGNATURE_TIMEOUT` | RB-015 | PL-005 | RB-016 | Escalate time/runtime evidence without changing clocks or weakening randomness. |
| KQ-016 | S104, S204, S209 | `HSM_SIGNING_TIMEOUT`, `HSM_QUORUM_LOST`, `HSM_FIRMWARE_PROTOCOL_MISMATCH`, `HSM_SIGNING_FAILED` | RB-016 | PL-005 | RB-015 | Identify the synthetic HSM failure mode without automated failover, restart, or firmware action. |
| KQ-017 | S106, S110 | `GATEWAY_RESPONSE_SCHEMA_MISMATCH`, `MALFORMED_ISSUER_RESPONSE`, `RESPONSE_MAPPING_FAILED` | RB-017 | PL-005 | RB-005 | Distinguish incompatible schema from malformed response; do not expose raw payloads. |
| KQ-018 | S107, S201 | `REGIONAL_FAILOVER_LOOP`, `CONFIG_VERSION_SPLIT_BRAIN`, `ROUTE_DECISION_DIVERGENCE` | RB-018 | PL-003 | RB-008 | Medium confidence and human routing/configuration review; no forced failover. |
| KQ-019 | S109 | `BIN_ROUTE_TABLE_STALE`, `ISSUER_ROUTE_NOT_FOUND`, reference-data refresh | RB-019 | PL-002 | RB-008 | Verify approved reference-data version before route attribution or reload. |
| KQ-020 | S111 | `TOKEN_VAULT_TIMEOUT`, `PARTIAL`, missing observability partition | RB-020 | PL-002 | RB-004 | `INSUFFICIENT_EVIDENCE` unless independent evidence closes the missing partition. |
| KQ-021 | S205 | `CALENDAR_RULE_EVALUATION_FAILED`, `RECURRING_AUTHORIZATION_REJECTED`, date-sensitive cohort | RB-021 | PL-003 | RB-009 | Medium confidence; do not change rules or replay synthetic payments automatically. |
| KQ-022 | S211 | evidence source `UNAVAILABLE`, no error observations | RB-001 | PL-002 | none | `INSUFFICIENT_EVIDENCE`, low confidence, no probable cause or recommendation. |
| KQ-023 | Synthetic exclusion probe | “legacy gateway recovery”, “emergency routing”, “AI incident automation” | RB-002 | PL-004 | RB-006 | Current approved replacements may be selected; RB-022, PL-007, and PL-008 must remain absent. |

## Candidate and selection assertions

For each run, persist and review:

- derived query and query-template version;
- metadata filters and evaluation timestamp;
- lexical candidates with rank;
- vector candidates with cosine similarity and rank;
- RRF candidates with fused rank and tie-break fields;
- selected context position, document key, version, chunk ordinal, and source
  location; and
- retrieval status and any embedding failure or lexical fallback.

The evaluator maps persisted document IDs and versions back to inventory keys.
It does not grade on title text returned by the model.

## K4 pass thresholds

| Measure | Threshold |
|---|---:|
| Ineligible candidate or selected chunk rate | 0% |
| Primary runbook selected for KQ-001 through KQ-022 | 100% |
| Required supporting policy selected for KQ-001 through KQ-022 | At least 90% |
| Primary runbook outranks the listed weak approved match | At least 90% of applicable cases |
| KQ-020 preserves `PARTIAL` evidence semantics | 100% |
| KQ-022 preserves unavailable/no-cause semantics | 100% |
| KQ-023 excludes all three superseded versions | 100% |

If a threshold fails, record the exact query, candidates, ranks, selected
chunks, corpus/index versions, and suspected cause. Do not relabel an expected
source after seeing model output. Any label change requires a reviewed update
to this file and a new evaluation version.

## K5 report review labels

For available scenarios, a valid report must cite persisted evidence for every
observation and inference and cite at least one selected approved-knowledge
chunk for its recommendation. It must preserve qualifications stated in the
case. KQ-020 and KQ-022 are deliberate insufficient-evidence checks. No case
permits the model to approve the report, contact an owner, retry a payment,
change configuration, rotate a credential, bypass a security control, or
execute another operational action.
