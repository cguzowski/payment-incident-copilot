# Task: Prevent duplicate citations from producing malformed live reports

Status: Complete — focused, live, Backend, and full repository verification passed
Created: 2026-09-30
Owner: Christopher Guzowski

## Goal

Make schema-constrained local report generation reliably produce a valid
`report-v1` document when only one evidence identifier is eligible, without
weakening validation or silently repairing model output.

## User story

As an operator, I want a supported investigation with available evidence and
approved knowledge to produce a reviewable report, so repeated duplicate
citations from the local model do not leave the workflow stuck at MALFORMED.

## Context

Live S013 investigation `005ea21a-d8ee-4558-aafa-78ab4e107029` produced two
MALFORMED attempts. A direct reproduction of the same Qwen request showed the
only eligible evidence UUID repeated twice in `confidence.evidenceIds`.
`report-v1` correctly requires unique reference arrays, but Ollama's native
structured-output grammar did not enforce `uniqueItems`.

## Chosen contract

- Keep `report-v1`, strict schema validation, semantic/citation validation,
  one model call per attempt, and fail-closed MALFORMED handling unchanged.
- Publish `report-prompt/v4` with an explicit instruction that every evidence
  and knowledge reference array contains unique identifiers and never repeats
  an identifier.
- Narrow every evidence-reference array's per-request `maxItems` to the number
  of distinct eligible evidence IDs (bounded by the base schema maximum).
  Apply the equivalent bound to knowledge-reference arrays where knowledge is
  allowed. Empty observation/summary knowledge arrays remain capped at zero.
- Preserve exact constrained-schema hashing so the changed request contract is
  auditable per attempt.

## In scope

- Test-first prompt/version and constrained-schema changes.
- Regression coverage for one-source and multi-source citation bounds.
- Focused backend tests, full repository verification, and a live retry of the
  same S013 investigation after restarting the API.
- Factual task, status, and decision documentation updates.

## Out of scope

- Weakening or removing `uniqueItems`, accepting duplicate references, or
  deduplicating/repairing model output after generation.
- Automatic retries, persisting unrestricted raw model output, changing the
  report schema version, model, retrieval results, or corpus.
- General report-quality remediation beyond this reproduced validity defect.

## Constraints

- Preserve advisory-only reports and mandatory human review.
- Do not expose raw provider payloads or sensitive diagnostics in API/UI output.
- Automated tests remain deterministic and require no live model.
- Follow red-green-refactor.

## Acceptance criteria

- [x] A regression test proves one eligible evidence ID constrains every
      evidence-reference array to at most one item and initially fails.
- [x] Multi-source evidence and knowledge arrays are bounded by their distinct
      eligible identifier counts without exceeding base-schema limits.
- [x] `report-prompt/v4` explicitly prohibits duplicate identifiers while
      strict parser and validator behavior remains unchanged.
- [x] The same live S013 investigation produces an AVAILABLE report after the
      API restart, with unique eligible citations and retained v4/schema hashes.
- [x] Focused backend tests and `./verify.ps1` pass with zero skipped tests.

## Test plan

- `narrowsCitationArrayBoundsToDistinctEligibleSources` -> prompt/schema unit
  regression covering one and two evidence IDs plus selected knowledge IDs.
- Existing parser tests continue rejecting duplicate references.
- Existing model-option and report service tests remain green.
- Run focused report tests, `./verify.ps1 -Scope Backend`, then `./verify.ps1`.
- Restart the local API and retry S013 once; inspect the persisted attempt and
  report citations through the tenant-scoped API.

## Progress notes

- 2026-09-30: Screenshot and live API history confirmed two consecutive
  MALFORMED attempts for S013 despite AVAILABLE evidence and knowledge.
- 2026-09-30: Direct local Qwen reproduction completed normally in 93.8 seconds
  but repeated the sole eligible evidence UUID in `confidence.evidenceIds`.
  This violates `uniqueItems` and explains the safe application rejection.
- 2026-09-30: Added `report-prompt/v4` and per-context reference-array bounds.
  Strict parser/semantic validation, one-call generation, and MALFORMED handling
  remain unchanged; no output repair, deduplication, or retry was introduced.
- 2026-09-30: Restarted the API and retried the same S013 investigation once.
  Attempt `721d1cd4-6adb-4191-adf1-69723370872c` completed AVAILABLE/PROPOSED
  with unique evidence and knowledge citations in 94.9 seconds.

## Completion evidence

- Red phase: `ReportPromptAndParserTest` failed two assertions for the intended
  reasons: the sole eligible evidence ID still allowed `maxItems: 10`, and the
  prompt still reported `report-prompt/v3`.
- Green phase: focused command
  `.\mvnw.cmd -pl backend/copilot-api '-Dtest=Report*Test,SpringAiReportModelTest' test`
  passed 35 tests with zero failures/errors/skips. Coverage includes one and two
  eligible sources, the base maximum of ten, and unchanged duplicate rejection.
- Live verification: attempt `721d1cd4-6adb-4191-adf1-69723370872c` persisted as
  AVAILABLE with `report-prompt/v4`, `report-v1`, evidence
  `a907aaf5-0ba5-49ae-9474-1ce0187a3f36`, and retrieval
  `bfadd708-0c19-4135-980b-01775d5a85a0`. Prompt hash
  `49abd2a7eaf4f185b7099662fab8aae3e2739f96b7ce89369de84367f5b58496`
  and constrained-schema hash
  `cfc0c6661a33fdd7540d48566c1430900f3fc8760a75608634395e425f5c9999`
  are retained on the attempt.
- `.\verify.ps1 -Scope Backend` passed 304 copilot API, 9 operations MCP, and
  31 generator tests with zero failures/errors/skips. The authoritative
  `.\verify.ps1` gate passed the same backend suites plus 91 Angular tests,
  formatting, builds, Compose validation, repository checks, and diff checks.

## Remaining limitations

- Native provider enforcement of JSON Schema keywords varies. Application
  validation remains authoritative and MALFORMED remains a valid safe outcome
  for other invalid model responses.
- One successful retry establishes the reproduced validity fix, not broad
  report quality; offline Q3 evaluation and mandatory human review remain the
  applicable safeguards.

## Decisions needed

None.
