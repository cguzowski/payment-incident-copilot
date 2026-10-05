# Task: O4 — Preserve observed facts and source mapping

Status: Complete within approved bounded validation scope; live sufficient-evidence quality unmeasured
Created: 2026-10-04
Owner: Christopher Guzowski

## Goal

Prevent new reports from distorting observed codes/counts and explicitly mapped
source roles while retaining immutable reports and auditable failure outcomes.

## User story

As an analyst, I want exact codes, counts and source roles preserved so that a
cited report cannot quietly distort the underlying evidence.

## Chosen contract

Owner approved bounded validation, rejecting violations as MALFORMED without
rewrites or automatic retries. Keep report-v1's two observations and 300-character
statements. New observations must exactly copy a supplied event tuple in the
form sourceEventId=...; observedAt=...; errorCode=...; count=.... The applicable
attempt owns those tuples; newest degraded status cannot relabel earlier facts.
Reject changed, invented, repeated or wrongly cited tuples. Prompt/schema hints
assist generation; application validation remains authoritative. Empty snapshots
permit no observations; omission of some supplied events is permitted by the
two-observation bound and is not a completeness claim.

Check explicit E## references against the exact cited passages. For capture
acknowledgement requests, enforce an explicit role mapping when cited guidance
supplies one, including E04. Do not derive incident facts from guidance, add
global scenario mappings or claim general semantic entailment. Version the new
prompt and record checker limits in an ADR.

## In scope

Report prompt/schema constraints, parser-bound fact/source validation, deterministic
regressions, documentation, focused/full verification and fresh isolated live
attempts for diagnostic regressions and additional sufficient/degraded cases.
Retain separate manual fact/source review alongside unchanged comparisons.

## Out of scope

Retrieval/corpus changes, new evidence providers, model/deadline changes,
comparison-rule changes, oracle-derived generation, automatic decisions/retries,
historical mutation, commits and pushes. O3 remains unfinished.

## Constraints

Keep tenant isolation, exact snapshot/citation bindings, versioned provenance,
advisory recommendations, explicit human decisions and degraded LOW/null behavior.
Preserve frozen corpus bytes and historical reports. Use deterministic doubles
in automated tests and follow red-green-refactor. No new dependency.

## Acceptance criteria

- [x] Codes/counts described as observations preserve their evidence association;
  a count of eight cannot be accepted/rendered as one.
- [x] Missing counts remain missing; repeated or absent signals are not invented.
- [x] Stage-specific requests use the supported source role, including E04 for
  capture acknowledgements where guidance requires it.
- [x] Schema/citation/failure paths remain auditable and tenant-scoped. Fresh
  live attempts receive separate fact/source checks alongside unchanged scores.

## Test plan

preservesExactObservedCodeAndCount, detectsCountAssignedToWrongErrorCode,
missingObservationIsNotFabricated, repeatedSignalsPreserveTheirBoundedSourceContext
and captureConfirmationRequestUsesSupportedSourceRole cover the O4 criteria.
Add wrong-attempt, missing/uncited guidance, duplicate and paraphrase rejection,
degraded/empty and real HTTP MALFORMED persistence regressions. Run focused report
tests then ./verify.ps1. Fresh isolated first attempts include S005/S301/S302/S303
and additional AVAILABLE/degraded cases; no score-seeking retry. Preserve failures
and historical/corpus hashes. Comparisons require an explicit human terminal
decision; never make AI decisions to unlock scoring.

## Progress notes

- Owner activated O4 and approved bounded validation and MALFORMED rejection.
- Preserved the completed reliability task before replacing current.md.
- Plan: reproduce tuple/source errors, enforce the bounded contract, run focused
  and full gates, then retain fresh live results and manual support checks.
- Risks: strict tuples can increase model rejections; source checks cover explicit
  patterns only. The existing two-minute throughput limitation may prevent live
  comparisons. No broad quality improvement is assumed.

## Completion evidence

- Initial parser regressions execute 14 tests with 13 intended failures against
  the old parser (tmp/o4/red.log). PDF wrapping reproduces one missing mapping
  rejection before the whitespace fix (red-pdf-whitespace.log). Three stage-role
  swaps and literal template-token preservation fail before their fixes
  (red-roles-and-literals.log). An intermediate equality assertion compared
  Jackson integer-node types; the final test compares exact serialized input.
- Final focused report/persistence suite passes 75 tests with zero failures,
  errors or skips (green-focused-final.log). PostgreSQL HTTP tests retain
  MALFORMED, unchanged evidence, tenant isolation, open state and one model call.
- Full ./verify.ps1 passes 401 API / 9 MCP / 82 generator / 103 console tests,
  zero failures/errors/skips, eight nested Node cases, formatting, builds,
  repository-script, Compose and diff checks (full-verification-final.log).
  The first full run failed on npm's esbuild file lock. Only verified workspace
  console processes were temporarily stopped; their original commands were
  restored outside the sandbox after sandbox launches failed with spawn EPERM.
  Both IPv4/IPv6 console listeners return HTTP 200 (console-health.json).
- The reliability archive's Git blob equals the previous current.md blob:
  80dc135dbdc6d305b46478242256415773afcbfa. O3 remains unfinished.
- Seven isolated first attempts finish with six TIMED_OUT at 120 seconds and
  S211 AVAILABLE at 60.350 seconds. S211 preserves LOW/null/empty observations,
  with 2/2 valid references. All seven exact prompt/schema hashes and versioned
  settings match persisted bindings; all 373 protected hashes and all ten
  original O3/reliability histories remain unchanged. Six provider tasks cancel;
  no truncation or abandoned completion is observed. No automatic retry occurs.
- Separate manual review records S211's alert trend stated as fact under an
  unavailable evidence citation and an unrelated capture/refund/settlement gap.
  Summary/gaps are outside this bounded checker. No sufficient-evidence report
  completes, so live count/source-role improvement remains unmeasured.
- [Retained evaluation](../../../SynTen%20Inc/evaluation/2026-10-04-o4-bounded-fact-and-source-validation.md)
  links exact first-attempt results and limits; local evidence is under tmp/o4/.
  The owner explicitly authorized a fixed diagnostic rejection of S211 solely
  to unlock the unchanged post-decision comparison. REJECTED decision
  a4663378-4375-4c1d-9cd3-6513e06fe0e5 precedes the answer-key reveal.
- Comparison dcb62224-7ea7-4361-97cf-d1f273b22fe7 is AVAILABLE under unchanged
  comparison-rubric/v2 and comparison-text-prompt/v1: disposition, confidence,
  cause and recommendation each score 100; report score is 100/GOOD. Null-field
  checks use no judge call. Expected APPROVED versus fixed diagnostic REJECTED
  scores 0/BAD; this is not an analyst-performance benchmark. Exact report,
  evidence, decision and tenant artifact bindings pass comparison-verification.log.
  All seven new report histories, ten original histories and 373 protected hashes
  remain unchanged after comparison. Six failures remain open and unrevealed.

## Remaining limitations

Validation is deliberately bounded to exact
observation tuples, explicit source identifiers and four documented stage-role
patterns; summary, confidence, gaps and general prose entailment are not proven.
Six live timeouts leave sufficient-evidence/partial fact and source-role prose
unmeasured. S211's summary/gap limitations remain explicit; no overall quality
improvement is established. The existing two-minute throughput limitation persists. Main-demo API processes
were not restarted; only the isolated runtime uses this artifact.

## Decisions needed

None. Owner-authorized diagnostic rejection and comparison are complete.
