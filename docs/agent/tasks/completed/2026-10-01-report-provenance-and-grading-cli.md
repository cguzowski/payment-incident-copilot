# Task: Correct report token provenance and grading CLI timestamps

Status: Complete - both regressions fixed; full verification passed
Created: 2026-10-01
Owner: Christopher Guzowski

## Goal

Fix the owner-reported output-token provenance mismatch and PowerShell 7.6.5
grading CLI timestamp failure.

## User story

As an operator, I want report provenance to reflect the effective model output
budget and retained reports to grade reproducibly through the documented CLI.

## Chosen contract

New attempts record the existing ADR-0012 limit of 1,536 output tokens, using
the same setting as the adapter. Preserve it through successful and failed
completion and persistence. Historical attempts and evaluation artifacts remain
unchanged. CLI JSON loading preserves timestamp strings without weakening
the evaluator's strict timestamp validation, including on PowerShell 7.6.5.

## In scope

Report generation settings/provenance, CLI input parsing, regression tests,
current documentation and verification evidence.

## Out of scope

Provider budget changes, historical metadata rewrites, live model runs,
grading metrics/schema changes, payment expansion work.

## Constraints

Deterministic provider-free tests; preserve invalid-input rejection, offline
grading and atomic output/no-overwrite behavior. Preserve existing ADR decisions.

## Acceptance criteria

- [x] Started and terminal attempts record 1,536 tokens, matching provider options;
      persistence round-trips the corrected provenance.
- [x] Executing the CLI InputPath under PowerShell 7.6.5 preserves timestamps,
      input hash, latency and grade relative to direct module invocation.
- [x] CLI rejects invalid timestamp input without publishing an artifact.
- [x] Focused tests and full ./verify.ps1 pass; docs distinguish corrected future
      behavior from immutable historical mismatches.

## Test plan

Add assertions to named service success/timeout tests and PostgreSQL report
persistence coverage before changing production. Existing adapter options test
asserts the exact 1,536-token budget. Add executable CLI tests comparing complete
fixture input against the direct module grade, with offset/subsecond timestamps,
and malformed timestamp rejection. Confirm intended red failures, implement,
run focused Java/PowerShell suites, then the full verification gate.

## Progress notes

- Read required repository/service context, architecture and ADR-0012; initial
  working tree clean. Archived the completed E2 task unchanged.
- Regression red: service success and timeout tests expected 1,536 but observed
  4,096. Executed CLI input test failed with createdAt must be non-blank under
  PowerShell 7.6.5 before changing production.
- Shared report generation settings now bind attempt provenance and Ollama
  options. CLI preserves date strings using DateKind where supported.
- Serialized CLI test fixtures now retain empty arrays rather than null, so
  the regression supplies valid report-v1 input through JSON.
- Follow-up screenshot: recovered generator UI 404 after the clean verification
  build by restarting the pre-build development process from the verified JAR.
  UI/static/health endpoints and MCP identity passed live read-only checks;
  API and console remained available. No production code change or new incident.

## Completion evidence

- Focused Java command: ./mvnw.cmd -pl backend/copilot-api
  '-Dtest=ReportGenerationServiceTest,SpringAiReportModelTest,ReportPersistencePostgresIntegrationTest'
  test passed 12 tests, zero failures/errors/skips, with Docker PostgreSQL.
  Success/start/timeout provenance and persisted round-trip assertions pass;
  adapter options independently assert the exact 1,536 budget.
- ./scripts/evaluation/SynTenReportEvaluationV1.Tests.ps1 passed all ten tests
  under PowerShell 7.6.5, including cliInputPreservesTimestampStringsAndMatchesDirectGrade
  and cliRejectsInvalidTimestampWithoutPublishingArtifact. Complete fixture
  CLI/direct grade bytes match for offset and subsecond timestamps. Invalid
  createdAt/requestedAt/completedAt inputs reject before artifact publication.
- Actual CLI regrade of retained q5-live-input-v1.json was byte-identical to
  q5-live-grades-v1.json: SHA-256
  efb290272dfb073e701ef75e495fd91f446ffe188eef4e9b4c56e9196b7a14fb.
  Temporary output removed; historical input, observations and grades unchanged.
- Full ./verify.ps1 passed on 2026-10-01 under PowerShell 7.6.5: 339 API,
  9 MCP, 52 generator and 103 Angular tests, zero failures/errors/skips;
  all script tests, Java/Prettier formatting, production builds, Compose
  validation and diff checks passed. Docker access was required because the
  sandbox denied the initial named-pipe probe; elevated verification succeeded.
- Final scope/diff checks passed. Historical corpus, independent library,
  evaluation artifacts and accepted ADRs have no diff. E2 archive text matches
  the previous current task. No UI change; visual verification is inapplicable.

## Remaining limitations

Historical provenance remains inaccurate and must not be retroactively relabeled.
A running API needs the updated build/restart; no live provider run is required
or claimed. CLI DateKind fallback is structurally retained for older PowerShell;
the executable compatibility tests ran on the reported 7.6.5 version.

## Decisions needed

None; owner explicitly requested both fixes within existing contracts.
