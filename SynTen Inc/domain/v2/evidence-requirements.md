# Evidence requirements and source authority

Version: `synten-payment-domain/v2`
Scope: [lifecycle](lifecycle.md), [risk inventory](risk-inventory.md).

This defines required evidence semantics for E2 authoring and E4 source design,
not a selected MCP tool/schema. Only E01 is available through current MCP.
Proposed item-level evidence is unavailable in the current product. Guidance must
state that limitation instead of asking operators to use nonexistent UI controls.

## Source inventory

| ID | Source / fictional owner | Can establish | Cannot establish | Availability |
|---|---|---|---|---|
| E01 | Aggregate service errors / stage service owner | Reported code/count within returned service/window | Individual outcome, amount, card condition, causal mechanism or measured alert threshold | Current MCP v1 |
| E02 | Intent/request journal / Lifecycle Owner | Submission, intent linkage, operation/key and canonical payload identity | Downstream acceptance/completion | Future read-only source |
| E03 | Correlated issuer/gateway responses / Authorization Owner | Explicit authorization outcome/reason and downstream operation identity | Settlement, refund or current held balance | Future read-only source |
| E04 | Capture journal and acknowledgements / Capture Owner | Linked requests and explicit capture confirmation/failure | Settlement receipt or refund completion | Future read-only source |
| E05 | Hold/release confirmations / Gateway Owner | Remaining hold, correlated reversal/expiry result | Refund of captured units | Future read-only source |
| E06 | Refund journal and acknowledgements / Refund Owner | Linked amount and explicit refund result | Recipient-visible balance without additional source | Future read-only source |
| E07 | Batch membership and receipts / Settlement Owner | Closed membership/cutoff and downstream batch outcome | Unlisted operations or real bank movement | Future read-only source |
| E08 | Delivery journal and recipient acknowledgements / Delivery Owner | Attempts, retry budget consumption, explicit acknowledgement | Payment outcome or recipient projection correctness | Future read-only source |
| E09 | Reconciliation input snapshots / Reconciliation Owner | Source identities, membership, completeness, cutoff and comparison | Authoritative correction or unexplained missing records | Future read-only source |
| E10 | Effective configuration/change journal / Platform Owner | Applicable deadlines, routing, version and change timing | Proof that a change caused an observed failure | Future read-only source |
| E11 | Dependency health/clock/access observations / Platform or Security Owner | Bounded observed connectivity, time offset or access failures | Payment failure or permission to bypass security | Future read-only source |

Application alert, investigation and audit records already establish intake and
operator workflow facts. They cannot replace E02-E11 for payment-domain claims.
Approved knowledge provides rules and diagnostic methods, never occurrence proof.
Oracle answers are offline evaluation material and must not enter these sources.

## Minimum provenance and source coverage

Every future result must identify tenant, source, source contract/version,
collection timestamp, requested window and returned coverage. Individual records
need opaque source record/event ID, relevant intent/request/operation linkage,
source event time, received time and source sequence/version where available.
Retain operation type, explicit outcome/reason, amount unit and relevant effective
configuration identity when a conclusion depends on them. Optional unavailable
fields remain explicitly unknown; do not manufacture identifiers or times.

Snapshots must distinguish complete query coverage from pagination, truncation,
retention loss, clock uncertainty or unsupported filters. A complete empty result
means no matching records in that particular covered scope. It does not prove
that an operation never occurred elsewhere. Record attempted scopes and failure
status so a later retry cannot erase an earlier unavailable result.

Preserve immutable source/snapshot identities sufficient for exact report citations.
Claims involving two sources must cite both; retain the conflict rather than
letting an advisory model reconcile it silently. Never expose credentials, real
account/card values or private endpoints. Tenant mismatch cannot return another
tenant's records; future contracts must test isolation and safe failures.

## Authority and uncertainty rules

Authority is claim-specific. E02 establishes local submission; E03 establishes
explicit authorization response; E04/E06/E07 establish their own downstream stage
only when correlated confirmations exist. Journal status without acknowledgement
is local state. Neither the newest timestamp nor a majority of replicas replaces
the responsible source. Contradictory authoritative records remain contradictory.

| Condition | Required interpretation | Analyst direction |
|---|---|---|
| AVAILABLE with complete records | Facts bounded to returned scope and source authority | Check identity, freshness and competing sources before inference |
| AVAILABLE but empty | No records in stated covered query only | Check scope/linkage/retention; never infer terminal failure |
| PARTIAL/truncated | Missing portion prevents completeness claims | Identify missing interval/page/source; preserve Q6 LOW/null behavior |
| UNAVAILABLE/TIMED_OUT/malformed | No usable facts from that attempt | Retain outcome and request source recovery through its owner |
| Stale snapshot | Historical state; current outcome unknown | Compare against effective deadline and obtain current authoritative snapshot |
| Contradictory records | Conflict with both versions/citations | Check identity, versions, source roles and clock uncertainty; leave unresolved |
| Missing timing/amount configuration | Cannot evaluate overdue/headroom conclusively | Request E10 or exact linked amount records |

Freshness is measured against a source-specific effective rule; v2 invents no
default expiry. Unknown outcome is a lifecycle concept, not a transport status.
An available source may explicitly report UNKNOWN. Several AVAILABLE aggregate
error results still do not establish item-level payment conclusions.

## Evidence requests and later verification

For an unresolved claim, name the source ID and owner, exact tenant/operation
scope, interval/cutoff, required distinguishing field and alternative conclusions
it could support. For example, capture acknowledgement distinguishes submission
failure from a confirmed capture with a lost local response; this is a generic
source comparison, not a scenario fixture or expected answer.

E4 must test complete/empty/partial/unavailable/timed-out/malformed sources,
wrong-tenant and foreign-operation linkage, duplicates, out-of-order events,
stale/configuration-missing snapshots, contradictory confirmations and immutable
retry history. Define MCP schemas and persistence contracts then; E1 selects no
new tool name or report-schema exception.
