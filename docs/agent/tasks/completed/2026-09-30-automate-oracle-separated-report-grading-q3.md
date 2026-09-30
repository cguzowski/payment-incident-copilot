# Task: Automate oracle-separated report grading (Q3)

Status: Complete — evaluator, retained fixture, and full repository verification passed
Created: 2026-09-30
Owner: Christopher Guzowski

## Goal

Create a versioned, reproducible offline evaluator that grades a complete
synthetic report run against the sealed scenario oracle and retains
correctness, citation, unsupported-claim, latency, and terminal-failure metrics
without exposing oracle content to report generation.

## User story

As an evaluator, I want every synthetic scenario report graded by one
inspectable contract after generation, so I can compare model quality and
failure behavior without relying on anecdotal S001 review or leaking the answer
key into model inputs.

## Context

Q1 separated observable scenario inputs from `scenario-oracle/v1`, and Q2 made
the fixed retrieval benchmark pass. Report generation already persists its
schema, prompt, model, source references, timing, and terminal status, but the
repository has no automated post-run report-quality evaluator. Q3 adds that
evaluation boundary. Q4 remains the explicit human-decision audit proof, and Q5
remains broader live-model execution.

## Chosen contract

- Add `synten-report-eval/v1` as an offline repository evaluation contract. It
  consumes a completed run artifact plus the observable catalog and sealed
  `scenario-oracle/v1`; it is not a runtime dependency of the generator,
  copilot API, report prompt, retrieval path, or operator console.
- A gradeable run contains exactly one result for each of the 36 reviewed
  scenarios. Duplicate, missing, unknown, version-mismatched, malformed, or
  internally inconsistent inputs fail closed and produce no publishable grade.
- Each result retains scenario code, attempt/status identity, model/prompt/schema
  metadata, exact request/completion timestamps, report content when available,
  and the exact evidence and knowledge identifiers eligible for citation.
- Grade correctness as explicit, separately reported oracle-aligned checks:
  terminal availability, disposition, confidence, required observable signal
  coverage, and the required absence of a cause/recommendation for
  insufficient-evidence scenarios. Do not collapse these into a claim of
  semantic truth.
- Grade citation integrity independently: every referenced identifier must be
  in the retained eligible source sets; claim citation coverage and the
  recommendation's approved-knowledge citation are reported separately.
- Grade reproducible unsupported-claim indicators: unknown evidence identifiers,
  unknown knowledge identifiers, machine-signal tokens not present in the
  scenario's observable evidence, and any cause or recommendation asserted for
  an oracle insufficient-evidence scenario. This is a bounded detector, not a
  general natural-language entailment claim.
- Record per-scenario details plus aggregate counts/rates, terminal-status
  distribution, and latency count/min/median/p95/max. Preserve the evaluator,
  observable catalog, oracle, and input hashes in an atomic non-overwriting
  artifact.
- The contract records measurements and has no Q3 quality pass threshold. A
  later owner decision may set a promotion threshold after measured live runs.

## In scope

- A documented report-evaluation input and output schema/version.
- Deterministic offline grading for all 36 oracle scenarios.
- Fail-closed input/version/coverage validation and atomic non-overwriting
  artifact publication.
- Named deterministic tests for every metric and important invalid input.
- Repository verification integration, architecture/ADR/status documentation,
  and a retained inspectable fixture evaluation artifact with SHA-256.

## Out of scope

- Generating a new live report for every scenario or changing report prompts,
  schemas, models, retrieval, corpus bytes, or oracle answers.
- Using an LLM-as-judge, embeddings, fuzzy semantic similarity, or network calls
  in the grader or its automated tests.
- Setting a report-quality promotion threshold from unmeasured assumptions.
- Human-decision audit proof (Q4), broad live-model execution (Q5), UI changes,
  authentication, or deployment.

## Constraints

- Preserve the oracle separation and dependency boundary in ADR-0014. Oracle
  data is available only to the offline post-run grader and terminal reveal.
- Use synthetic data only and never include secrets, vectors, connection
  strings, unrestricted model payloads, or real payment data in artifacts.
- Retain facts and bounded indicators separately from inferred quality; do not
  label lexical signal coverage as semantic entailment.
- Automated verification must be deterministic and require no live model,
  database, or network provider.
- Follow red-green-refactor for every executable behavior.

## Acceptance criteria

- [x] A versioned input contract accepts exactly one result for each of the 36
      reviewed scenarios and rejects incomplete, duplicate, unknown,
      version-mismatched, malformed, or inconsistent runs before grading.
- [x] Per-scenario grades and aggregates reproducibly report correctness,
      citation integrity, bounded unsupported-claim indicators, latency, and
      every terminal failure status without overstating semantic entailment.
- [x] Oracle, observable-catalog, evaluator, and input hashes plus complete
      model/prompt/schema/run metadata are retained in an atomic,
      non-overwriting artifact.
- [x] Dependency/static tests prove report generation and runtime evidence,
      retrieval, and prompts cannot load oracle or grading content.
- [x] A complete 36-scenario synthetic fixture run is graded and retained with
      a recorded SHA-256, demonstrating every metric and failure category.
- [x] Focused evaluator tests and `./verify.ps1` pass with zero skipped tests;
      unavailable checks are reported with exact commands and remaining risk.

## Test plan

- `gradesCompleteRunAcrossAllOracleScenarios` -> deterministic evaluator test
  with all 36 scenarios and independently asserted aggregates.
- `rejectsIncompleteDuplicateUnknownAndVersionMismatchedRuns` -> fail-closed
  contract tests.
- `reportsCorrectnessCitationUnsupportedLatencyAndFailuresSeparately` -> metric
  tests covering available, insufficient, malformed, unavailable, timed-out,
  interrupted/missing, bad-citation, and foreign-signal cases.
- `writesAtomicHashedArtifactAndRefusesOverwrite` -> filesystem contract test.
- `keepsOracleAndGraderOutOfRuntimeInputs` -> static dependency/content test.
- Run the focused evaluator test script, Repository scope, then the authoritative
  unscoped `./verify.ps1` gate.

## Expected approach

1. Add the evaluator tests and confirm the intended missing-contract failures.
2. Implement input validation and per-scenario grading one metric group at a
   time, keeping tests green between behaviors.
3. Add deterministic fixture generation and atomic artifact publication.
4. Produce and hash the complete fixture artifact, then review it for prohibited
   fields and internal consistency.
5. Update ADR-0014 or a focused new ADR, architecture, status, roadmap, and this
   task with factual behavior and verification evidence.
6. Run focused and full repository verification.

## Likely files or components

- `scripts/evaluation/SynTenReportEvaluationV1.psm1`
- `scripts/evaluation/SynTenReportEvaluationV1.Tests.ps1`
- `scripts/evaluation/run-synten-report-evaluation-v1.ps1`
- `SynTen Inc/evaluation/report-evaluation-v1.md`
- `SynTen Inc/evaluation/results/`
- `scripts/verification/Verification.psm1`
- `docs/agent/decisions/`
- `docs/agent/ARCHITECTURE.md`

## Validation commands

```powershell
./scripts/evaluation/SynTenReportEvaluationV1.Tests.ps1
./verify.ps1 -Scope Repository
./verify.ps1
```

## Decisions needed

None. The first live measurements may justify a later owner decision about
quality thresholds; Q3 intentionally does not invent one.

## Progress notes

- 2026-09-30: Owner activated the next ordered roadmap outcome after Q2. Q2 was
  complete, its full gate had passed, and the working tree was clean.
- 2026-09-30: Chose a deterministic offline grader with explicit bounded
  indicators. LLM judging and fuzzy semantic scores are excluded because they
  would make the first evaluation contract less reproducible and harder to
  audit.
- 2026-09-30: Confirmed the first red run failed because the report evaluator
  module was absent. Subsequent red tests covered the missing deterministic
  fixture builder, runner, and repository-verification hook before each was
  implemented.
- 2026-09-30: The focused evaluator suite passes eight named tests. A retained
  36-scenario fixture artifact exercises 33 AVAILABLE, one UNAVAILABLE, one
  TIMED_OUT, and one MALFORMED result plus five bounded unsupported-claim
  indicators.
- 2026-09-30: Repository scope and the authoritative full `./verify.ps1` gate
  passed. Q3 is complete; no live-model quality claim or promotion threshold
  was added.

## Completion evidence

- Red-phase evidence: the initial focused run failed because
  `SynTenReportEvaluationV1.psm1` was missing. Separate subsequent red runs
  failed on the absent deterministic fixture builder, offline runner, and
  repository-verification hook before each implementation.
- Green-phase evidence: `./scripts/evaluation/SynTenReportEvaluationV1.Tests.ps1`
  passed eight tests covering exact 36-scenario grading, incomplete/duplicate/
  unknown/version/hash/inconsistency rejection, every metric group, atomic
  non-overwriting output, deterministic fixture construction, offline runner
  boundaries, verification integration, and runtime oracle isolation.
- Acceptance-criteria coverage: the retained fixture grades all 36 scenarios;
  contains 33 AVAILABLE, one UNAVAILABLE, one TIMED_OUT, and one MALFORMED
  result; reports 32 exact disposition matches, 33 confidence matches, 33
  required-signal coverage results, 286 of 288 valid references, five bounded
  unsupported-claim indicators, and complete latency statistics.
- Full verification: `./verify.ps1` passed on 2026-09-30 with 303 copilot API,
  9 operations MCP, 31 generator, and 91 Angular tests, all with zero failures,
  errors, or skips. Formatting, backend/frontend builds, locked frontend
  installation, Compose validation, repository scripts, and diff checks passed.
  `./verify.ps1 -Scope Repository` also passed independently.
- Manual verification: artifact, evaluator, observable catalog, and oracle
  SHA-256 values were recalculated and matched retained provenance. Static
  inspection found no secret, connection-string, vector, embedding, or API-key
  fields in the artifact.
- Documentation updated: ADR-0016, ADR-0014, architecture, status, roadmap,
  SynTen README, and the report-evaluation contract describe the implemented
  boundary and limitations.
- Retained artifact:
  `SynTen Inc/evaluation/results/q3-report-grader-fixture-v1.json`, 122,566
  bytes, SHA-256
  `a023fa81eac34732a619cd76850db936dac2f48358fa55afa9263a1846e93555`.

## Remaining limitations

- The bounded unsupported-claim detector cannot establish general
  natural-language entailment. It reports only the exact indicators named in
  the contract.
- Q3 creates and proves the grader with synthetic fixtures; Q5 remains
  responsible for broad live-model execution.
