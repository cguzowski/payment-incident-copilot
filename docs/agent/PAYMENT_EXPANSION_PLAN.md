# Independent knowledge and familiar payment incidents

Status: E1/E2 complete; E3 is next
Last reviewed: 2026-10-01
Owner: Christopher Guzowski

## Outcome and authorization

The owner approved familiar payment problems beyond authorization-decline spikes
and required independent PDF and incident design. The owner activated E1 on
2026-10-01 after the planning refresh. Activate a narrow task contract before each
implementation phase. Preserve all seven completed workflows and historical evidence.

Reports should explain observed effects, supported potential causes, and advisory
recovery or diagnostic steps. Some cases have clear paths; others require several
documents, competing hypotheses or further evidence. Guidance never proves that
a condition occurred. Coverage is bounded by the supported payment domain, not
a promise to solve every possible scenario.

## Baseline

Runtime supports seven incident families under ADR-0018 and aggregate service-error
evidence. Six additional families use versioned Markdown guidance. Their deterministic
workflows are complete; payment/event/refund records and PDF-only retrieval are absent.
The existing corpus contains 30 PDFs (27 approved, 3 superseded), 705 chunks,
and two legacy API Markdown inputs plus twelve additional family guidance inputs.
Retrieval does not enforce PDF-only sources.
Corpus v2 excluded the sealed oracle but consumed observable scenario inputs.
Its labels measure historical regressions, not independent generalization.
Preserve existing source/PDF bytes, hashes, labels, reports and snapshots.

The catalog pins counts and a fingerprint; expansion needs a versioned accepted
manifest, not removal of integrity checks. Degraded-evidence reports currently
require LOW/null conclusions. New diagnostic guidance must not bypass Q6; report
schema or sufficiency changes require an explicit successor decision and tests.

## Target demo problems

These are conceptual categories, not selected API enum values or incident fixtures.

| Category | Familiar symptom | Investigation distinction |
|---|---|---|
| Card rejection | Locked card, insufficient funds, expired card | Explicit issuer reason versus ambiguous rejection or technical failure |
| Uncertain outcome | Timeout, failed checkout, pending payment | Submission versus final result; timeout alone does not establish failure |
| Suspected duplicate | Customer reports two charges | Distinct requests, retries and holds versus completed payments |
| Refund problem | Missing, delayed, failed or partial refund | Original payment, refund events, downstream state, amount and timing |
| Reversal/hold problem | Cancelled purchase still shows a hold | Authorization release versus refund and unavailable downstream confirmation |

All records, account conditions, amounts and identifiers remain synthetic. No real
accounts are inspected and no payment, refund or reversal is executed.

## Ordered implementation phases

| Phase | Deliverable | Exit evidence |
|---|---|---|
| E1 — Complete: domain and risks | [Successor domain](../../SynTen%20Inc/domain/v2/README.md), lifecycle, 24 independent risks and 11 source requirements | Static/editorial review and Repository verification; no incident fixtures |
| E2 — Complete: independent PDF library | [Frozen independent library](../../SynTen%20Inc/payment-knowledge/v1/README.md): 11 runbooks / 5 policies; historical baseline retained separately | 16 PDFs / 48 pages, extraction, all-page editorial/visual QA, byte-identical builds, hashes and full gate |
| E3 — Next: catalog and retrieval | PDF-only eligibility, versioned accepted catalogs, applicable shared/family policies, embeddings and readiness | Test-first isolation, idempotency, missing-PDF behavior, relationship eligibility and historical provenance |
| E4 — Evidence and workflow | Read-only synthetic payment/event/refund evidence, multi-family intake and understandable timeline | Versioned MCP contracts, failure-path/persistence tests, independent builds, visual QA and authorization regressions |
| E5 — Independent incidents | Cases derived from system behavior after corpus freeze | Recorded inputs exclude PDF text, target document identities, mapping labels and oracle answers; observed records separated from sealed truth |
| E6 — Held-out evaluation | Fixed post-run rubric, live runs and review examples | Grounding, relevance, effects/causes, conditional resolution, uncertainty, citations, policy compliance, latency and failures with exact provenance |

E3 is the next implementation objective. E2 is frozen before E5 starts. E3/E4
consume agreed domain contracts, not desired incident answers. Use one active
objective and one writing agent per worktree.

## Independence and useful guidance

Corpus authors consume the domain profile, independent lifecycle risk inventory,
source-system semantics and governance requirements. They must not read incident
catalogs, scenario fixtures, labels, expected answers, report outputs or the oracle.
Existing PDFs remain a scenario-informed baseline and are not relabeled independent.

Choose additions for general usefulness, including risks with no eventual matching
incident: delayed/duplicated/out-of-order events, state reconciliation, retry safety,
dependency failures, stale configuration, recovery, escalation and human authority.
Runbooks vary by topic and specify distinguishing evidence, negative checks,
alternative hypotheses, conditional recovery, validation and rollback. Policies
define durable controls without repeating procedures. Mark unavailable sources.

Freeze the reviewed library before new scenario authoring. Scenario authors use
synthetic system behavior and evidence contracts without reading PDFs or selecting
target documents. Technical retrieval tests may use known sources; label them
separately from independent quality evaluation.

Create straightforward, ambiguous, compound/cascading, misleading-symptom,
incomplete, contradictory and unfamiliar combinations. Do not put solutions in
titles, inject oracle facts as evidence or require every incident to be solvable.
An observed issuer reason may legitimately explain some cases. General guidance
can provide useful next steps without a dedicated runbook.

## Verification and evaluation

For each behavior phase, map criteria to named tests, confirm the intended red
failure, implement minimally, run focused/relevant suites and ./verify.ps1.
Automated providers remain deterministic; record live checks separately.
PDF work additionally requires extraction, editorial review and visual QA.

E3 covers direct/related PDF eligibility, empty/missing catalogs, failed embeddings,
shared-policy relevance, tenant/family/version exclusions, superseded documents,
idempotency and immutable historical Markdown citations. E4 covers missing, delayed,
contradictory and malformed records, timeouts, invalid transitions and audit bindings.

Fix the E6 rubric before live runs. Grade claims against available evidence and
recommendations against applicable guidance, allowing multiple valid sources and
next steps. Distinguish hidden truth from what an operator could establish. The
oracle is used only offline after outputs are recorded. Document bounded grader
limitations and manual review; do not weaken labels or gates to manufacture a pass.

Later corpus revisions must address general operational gaps, receive new versions,
and be evaluated against fresh held-out incidents. Previously seen cases become
regressions. Never overwrite historical benchmark evidence.

## Decisions at phase boundaries

- E1 resolved conceptual states, source authority, synthetic amount units and
  timing/reason semantics in domain/v2. Existing family enum values remain intact;
  new categories are not API values. Source-specific duration values and runtime
  schemas remain E4 decisions; absent timing configuration stays unknown.
- E2 resolved successor membership/metadata under payment-knowledge/v1. Fictional
  approval is a knowledge control; shared stage/family metadata is not yet runtime
  eligibility. Historical scenario-informed PDFs remain a separate baseline.
- E3: Shared-policy applicability, accepted catalog version rules and explicit
  missing-PDF readiness behavior.
- E4: MCP tools/schemas, timeline fields and any report-schema/sufficiency change.
  Preserve Q6 until an explicit successor is approved.
- E6: Grading methods, manual review and any promotion thresholds.

Resolve material choices in narrow phase contracts rather than inventing them
during implementation. AWS, authentication and autonomous remediation remain deferred.
