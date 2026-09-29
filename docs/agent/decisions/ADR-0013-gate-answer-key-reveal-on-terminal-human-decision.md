# ADR-0013: Gate answer-key reveal on a terminal human decision

Status: Accepted  
Date: 2026-09-24  
Decision owner: Christopher Guzowski

## Context

The synthetic incident generator returned each scenario's deterministic answer
key in the generation response. Collapsing that data in the browser did not
prevent it from influencing evidence gathering, retrieval, report generation,
or the human decision. Q1 requires a verifiable reveal boundary.

## Decision drivers

- Keep the oracle outside every evaluated copilot input.
- Use an authoritative persisted lifecycle state rather than browser state.
- Preserve independent generator and copilot deployments.
- Fail closed when the copilot cannot confirm the incident state.
- Attribute successful reveals without logging oracle content.

## Considered options

### Option A: Reveal after report generation

- Advantage: evaluators can compare the report before deciding.
- Disadvantage: the oracle can influence the human decision, so the decision is
  no longer independently evaluated.

### Option B: Reveal after a terminal human decision

- Advantage: evidence, retrieval, report, and decision outputs are frozen.
- Disadvantage: an evaluator must record approval or rejection before learning
  the deterministic expected result.

## Decision

The answer key remains sealed until the copilot API reports the incident as
`APPROVED` or `REJECTED`. Generation responses contain neither truth nor the
explicit scenario code or rarity. A reveal request supplies only the incident
identifier and synthetic operator identity. The generator retrieves canonical
tenant-scoped incident detail from the copilot API, decodes the stored opaque
alert reference, resolves `scenario-oracle/v1`, records truth-free reveal
metadata, and only then returns the answer key.

`NEW`, `INVESTIGATING`, and `AWAITING_REVIEW` return conflict without oracle
content. Missing, malformed, mismatched, or unavailable copilot state fails
closed.

## Consequences

### Positive

- The browser no longer receives the oracle during generation.
- The human decision cannot be changed after the reveal boundary.
- Successful reveals carry incident, operator, terminal status, scenario,
  oracle version, and timestamp metadata without logging truth.

### Negative or accepted tradeoffs

- Generator audit metadata is currently emitted to its structured application
  log; it is not projected into the copilot investigation timeline.
- The `sig-v1` reference remains compatible with deterministic MCP evidence and
  internally carries a scenario code.
- The oracle remains present in the generator deployment, so package dependency
  tests and the terminal reveal boundary remain security-relevant controls.

## Validation or revisit trigger

Revisit when reveal events must be retained in the copilot audit timeline or
when the `sig-v1` evidence reference is versioned. ADR-0014 records the completed
observable/oracle and corpus boundary.
