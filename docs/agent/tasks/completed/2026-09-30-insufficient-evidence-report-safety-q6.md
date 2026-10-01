# Task: Insufficient-evidence report safety (Q6)

Status: Complete — deterministic/full verification and both live regressions passed
Created: 2026-09-30
Owner: Christopher Guzowski

## Goal

Correct generation and validation for the partial/unavailable evidence failures
measured in Q5 while preserving the completed review workflow.

## User story

As an operator, I want degraded evidence to produce an explicit low-confidence
inability to conclude so that cited guidance is not mistaken for observed proof.

## Chosen contract

The owner activated the proposed Q6 remediation with `proceed to implement`.
When the latest evidence status is not AVAILABLE, or its snapshot contains no
observations, generation must require INSUFFICIENT_EVIDENCE, LOW confidence,
null probableCause and recommendation, and at least one evidence gap. Independently
reject model output violating this contract as MALFORMED; never repair or retry
it automatically. Preserve exact latest/applicable evidence bindings, including
earlier observations after a degraded retry. AVAILABLE snapshots with observations
retain the existing proposed-report behavior. Version prompt/schema constraints
explicitly; preserve historical reports and Q5 artifacts.

## In scope

Context-dependent provider schema and prompt, independent application validation,
deterministic regression and HTTP/persistence tests, focused/full verification,
fresh live S111/S211 checks against unchanged corpus/oracle/grader, retained results,
and matching documentation/decision record.

## Out of scope

Token-metadata and grading-CLI fixes, confidence tuning for sufficient evidence,
model changes, retrieval/corpus/oracle/grader changes, automated human decisions,
deployment and authentication.

## Constraints

Synthetic data only; no scenario IDs or oracle dependencies in runtime. One model
call per attempt. Preserve missing evidence and immutable provenance. Automated
tests require no live model. No general semantic-entailment claim.

## Acceptance criteria

- [x] Provider schema/prompt require the insufficient-evidence contract for partial,
      unavailable and empty observations; sufficient AVAILABLE evidence is unchanged.
- [x] Independent parsing rejects unsupported PROPOSED responses, non-LOW confidence,
      non-null conclusions and missing gap descriptions for degraded evidence.
- [x] HTTP/persistence tests prove compliant degraded reports reach AWAITING_REVIEW,
      and invalid reports remain MALFORMED without a lifecycle transition or retry.
- [x] Exact source bindings and historical applicable observations remain preserved.
- [x] Focused report tests and authoritative ./verify.ps1 pass.
- [x] Fresh live S111/S211 attempts retain model/prompt/schema/source metadata and
      demonstrate insufficient-evidence/LOW/null outcomes with fixed evaluation inputs.

## Test plan

ReportPromptAndParserTest: parameterized degraded-status/empty-input schema and
parser regression tests, acceptance of compliant reports, rejection of each invalid
field, citation integrity and sufficient-evidence compatibility. ReportApiPostgresIntegrationTest:
degraded compliant report persistence and invalid model-output fail-closed history.
Run focused Maven report tests, then ./verify.ps1. Live manual HTTP/history checks
for S111/S211 use the dedicated corpus-v2 evaluation database and retain inputs,
output, hashes and exact citations. The fixed Q3 grader requires 36 results; any
comparison using 34 retained Q5 results plus two fresh results must be explicitly
labeled a mixed-run diagnostic rather than a new 36-scenario live run.

## Progress notes

- Preserved completed Q5 task before replacing current.md. Existing Q5 documentation
  and retained artifacts are pre-existing user-owned changes.
- Inspected report validation: conditional LOW/null rules currently depend only on
  model-selected disposition and therefore accept PROPOSED for degraded evidence.

## Completion evidence

- Red: ReportPromptAndParserTest failed seven checks before production changes:
  missing context constraints and acceptance of gap-free insufficient reports.
  A separate PROPOSED parser regression failed for the intended reason. HTTP tests
  reproduced AVAILABLE output for missing gaps. An initially invalid UNAVAILABLE
  test fixture was corrected to remove content; rerun confirmed both HTTP regressions
  failed as expected before implementation.
- Green: .\mvnw.cmd -pl backend/copilot-api spotless:apply
  '-Dtest=ReportPromptAndParserTest,ReportApiPostgresIntegrationTest,ReportGenerationServiceTest,ReportDocumentValidatorTest'
  test passed 29 tests. Broader focused command .\mvnw.cmd -pl backend/copilot-api
  spotless:apply '-Dtest=Report*Test,SpringAiReportModelTest' test passed 48 tests,
  zero failures/errors/skips, including earlier-observation preservation.
- Fresh S111/S211 checks passed LOW/null/gap contracts on the first attempts, with
  exact tenant-scoped persisted bindings, ten audit events and zero decisions.
  Fixed grader diagnostic passed both insufficient-evidence checks. All 34 reused
  Q5 result objects are unchanged and regrade is byte-identical. See
  [retained Q6 results](../../../SynTen%20Inc/evaluation/q6-live-results.md).
- First ./verify.ps1 attempt passed backend but failed generator clean because the
  temporary generator held its target jar open on Windows. Restarted only the two
  task-owned Java processes from temporary jar copies. The second attempt passed
  backend and generator but npm ci failed with EPERM because the existing project
  Angular dev server held esbuild.exe. Identified and temporarily stopped that server.
  The third ./verify.ps1 run passed on 2026-09-30: 317 API, 9 MCP, 31 generator,
  and 91 Angular tests, zero failures/errors/skips; formatting, production builds,
  Compose validation, verification-system tests and repository diff checks passed.
- All 60 corpus source/PDF hashes and all three Q5 artifact hashes were checked.
  git diff --check passes. No corpus, oracle, retrieval, evaluator, frontend or
  dependency-lock bytes changed. No generated build output is in the diff.
- Final ./verify.ps1 -Scope Repository passed after completion documentation.
  Temporary evaluation API/generator stopped. Restored the existing Angular dev
  server at 127.0.0.1:4200 and verified HTTP 200; its sandboxed launch required an
  escalated retry because esbuild spawn was rejected with EPERM.

## Remaining limitations

General natural-language entailment remains outside deterministic validation.
Token-metadata and PowerShell grading-CLI defects remain separate follow-ups.
All partial evidence is handled conservatively. Only S111/S211 were rerun live;
the mixed diagnostic does not establish v5 quality across all 36 scenarios.
The unchanged frontend lockfile's npm ci reported seven dependency advisories
(four moderate, three high); npm audit is not a failing gate step.

## Decisions needed

None; conservative degraded-evidence handling implements the owner-activated scope.
