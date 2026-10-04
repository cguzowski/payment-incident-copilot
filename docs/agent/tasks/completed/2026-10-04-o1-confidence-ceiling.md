# Task O1: Calibrate confidence from evidence strength

Status: Complete
Created: 2026-10-04
Owner: Christopher Guzowski

## Goal

Prevent unsupported HIGH confidence in reports from the current aggregate-only
operational evidence and explain the confidence limitations.

## User story

As an analyst, I want confidence to reflect corroboration and missing evidence
so that repeated errors do not masquerade as a confirmed cause.

## Context

Owner activated O1 from next.md. The five-case diagnostic found three HIGH
reports where only one aggregate source was available. Preserve the unfinished
cleanup exactly under 2026-10-04-deferred-demo-cleanup.md; it is not
complete and its verification limitations still apply.

## Chosen contract

Current ReportEvidenceSnapshot carries a single aggregate operational source.
Error categories, counts, applicable/latest snapshots of that source, alerts
and approved knowledge do not constitute independent causal corroboration.
New generation permits LOW or MEDIUM for AVAILABLE nonempty evidence; MEDIUM
requires a bounded supported probable cause/advisory next step with unresolved
confirmation acknowledged. LOW remains available for weak/ambiguous support.
Degraded/empty evidence retains INSUFFICIENT_EVIDENCE, LOW, null cause and
recommendation and explicit gaps. HIGH requires independent claim-specific
operational corroboration, which the present input contract cannot represent.
Constrain provider output and independently reject HIGH as MALFORMED with one
model call, no repair, no retry and no lifecycle transition. Do not rewrite
historical reports or remove HIGH from the historical base report-v1 schema.
Version the generation prompt and retain exact prompt/schema hashes.

## In scope

Confidence instructions, constrained schema, independent parser validation,
regression/persistence tests, version/provenance, ADR and fresh live comparison.

## Out of scope

O2/O3/O4, retrieval or corpus changes, new evidence providers, model replacement,
context/timeout changes, comparison or judge changes, historical mutation,
automatic approval, commits/pushes and cleanup completion claims.

## Constraints

No oracle/scenario-dependent behavior or blanket MEDIUM assignment. Preserve
LOW/null safeguards, tenant/citation boundaries, human review and audit history.
Use deterministic automated tests and the unchanged post-decision rubric for
separate live evaluation. Retain failures and baseline artifacts.

## Acceptance criteria

- [x] Aggregate evidence plus guidance cannot generate or validate HIGH; LOW and MEDIUM remain available for nonempty AVAILABLE evidence.
- [x] Prompt rationale instructions distinguish support from missing independent confirmation and do not count error categories/history/guidance as corroboration.
- [x] Partial/unavailable/empty evidence still requires LOW/null/gaps and independently rejects violating output.
- [x] Invalid HIGH output persists one MALFORMED attempt with no report/repair/retry or incident transition; historical HIGH remains readable.
- [x] Fresh live comparisons retain confidence/rationale, all metrics, failures and provenance for diagnostic cases and additional cases using the unchanged rubric.
- [x] Focused and full verification pass; documentation reflects evidence and limits.

## Test plan

ReportPromptAndParserTest.singleAggregateSourceDoesNotEstablishCorroboratedCause:
provider schema allows only LOW/MEDIUM and parser rejects HIGH even with multiple
observations, distinct historical IDs and multiple knowledge chunks.
ReportPromptAndParserTest.confidenceRationalePreservesMissingConfirmation:
versioned prompt defines MEDIUM/LOW and requires explicit limitations.
Existing parameterized degraded-context tests retain all LOW/null checks.
ReportApiPostgresIntegrationTest.rejectsHighConfidenceWithoutRepairOrTransition:
one-call MALFORMED history, provenance and unchanged INVESTIGATING state.
Existing persistence/history tests plus historical HIGH readback prove retention.
Fresh live evaluations require actual model calls and therefore are manual
verification separate from deterministic tests. Keep frozen baseline, use fresh
identities and reveal only after diagnostic final rejections.

## Verification commands

./mvnw.cmd -pl backend/copilot-api -Dtest=ReportPromptAndParserTest test
./mvnw.cmd -pl backend/copilot-api -Dtest=ReportApiPostgresIntegrationTest test
./verify.ps1

## Progress notes

- 2026-10-04: Resumed owner activation; no implementation occurred before the
  interruption. Preserved incomplete cleanup byte-for-byte in a deferred task record.
- Plan: reproduce confidence failures; implement provider/parser safeguards and
  prompt v6; run focused/full checks; evaluate fresh live cases; review scope.
- Risk: fail-closed validation can increase MALFORMED outcomes; live evaluation
  must retain these failures. Confidence prose remains model-generated and
  bounded validation does not prove semantic correctness.

## Completion evidence

- Red: tmp/o1-red-final.log records three intended unit failures: HIGH schema
  eligibility, accepted HIGH parsing and missing corroboration instructions.
  tmp/o1-http-red.log records expected MALFORMED versus actual AVAILABLE for a
  valid cited HIGH report (one failing test, no errors/skips).
- Green: tmp/o1-focused-green.log records 24 passing prompt/parser and HTTP
  persistence tests, zero failures/errors/skips, including historical HIGH.
- Full gate: ./verify.ps1 passed in tmp/o1-full-verify-final.log. The first gate
  failed Java formatting after passing API tests; spotless:apply corrected the
  changed files and the full gate passed on rerun.
- Live evaluation: seven fresh first attempts against a separate database copy
  completed with three AVAILABLE and four TIMED_OUT results, no retries.
  Available reports completed final rejection/reveal/unchanged comparison:
  S005 88, S313 100, S002 64; confidence matches 2/3. Exact snapshots, 24/24
  citation references, immutable compared histories and 15 protected files
  passed tmp/o1-confidence-2026-10-04/verify-live.ps1. Full results and support
  defects: [O1 evaluation](../../../../SynTen%20Inc/evaluation/2026-10-04-o1-confidence.md).
- Final ./verify.ps1 -Scope Repository passed in tmp/o1-repository-final.log;
  git diff --check passed and protected runtime/evaluation/corpus/schema paths
  have no diff. The deferred cleanup matches its original Git blob exactly.
  Temporary evaluation Java services were stopped after verified process-path
  ownership; the database copy and local evidence remain retained.

## Remaining limitations

Only aggregate evidence is currently available. S002's unchanged key expects
HIGH, which the current ceiling cannot match. The three original HIGH/MEDIUM
misses timed out; no overall answer-key improvement or general calibration is
established. Cause/support defects persist even with valid citations and correct
confidence labels. O2/O3/O4 remain queued. The main demo runtime is unchanged;
restart with the new build to adopt v6. The deferred cleanup remains unclosed.

## Decisions needed

None for this task; evidence contract defines the corroboration boundary.
Future HIGH support requires an explicit richer operational-evidence contract.

