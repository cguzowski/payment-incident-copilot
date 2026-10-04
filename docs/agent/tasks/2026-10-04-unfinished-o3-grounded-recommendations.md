# Task: Generate specific, grounded recommendations (O3)

Status: Implementation and evaluation verified; grounding acceptance criteria remain unmet
Created: 2026-10-04
Owner: Christopher Guzowski

## Goal

Anchor probable causes and concrete safe advisory next steps to observed
mechanisms and supplied approved guidance.

## User story

As an analyst, I want a bounded explanation and a concrete safe
next step so that I know which owner and records can resolve the incident.

## Chosen contract

Version the generation prompt with concise cause/recommendation instructions.
Keep existing structural validation; do not add a general prose entailment
checker or silently repair model output. Separate observed mechanisms from
unverified deeper hypotheses. Use only supplied guidance for owners, records,
and applicable retry prerequisites; preserve missing guidance and unknown final
outcomes. Existing malformed-output and explicit human-review behavior remains.

## In scope

Versioned report prompt, prompt/provenance regression tests, documentation,
focused/full verification and fresh live comparisons with manual support review.

## Out of scope

Retrieval/corpus changes, O2/O4, new capabilities, automatic retries or decisions,
oracle-derived templates, historical mutation, comparison/evaluator changes,
context-budget changes, commits and pushes.

## Constraints

Preserve frozen corpus, tenant isolation, exact snapshot/citation bindings,
advisory output, versioned provenance and explicit human decisions. Preserve
INSUFFICIENT_EVIDENCE/LOW/null for degraded or empty evidence. Keep the current
comparison-rubric/v2 and comparison-text-prompt/v1 unchanged, including keys,
weights, bands and thresholds. Generation never receives oracle data.

## Acceptance criteria

- [ ] An observed timeout/unavailability signature is distinguished from an
  unverified configuration hypothesis.
- [ ] Available guidance produces a specific advisory owner/record request and
  applicable no-blind-retry safeguards without inventing absent guidance.
- [x] Concision, schema, exact citations, degraded LOW/null and human authority
  remain enforced by existing and focused report tests.
- [x] Fresh live cause/recommendation scores and cited support are retained under
  the unchanged comparison, with manual support review recorded separately.

## Test plan

ReportPromptAndParserTest:
probableCauseSeparatesObservedMechanismFromUnverifiedHypothesis,
recommendationNamesSupportedOwnerRecordsAndSafetyConditions,
missingGuidanceRemainsExplicitRatherThanInvented. Test version/hash and exact
serialized evidence/guidance; retain parser concision/citation/degraded tests.
ReportApiPostgresIntegrationTest verifies new provenance and human-review state.
Run focused prompt/parser and HTTP tests, then ./verify.ps1.
Fresh live attempts cover the five diagnostic regressions plus sufficient and
degraded cases outside that sample, one attempt per case. Retain baseline/new
artifacts, failures, metrics, model/prompt/retrieval provenance and separate
manual cited-support review. No retries to improve scores or new threshold.

## Progress notes

- Owner activated O3. Archived the completed confidence-expectation task.
- Plan: failing instruction/provenance regressions, minimal prompt update,
  focused/full gate, fresh bounded live comparison and manual support review.
- Risks: prompt instructions cannot prove semantic support; existing limited
  context, retrieval misses and model timeouts may prevent live success.
- Owner authorized the four prepared fixed evaluation rejections after reviewing
  the request. They were submitted only in the isolated O3 database; comparisons
  ran after those decisions. No retries, AI approval or recovery action.

## Completion evidence

- Red: tmp/o3/red.log records the three new instruction failures. The initial
  provenance run skipped PostgreSQL tests without Docker; red-docker.log executed
  all 28 cases with 13 intended instruction/version failures and no errors/skips.
- Green: green.log passed all 28 focused prompt/parser and PostgreSQL HTTP tests.
- Full ./verify.ps1 passed 371 API, 9 MCP, 82 generator and 103 console tests,
  zero failures/errors/skips, eight nested Node UI cases, formatting/builds,
  Compose and diff checks (full-verification-final.log). The first run stopped
  on the console's esbuild.exe lock. Console was stopped and restored after the
  successful rerun, with HTTP 200 on port 4200.
- Seven first live attempts returned four AVAILABLE and three TIMED_OUT reports,
  no retries. Both degraded cases preserve LOW/null; all 25 citations match their
  exact snapshots. Manual review finds unsupported configuration prose, generic
  recommendations, guidance copied into observations and truncated sentences.
  The first two acceptance criteria remain unchecked because dependable live
  adherence is not established despite passing instruction regressions.
- [Live review](../../../SynTen%20Inc/evaluation/2026-10-04-o3-grounded-recommendations.md)
  retains failures, timings, separate cited-support review and baseline context.
  All seven template/schema hashes match persisted provenance; 357 baseline/corpus
  and 16 comparison/oracle/evaluator file hashes remain unchanged. Archive Git
  blob equals the previous current.md blob. Artifacts are retained under tmp/o3/.
- Four authorized fixed rejections/reveals/comparisons completed. Report scores
  S005/S313/S002/S211 are 88/100/90/100; cause/recommendation 70/80, 100/100,
  80/80 and 100/100. Disposition and confidence match 4/4. The degraded text scores
  are deterministic null checks; manual support defects remain. Fixed rejection
  scores are 100/0/0/0 and are not a human-review benchmark.
- comparison-verification.log confirms seven unchanged report histories, four
  exact evidence/report/decision bindings, two judge calls without retries,
  three unfinalized failures and all 373 protected hashes. comparison-hashes.log
  verifies all four native input/prompt/response hashes. Summary/artifact hashes
  are retained in comparison-summary.json. Historical scoring inputs are intact.
- ./verify.ps1 -Scope Repository and git diff --check passed after recording
  comparison results (repository-after-comparison.log). No production changes
  were made after the previously passing full gate.

## Remaining limitations

No general semantic entailment checker. Three stage cases timed out; returned
reports do not establish reliable adherence despite GOOD comparison bands.
Context budgeting and retrieval usefulness
are outside this task. Prompt tests establish instructions, not semantic support.

## Decisions needed

None. Use prompt instructions with existing validation for this bounded scope.
