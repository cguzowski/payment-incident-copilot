# Repository agent instructions

These rules apply to every agent working in this repository. Nested
`AGENTS.md` files add service-specific rules; they do not replace this file.

## Required context

Before planning or editing, read:

1. `docs/agent/PROJECT.md` — product goal and MVP scope.
2. `docs/agent/CONSTRAINTS.md` — non-negotiable boundaries.
3. `docs/agent/STATUS.md` — current facts and blockers.
4. `docs/agent/tasks/current.md` — active user story and acceptance criteria.
5. The nearest service-specific `AGENTS.md`, when one exists.

For executable changes, also read `docs/agent/QUALITY.md`.

Read only when relevant:

- `docs/agent/ARCHITECTURE.md` for cross-service, API, persistence, or data-flow
  changes.
- `docs/agent/DOMAIN.md` for payment or incident terminology.
- Relevant records under `docs/agent/decisions/` before changing an accepted
  architectural decision.

Do not load the entire documentation tree by default.

## Operating rules

- Work on one explicit objective at a time.
- Use the user story and acceptance criteria as the behavioral contract.
- Inspect Git status and existing changes before editing. Treat unrelated
  changes as user-owned.
- Make the smallest coherent change that fully satisfies the objective.
- Preserve public behavior unless the task explicitly changes it.
- Do not perform unrelated cleanup, add speculative abstractions, or introduce
  a dependency before it is required.
- Follow existing naming, structure, and local conventions.
- Never commit secrets, credentials, personal data, or real payment data.
- Do not invent product, security, or architecture decisions. Stop and ask when
  a missing decision would materially change the result.

## Test-driven development

Every production behavior change follows red-green-refactor:

1. Map the user story and every acceptance criterion to named tests.
2. Write the smallest meaningful test before production code.
3. Run it and confirm it fails for the intended reason (red).
4. Write only enough production code to make it pass (green).
5. Refactor while keeping the relevant tests green.
6. Run the focused tests, then the broader relevant suite.

Do not implement behavior first and backfill tests. Reproduce defects with a
failing regression test before fixing them. If a criterion cannot be automated,
record why and perform the strongest available manual verification.
Documentation-only changes require appropriate static checks, not artificial
tests.

## Product guardrails

- Use synthetic data only; this product does not process or move money.
- LLM output is advisory and always requires human review.
- Separate observed facts from AI inference.
- Every report conclusion must reference supporting evidence.
- Preserve missing, unavailable, or contradictory evidence; never fabricate it.
- Preserve model, prompt, evidence, retrieval, and decision metadata needed for
  auditability.
- Never allow AI to approve reports or execute irreversible operational actions.
- Carry tenant identity through persistence and retrieval boundaries.
- Keep the operator console, copilot API, and operations MCP server independently
  buildable and deployable.

## Workflow

1. Inspect the relevant implementation, tests, instructions, and current diff.
2. Restate the objective and surface material ambiguity.
3. For substantial work, write a concise plan with risks and verification.
4. Execute the TDD cycle one behavior at a time.
5. Run the documented focused and broader verification commands.
6. Review the final diff for scope, duplication, secrets, and generated output.
7. Update `docs/agent/STATUS.md` when project state changes.
8. Update the active task with factual progress and verification evidence.
9. Record consequential architectural decisions in an ADR.

Substantial work includes cross-service changes, API or database changes,
security-sensitive behavior, architecture changes, and business-critical flows.
Do not pause for approval after a plan unless a material decision is unresolved.

## Parallel work

- One writing agent per worktree.
- Give each parallel task a narrow, non-overlapping objective and its own branch
  and worktree.
- Never allow multiple agents to edit the same working tree concurrently.
- Review and verify each isolated diff before integration.

## Completion standard

Do not claim completion without evidence. A completed behavior change has:

- Acceptance criteria mapped to passing tests or a documented manual exception.
- Focused tests and relevant suites passing.
- Type, lint, build, security, and visual checks passing where applicable.
- Invalid inputs and important failure paths covered.
- Documentation matching the resulting behavior.
- No unrelated changes, secrets, or generated artifacts in the diff.

If a check cannot run, report the exact command, reason, and remaining risk. The
final report must state what changed, why, affected files, verification, and any
remaining limitations.

## Current task update policy

- `docs/agent/tasks/current.md` is the authoritative task contract.
- Before implementation begins, the agent may clarify the task only with owner
  approval.
- After implementation begins, Goal, User story, Chosen contract, In scope,
  Out of scope, Constraints, Test plan, Acceptance-criteria wording, and
  Decisions needed are locked.
- The agent may update Status, Progress notes, Completion evidence, Remaining
  limitations, and acceptance-criteria checkbox state.
- A checkbox may be marked complete only after the required verification has
  actually executed and passed.
- The agent must not remove, weaken, reinterpret, or silently bypass a
  requirement to match its implementation.
- If implementation reveals that a locked requirement must change, stop and
  request owner approval before editing the task or continuing.
- Preserve the previous completed task under
  `docs/agent/tasks/completed/` before replacing `current.md`.
