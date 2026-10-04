# Task: Restore attainable HIGH with conservative confidence selection

Status: Complete
Created: 2026-10-04
Owner: Christopher Guzowski

## Goal

Make HIGH attainable while favoring MEDIUM for unconfirmed aggregate evidence
and retaining LOW for weak support.

## User story

As an analyst, I want confidence calibrated to the strength of a bounded
investigation conclusion rather than an absolute ban on HIGH.

## Context

The owner explicitly clarified that HIGH must remain possible despite missing
independent confirmation. This supersedes O1's ceiling, archived under
[completed tasks](2026-10-04-o1-confidence-ceiling.md). The preceding
request to compensate Confidence match for that ceiling is no longer needed;
retain exact key matching and do not change the comparison rubric.

## Chosen contract

Version generation to report-prompt/v7. AVAILABLE nonempty evidence permits
LOW/MEDIUM/HIGH. Normally choose MEDIUM when observations and guidance support
a bounded mechanism/next step but confirmation or material uncertainty remains.
Reserve HIGH for unusually substantial, direct and consistent incident-window
observations supporting the specific narrow mechanism, applicable guidance,
and no material contradiction or similarly supported alternative. Independent
confirmation strengthens confidence but is not a prerequisite; HIGH never
means a deeper root cause or final payment outcome is confirmed. Mere severity,
volume, repeated categories/history or generic guidance cannot justify HIGH.
LOW applies to weak, ambiguous or conflicting support. Lack of support for a
cause and next step retains INSUFFICIENT_EVIDENCE/LOW/null/gaps.
Partial/unavailable/empty evidence keeps its existing independent LOW/null
validation. Rationale names supporting observations and remaining uncertainty.

## In scope

Remove provider/parser HIGH ceiling, revise confidence prompt and provenance,
regression tests, documentation and bounded live diagnostic checks.

## Out of scope

Comparison/oracle changes, historical mutation, context/timeout fixes, O2-O4,
new providers, semantic confidence enforcement, automatic approvals, commits
and pushes. Preserve current malformed-attempt history.

## Constraints

No scenario labels or answer keys in generation. No confidence quota or forced
MEDIUM assignment. Observational strength guides prompt selection; structural
validation cannot prove semantic support. Preserve citations and human review.

## Acceptance criteria

- [x] Nonempty AVAILABLE evidence allows and parses all three confidence levels.
- [x] Prompt favors MEDIUM, reserves HIGH for direct substantial narrow support,
      and acknowledges missing confirmation without an absolute HIGH ban.
- [x] Degraded/empty evidence independently rejects MEDIUM/HIGH/assertions and
      retains INSUFFICIENT_EVIDENCE/LOW/null/gaps.
- [x] Valid HIGH persists as AVAILABLE/AWAITING_REVIEW with one model call;
      historical HIGH remains readable and v7 provenance is stored.
- [x] Focused and full verification pass; bounded live diagnostics and their
      limits are retained without claiming an established confidence frequency.

## Test plan

ReportPromptAndParserTest.availableAggregateEvidenceKeepsAllConfidenceLevels:
schema and parser allow LOW/MEDIUM/HIGH with exact sources.
confidenceRationalePreservesMissingConfirmation: conservative criteria in v7.
Existing six degraded-context cases additionally reject HIGH and MEDIUM with
null insufficient-evidence reports.
ReportApiPostgresIntegrationTest.persistsAvailableHighForHumanReview: one call,
AVAILABLE history, AWAITING_REVIEW state and v7 metadata; no approval.
Existing historical and malformed-output tests preserve audit/failure behavior.
Bounded live replay of existing sufficient, weak and partial synthetic contexts,
without adding attempts or revealing keys. Record failures and no retries.

## Verification commands

./mvnw.cmd -pl backend/copilot-api -Dtest=ReportPromptAndParserTest,ReportApiPostgresIntegrationTest test
./verify.ps1

## Progress notes

- 2026-10-04: Owner clarified confidence intent before this implementation.
  Plan: failing regressions, minimal ceiling removal/v7 prompt, focused/full
  verification and live diagnostic evidence. Prompt behavior cannot guarantee
  a particular frequency; context truncation is a known separate limitation.

## Completion evidence

- Red: tmp/confidence-v7-red-docker.log records the expected AVAILABLE versus
  actual MALFORMED HTTP failure, missing HIGH schema eligibility, parser
  rejection and missing v7 instructions/provenance. An earlier sandbox run
  could not access Docker; the Docker rerun executed all 24 tests with no skips.
- Green: tmp/confidence-v7-green.log records 24 passing prompt/parser and HTTP
  tests, including one-call HIGH persistence for human review and historical
  HIGH readback. No comparison, oracle or offline evaluator change was made.
- Initial live v7 diagnostics retained supported MEDIUM, weak MEDIUM and partial
  LOW/null results under tmp/confidence-v7-live/. The weak case incorrectly
  borrowed confidence from synthetic provenance and generic guidance. These are
  nonpersisted direct model replays, not answer-key comparisons or new attempts.
- The weak-support instruction regression first failed in
  tmp/confidence-v7-weak-red.log. The tightened instruction passed all 17
  prompt/parser tests in tmp/confidence-v7-unit-final.log. Both prompt hashes and
  initial/final diagnostic responses are retained; no failure was overwritten.
- The first full gate passed backend/generator verification then failed npm ci
  on the active console's esbuild.exe lock. Console was temporarily stopped;
  final full verification passed in tmp/confidence-v7-full-final.log: 368 API,
  9 MCP, 74 generator and 103 console tests, zero failures/errors/skips, seven
  nested generator UI cases, formatting, builds, Compose and repository checks.
  The final weak diagnostic timed out at
  the unchanged 120-second limit; its failure is retained under weak-final/.
- Final prompt hash: f1f7e74cae0b2c656608dd666e1dc21f7f980b42c4de9df66532d82a33358420.
  Initial diagnostic hash: d5524424bd32ba24459cf3d941d9d24bed9ebeda0a31aa5a0aacd433723f283d.
  Both are development iterations of v7, with exact context/schema/output files
  retained; only the final prompt is packaged. This task adds no new reports,
  decisions or reveal/comparison artifacts to the owner's investigation.
- Final ./verify.ps1 -Scope Repository and git diff --check passed; task/ADR
  links resolve and comparison/evaluator/base-schema/frozen-corpus paths have
  no diff. Console was restored on port 4200 (HTTP 200), API health is UP, and
  the owner's API process was not restarted. Activate v7 with the new API build.

## Remaining limitations

No calibrated probability or semantic support checker. Known 4,209-to-2,050
token truncation affected the owner's webhook investigation under v6; this
task does not change its historical attempts or resolve context budgeting.
The first weak diagnostic selected MEDIUM using unsupported synthetic metadata;
the tightened prompt's live weak selection remains unverified after timeout.
The initial partial diagnostic also copied guidance into observations and ended
some fields mid-sentence despite correct LOW/null structure. No broad confidence
distribution or improvement claim is established by these diagnostic samples.

## Decisions needed

None; the owner's clarification authorizes the revised confidence contract.

