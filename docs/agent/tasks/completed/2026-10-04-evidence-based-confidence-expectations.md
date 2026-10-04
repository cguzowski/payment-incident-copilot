# Task: Evidence-based post-investigation confidence expectations

Status: Complete
Created: 2026-10-04
Owner: Christopher Guzowski

## Goal

Calibrate expected LOW/MEDIUM/HIGH independently from the report's selected
confidence using explicit evidence rules, as authorized by the owner.

## User story

As an analyst, I want Confidence match to expect the level supported by the
information supplied to the investigation, with an inspectable rationale.

## Chosen contract

Use only the decision-bound latest operational snapshot and the original key's
required machine signals. Preserve original keys and immutable comparisons.
LOW: degraded, empty, invalid, missing required signals or symptom-only support.
MEDIUM: usable bounded mechanism observations with unresolved causal/scope
context. HIGH: a recognized direct diagnosis plus its expected failure, both
required by the key, ordered within one valid window, matching positive counts,
and no additional signals. Initial direct signatures are expired TLS certificate
with handshake failure, HSM quorum loss with signing timeout, and HSM firmware
protocol mismatch with signing failure. These support a narrow mechanism;
independent confirmation is not required. Counts alone never justify HIGH.
Unknown signatures cannot reach HIGH. Historical applicable evidence cannot
override a degraded latest snapshot. Rules never inspect actual confidence,
report rationale, human response, scenario IDs or severity.

## In scope

Versioned deterministic expectations and provenance, exact confidence scoring,
UI rationale and original-key display, regression tests and replay of the two
retained terminal comparisons. Archive the preceding completed confidence task.

## Out of scope

Report generation changes, oracle edits, historical mutation, other metric
formulas, weights, bands, approval formula, text judge, offline evaluator,
new evidence schemas, O2-O4, commits or pushes.

## Constraints

Keep 0/100 exact confidence match. Version the changed expectation contract.
Preserve missing/contradictory evidence. No general semantic entailment claim.
The signature set is deliberately bounded; no probability or frequency claim.

## Acceptance criteria

- [x] Independent rules cover LOW/MEDIUM/HIGH and important failure paths.
- [x] Confidence match uses the calibrated level; other formulas remain intact.
- [x] UI/artifacts expose rule version, rationale, original and calibrated levels.
- [x] Latest snapshot binding prevents historical evidence from inflating confidence.
- [x] Both recent retained runs independently expect MEDIUM; old bytes remain intact.
- [x] Focused tests and full verification pass; limitations are documented.

## Test plan

ComparisonServiceTest: original HIGH versus actual MEDIUM with bounded rate-limit
evidence must score 100, not 0; artifact retains original key and new metadata.
ConfidenceExpectationTest: direct signatures, missing counterpart, unequal counts,
reversed ordering, competing signals, invalid windows/counts, degraded latest with
strong history, symptom-only observations, missing required signals, unknown
signals and report-level independence. ComparisonRubricTest preserves existing
scoring thresholds and formula. comparison-ui.test.cjs verifies calibrated
expected level/rationale and original key with legacy fallback. Replay the exact
retained operational inputs from the owner's last two comparisons.

## Progress notes

- Owner selected explicit evidence rules. Plan: regression red, minimal rules and
  provenance/UI, focused checks, retained-input replay and full repository gate.

## Completion evidence

- Red: tmp/confidence-rules/red.log reproduces confidence expected 100 versus actual 0. The sandbox Node subprocess was denied; the escalated ui-red.log records the intended UI HIGH-versus-MEDIUM failure.
- Unknown/competing signals failed as MEDIUM rather than LOW in unknown-red.log before the classification guard was added.
- Green: generator-green.log passed all 82 generator tests with zero failures/errors/skips, including eight nested Node UI cases.
- retained-input-replay.json replays the exact latest evidence and unchanged text scores from the owner's last two terminal comparisons. Both independently expect MEDIUM and score confidence 100. Rate-limit report 69 becomes 94; DNS report 50 becomes 75. The existing decision formula now expects approval for the first, rejection for the second. Original artifacts and original oracle/judge-prompt SHA-256 hashes remain unchanged.
- Initial full verification passed application tests, formatting and builds, then failed the final git diff check on a trailing blank line in this task document. The document was repaired; the final full gate passed with 368 API, 9 MCP, 82 generator and 103 console tests, zero failures/errors/skips, eight nested Node UI cases, formatting/builds/Compose/diff checks. Logs: full-verification.log and full-verification-final.log.

- Final Repository scope and git diff --check passed after completion documentation. Console restored on port 4200 (HTTP 200); main API health was unavailable and its process was not restarted. No new live comparison or human decision was created. The verified generator package takes effect on its next startup.

## Remaining limitations

Explicit aggregate signatures do not prove deeper causes, affected paths or final
payment outcomes. No added independent source or semantic knowledge checker.

## Decisions needed

None; owner authorized evidence-based expectations.
