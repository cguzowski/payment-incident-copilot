# Next tasks: Investigation quality optimizations

Status: O1 clarification implemented; O2-O4 queued
Created: 2026-10-01
Owner: Christopher Guzowski

The owner requested these four next tasks after the
[five-case diagnostic](../../../SynTen%20Inc/evaluation/2026-10-01-investigation-diagnostic.md).
The owner activated [O1](completed/2026-10-04-o1-confidence-ceiling.md) on 2026-10-04.
Its confidence ceiling is superseded by the owner's
[attainable-HIGH clarification](completed/2026-10-04-attainable-high-confidence.md). The unfinished cleanup is
preserved under [deferred tasks](2026-10-04-deferred-demo-cleanup.md),
without a completion claim. Activate one task at a time in current.md after
preserving the previous task's actual completion state.

## Shared constraints and verification

Owner-authorized exception on 2026-10-04: [the current task](current.md) calibrates
only post-decision confidence expectations using explicit evidence rules under
ADR-0025/comparison-rubric/v2. Original keys, other metrics and formulas stay intact.
The constraints below retain the original O1-O4 contract and historical context.

- Do not change comparison-rubric/v1, comparison-text-prompt/v1, the offline
  evaluator, answer keys, comparison weights, bands or approval thresholds.
- Do not expose oracle data to investigation, retrieval or generation. Derive
  behavior from observed evidence and approved guidance, not scenario codes or
  expected-answer wording. Preserve frozen corpus bytes and historical reports.
- Keep tenant isolation, exact snapshot/citation bindings, versioned generation
  provenance, advisory recommendations and explicit human decisions.
- Preserve INSUFFICIENT_EVIDENCE/LOW/null behavior for degraded or empty evidence.
- Follow red-green-refactor, with deterministic model doubles for automated
  tests. Run focused suites and ./verify.ps1 before implementation completion.
- Separately run fresh live comparisons with the unchanged rubric, retaining
  baseline and new artifacts, failures, per-metric results and model/prompt/
  retrieval metadata. Include the diagnostic regressions and additional cases
  outside this five-case sample. Do not retry until scores improve or treat valid
  citation IDs as proof of supported prose. Report improvements and regressions;
  do not introduce a new scoring threshold without owner approval.

## O1: Calibrate confidence from evidence strength

Status: Initial implementation complete; ceiling superseded by current.md

**User story:** As an analyst, I want confidence to reflect corroboration and
missing evidence so that repeated errors do not masquerade as a confirmed cause.

**Context:** S301/S302/S303 selected HIGH rather than expected MEDIUM. Multiple
error categories came from one aggregate source; deeper mechanisms were unobserved.

**Scope:** Define evidence-based HIGH/MEDIUM criteria in versioned report
generation, keeping the existing degraded-evidence contract. Document the basis
for confidence and preserve uncertainty. No blanket MEDIUM default or hard-coded
scenario labels.

**Acceptance criteria and test map:**

- [x] One aggregate source plus approved guidance is not treated as independent
  causal corroboration; rationale identifies the limits.
  Proposed tests: singleAggregateSourceDoesNotEstablishCorroboratedCause and
  confidenceRationalePreservesMissingConfirmation, using deterministic contexts.
- [x] Partial, unavailable and empty evidence still require LOW/null/gaps.
  Retain existing degraded-contract regressions and add any missing edge cases.
- [x] Fresh live attempts record confidence and rationale against the unchanged
  key comparison, including sufficient-evidence and degraded cases.

**Likely components:** ReportPromptFactory, report generation prompt, report
validation tests and version/provenance metadata.

**Decision before implementation:** Define precise confidence criteria and
whether enforcement belongs in generation instructions, application validation
or both; define failure behavior without silently rewriting model output.

## O2: Retrieve useful operational passages

Status: Queued

**User story:** As an analyst, I want retrieved excerpts to contain diagnostic
and response guidance so that limited context is not consumed by cover metadata.

**Context:** S005 selected four metadata-only cover excerpts out of seven.
Related-policy boosts and document diversity can displace useful passages.

**Scope:** Evaluate passage usefulness, policy relationship weighting and bounded
selection of diagnostic/response passages from relevant runbooks. Retain useful
purpose text on mixed pages; do not exclude every first page. No corpus editing,
embedding-model replacement or speculative vector-store changes.

**Acceptance criteria and test map:**

- [ ] Metadata-only chunks do not displace eligible operational guidance.
  Proposed tests: operationalPassageOutranksMetadataOnlyCover and
  mixedPurposeAndMetadataPageRetainsUsefulGuidance.
- [ ] Relevant diagnostic and response passages can coexist within the bounded
  context; generic related policies do not overwhelm the incident's guidance.
  Proposed tests: boundedContextIncludesDiagnosticAndResponsePassages and
  genericRelationshipBoostDoesNotDisplaceOperationalGuidance.
- [ ] Tenant/family/approval/effective-time/PDF filters and immutable locators
  remain enforced; focused PostgreSQL regressions and the existing fixed
  retrieval benchmark retain their unchanged labels and thresholds.
- [ ] Live comparisons record selected passage content, context size, latency
  and report metrics, separating retrieval changes from generation changes.

**Likely components:** KnowledgeContextSelector, PostgresKnowledgeSearchRepository,
PostgresRelatedPolicySearch and retrieval unit/integration/evaluation tests.

**Decision before implementation:** Choose usefulness detection and selection/
ranking rules from measured candidates; version consequential ranking changes.
Measure actual prompt tokens before making any context-size change.

## O3: Generate specific, grounded recommendations

Status: Queued

**User story:** As an analyst, I want a bounded explanation and a concrete safe
next step so that I know which owner and records can resolve the incident.

**Context:** Reports drifted toward configuration/health hypotheses without
configuration evidence and omitted stage-specific checks or retry safeguards.

**Scope:** Anchor probable causes to observed failure mechanisms; distinguish
deeper hypotheses. Include the responsible owner, missing records, unknown final
outcome and applicable retry prerequisites when supplied by approved guidance.
No new operational capabilities, automatic retries or oracle-derived templates.

**Acceptance criteria and test map:**

- [ ] An observed timeout/unavailability signature is distinguished from an
  unverified configuration hypothesis.
  Proposed test: probableCauseSeparatesObservedMechanismFromUnverifiedHypothesis.
- [ ] Available guidance produces a specific advisory owner/record request and
  applicable no-blind-retry safeguards without inventing absent guidance.
  Proposed tests: recommendationNamesSupportedOwnerRecordsAndSafetyConditions
  and missingGuidanceRemainsExplicitRatherThanInvented.
- [ ] Concision, schema, exact citations, degraded LOW/null and human authority
  remain enforced by existing and focused report tests.
- [ ] Fresh live cause/recommendation scores and cited support are retained under
  the unchanged comparison, with manual support review recorded separately.

**Likely components:** Versioned report prompt, ReportPromptFactory,
ReportOutputParser and deterministic report generation tests.

**Decision before implementation:** Select the prompt/validation approach and
handling of unsupported hypotheses; do not silently repair persisted reports.

## O4: Preserve observed facts and source mapping

Status: Queued

**User story:** As an analyst, I want exact codes, counts and source roles
preserved so that a cited report cannot quietly distort the underlying evidence.

**Context:** S301 changed eight late responses to one; S005 omitted the exact
code/count; capture advice omitted E04 capture acknowledgement records.

**Scope:** Evaluate structured observation rendering or bounded fact validation
and improved generation instructions. Check claim-specific source roles against
supplied guidance. No general semantic-entailment claim, new evidence provider,
comparison-rule change or mutation of historical reports.

**Acceptance criteria and test map:**

- [ ] Codes/counts described as observations preserve their evidence association;
  a count of eight cannot be accepted/rendered as one.
  Proposed tests: preservesExactObservedCodeAndCount and
  detectsCountAssignedToWrongErrorCode.
- [ ] Missing counts remain missing; repeated or absent signals are not invented.
  Proposed tests: missingObservationIsNotFabricated and
  repeatedSignalsPreserveTheirBoundedSourceContext.
- [ ] Stage-specific requests use the supported source role, including E04 for
  capture acknowledgements where guidance requires it.
  Proposed test: captureConfirmationRequestUsesSupportedSourceRole.
- [ ] Schema/citation/failure paths remain auditable and tenant-scoped. Fresh
  live attempts receive separate fact/source checks alongside unchanged scores.

**Likely components:** ReportEvidenceSnapshot/ReportEvidenceObservation,
report prompt/parser or presentation boundary, and focused report regressions.

**Decision before implementation:** Choose deterministic rendering versus bounded
validation, define supported claim patterns and failure/retry behavior, and
record the limits of any prose checker. Do not silently rewrite model assertions.

## Progress and completion evidence

2026-10-01: Four tasks queued at the owner's request. No implementation or
acceptance verification has begun. The diagnostic baseline and current task
remain unchanged. Each activated task must retain its own red/green, full-gate
and live-evaluation evidence.

2026-10-04: Owner activated O1. Current.md records the aggregate-only confidence
contract and its verification; O2-O4 remain queued and unimplemented.

2026-10-04: O1 implementation and full gate passed. Seven fresh attempts retained
three available comparisons (88/100/64) and four timeouts. Confidence matches 2/3;
S002's HIGH key conflicts with the current ceiling, and the original three
confidence misses timed out. No overall answer-key improvement is established.
See the [live evaluation](../../../SynTen%20Inc/evaluation/2026-10-04-o1-confidence.md).

2026-10-04: Owner clarified that HIGH must remain attainable but exceptional,
MEDIUM should be normal, and weak support should select LOW. Current.md and
ADR-0024 replace the v6 ceiling. Exact comparison remains unchanged; no HIGH-key
normalization was implemented. O2-O4 are still queued.
The v7 correction passed full verification (368 API / 9 MCP / 74 generator /
103 console). Live confidence frequency remains unestablished; the initial weak
case selected MEDIUM and its follow-up under tightened instructions timed out.
