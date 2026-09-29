# Task: Gate the synthetic answer-key reveal

Status: Complete
Created: 2026-09-24
Owner: Christopher Guzowski

## Goal

Remove the immediate browser answer-key leak and establish the first auditable
Q1 evaluation-integrity boundary.

## User story

As an evaluator, I want the deterministic answer key withheld until the exact
incident has a final human decision, so it cannot influence evidence gathering,
retrieval, report generation, or the decision being evaluated.

## Chosen contract

- `APPROVED` and `REJECTED` are the only frozen states that permit reveal.
- Incident generation returns no answer-key fields.
- Reveal derives the scenario from the canonical incident detail returned by
  the copilot API; the browser does not submit a scenario or alert reference.
- Every successful reveal records attributable audit metadata through the
  generator's audit sink and returns the oracle version and reveal timestamp.
- The generator remains independently buildable and does not access copilot
  persistence directly.

## In scope

- A terminal-state-gated generator reveal endpoint.
- Copilot incident-detail lookup through the existing public HTTP API.
- Operator attribution, reveal timestamp, terminal state, and oracle-version
  metadata.
- Generator UI behavior and documentation.
- Deterministic unit, HTTP-contract, and static-UI tests.

## Out of scope

- Rewriting the frozen v1 SynTen corpus or its hashes.
- Claiming corpus-independent retrieval or report evaluation.
- Automated report grading, live-model runs, authentication, or deployment.
- Adding an answer-key reveal event to the copilot investigation timeline.

## Constraints

- Preserve the sparse alert and read-only MCP contracts.
- Do not expose truth through errors, logs before reveal, or copilot inputs.
- Treat copilot lookup failures as safe reveal failures.
- Preserve generator/copilot service independence and synthetic-only data.

## Acceptance criteria

- [x] Generation responses contain no answer key or truth fields.
- [x] Reveal is rejected before an explicit terminal human decision.
- [x] Reveal succeeds for both `APPROVED` and `REJECTED` incidents and is bound
      to the incident's canonical opaque alert reference.
- [x] Successful reveal records and returns operator, time, terminal state, and
      oracle-version metadata without sending truth to the copilot API.
- [x] The browser requests the answer key only after an explicit reveal action
      and explains the terminal-decision prerequisite.
- [x] Focused generator tests and the full repository verification gate pass.

## Test plan

- Controller contract tests for absent generation truth, valid reveal, invalid
  input, and safe conflict/upstream errors.
- Service tests for NEW/INVESTIGATING/AWAITING_REVIEW denial, APPROVED/REJECTED
  success, canonical-reference decoding, unknown scenarios, and audit capture.
- HTTP client tests for tenant-scoped incident lookup and safe failures.
- Static UI tests proving no generation-time answer rendering and explicit
  reveal-only fetch behavior.
- Run generator `clean verify`, then `./verify.ps1`.

## Progress notes

- 2026-09-24: Owner activated the first Q1 implementation slice and selected a
  terminal human decision as the frozen-output boundary.
- 2026-09-24: Confirmed the generation endpoint serialized the full answer key
  and the browser rendered it immediately behind a collapsed element.
- 2026-09-24: Removed truth, scenario code, and rarity from generation output;
  added terminal-state authorization against canonical copilot incident detail,
  attributable reveal metadata, truth-free audit logging, and explicit UI
  reveal behavior.

## Completion evidence

- Red: generator tests failed to compile because the reveal service, copilot
  incident-status client, response/audit contracts, and truth-free generation
  DTO did not exist.
- `./mvnw.cmd -f syntheticIncidentGenerator/pom.xml clean verify` passed 29
  tests with zero failures, errors, or skips, plus packaging and Spotless.
- The first `./verify.ps1` run stopped correctly because Docker was unavailable
  and 63 PostgreSQL tests were skipped. Docker Desktop was started and the gate
  was rerun from the beginning.
- Final `./verify.ps1` passed: 290 copilot API, 9 operations MCP, 29 generator,
  and 78 Angular tests with zero failures, errors, or skips, plus formatting,
  production build, Compose, structural checks, and `git diff --check`.
- ADR-0013 records the terminal-decision reveal boundary and accepted limits.

## Remaining limitations

- The v1 corpus contains text derived from scenario `truth.rootCause`; Q1 is not
  complete until an independently authored, versioned evaluation corpus path is
  approved and implemented.
- This slice records reveal metadata in the generator audit sink, not the
  copilot investigation timeline.

## Decisions needed

None.
