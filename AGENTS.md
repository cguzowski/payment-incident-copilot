# AGENTS.md

## Purpose

These instructions apply to every AI coding agent working in this repository.

Act as a disciplined software engineer. Make the smallest coherent change that completely satisfies the requested objective while preserving existing behavior and repository conventions.

## Before Making Changes

Before editing:

1. Identify the exact objective.
2. Locate the relevant files and existing implementation.
3. Determine the acceptance criteria.
4. Identify important risks, dependencies, and edge cases.
5. Find the repository's documented verification commands.

Read only the files needed for the current objective.

If a requirement is materially ambiguous, ask for clarification instead of silently choosing a product, business, security, or architectural decision.

## Scope Discipline

- Work on one objective at a time.
- Do not modify unrelated files.
- Do not perform unrelated cleanup or refactoring.
- Do not add dependencies unless they are necessary.
- Do not redesign architecture unless the task requires it.
- Preserve existing public behavior unless a change is explicitly requested.
- Follow the repository's existing naming, structure, formatting, and design patterns.
- Do not invent requirements.
- Do not hide assumptions; state consequential assumptions explicitly.

## Planning

For substantial work, create a short implementation plan before changing code.

Substantial work includes:

- Changes across multiple components or modules
- API or database changes
- Authentication, authorization, or security-sensitive work
- Architectural changes
- Business-critical behavior
- Work with unclear implementation boundaries

The plan should identify:

- The intended outcome
- The files or components likely to change
- The implementation sequence
- Important risks and edge cases
- How the result will be verified

Small, obvious, low-risk edits may be implemented directly.

If investigation invalidates the plan, stop and revise the plan before continuing.

## Implementation

- Prefer incremental, reviewable changes.
- Use the smallest implementation that fully solves the problem.
- Keep business logic explicit and understandable.
- Reuse existing abstractions when they fit.
- Avoid speculative abstractions and premature generalization.
- Avoid broad boilerplate rewrites.
- Keep changes consistent with surrounding code.
- Add comments only when they explain non-obvious reasoning.
- Never expose, log, or commit secrets or sensitive credentials.

## Testing and Verification

Never claim that work is complete without verification evidence.

When practical, define or write the important tests before implementing the behavior.

Prioritize tests for:

- Business-critical behavior
- Security-sensitive behavior
- Regression risks
- Important edge cases
- Integration boundaries
- User-visible behavior

Do not test every line merely to increase test coverage. Test meaningful behavior.

Run the checks relevant to the change, including where applicable:

- Focused unit tests
- Integration tests
- Backend tests
- Frontend tests
- Type checking
- Linting and formatting checks
- Build verification
- Security checks
- Browser-based verification
- Screenshots or visual comparison for UI changes

Start with the most focused verification and expand when appropriate.

If a required check cannot be run, report:

- The command or verification that was not run
- Why it could not be run
- What remains unverified

Do not describe unverified behavior as working.

## Parallel Work

Use parallel agents only when work can be separated into clearly isolated objectives, such as:

- Focused research
- Debugging
- Security review
- Code review
- Independent implementation areas

Each parallel task must have a narrow objective and return a concise result.

When multiple agents modify code:

- Use a separate worktree and branch for each task.
- Never allow multiple agents to edit the same worktree concurrently.
- Keep each task isolated until its changes are reviewed and integrated.

Preferred model:

> One task = one conversation = one worktree = one branch

## Repository Safety

- Inspect existing changes before editing.
- Treat existing uncommitted changes as user-owned.
- Do not overwrite or discard unrelated work.
- Do not use destructive Git operations unless explicitly requested.
- Do not revert changes that were not created for the current task.
- Keep generated files and temporary artifacts out of the repository unless required.

## Completion Standard

A task is complete only when:

- The requested scope has been implemented.
- Relevant tests pass.
- Relevant type, lint, and build checks pass.
- User-visible changes have been verified where applicable.
- No unrelated changes were introduced.
- Remaining risks or limitations are clearly reported.

The final report should state:

1. What changed
2. Why it changed
3. Which files or components were affected
4. What verification was performed
5. Any remaining risks, limitations, or follow-up work

# Payment Incident Investigation Copilot

## Mission

Build a portfolio-quality application that helps a payment operations analyst
investigate synthetic payment incidents using retrieved operational context,
approved knowledge, an LLM-generated report, and human review.

The product provides decision support. It does not autonomously resolve
incidents or execute payment actions.

## Required context

Before planning or changing code, read:

1. `docs/agent/PROJECT.md` — product goal and MVP scope.
2. `docs/agent/CONSTRAINTS.md` — non-negotiable boundaries.
3. `docs/agent/STATUS.md` — current repository state.
4. `docs/agent/tasks/current.md` — active task and acceptance criteria.
5. The nearest service-specific `AGENTS.md` — local implementation rules.

Read when relevant:

- `docs/agent/ARCHITECTURE.md` — cross-service, API, or data-flow changes.
- `docs/agent/DOMAIN.md` — payment or incident terminology changes.
- `docs/agent/QUALITY.md` — verification commands and quality requirements.
- `docs/agent/decisions/` — ADRs related to the affected architecture.

## Repository boundaries

- `frontend/operator-console`: Angular operator interface.
- `backend/copilot-api`: incidents, investigations, retrieval, reports,
  decisions, and audit history.
- `backend/operations-mcp-server`: deterministic synthetic operational tools.
- `infra`: local and AWS deployment configuration.
- `docs`: architecture, domain, task, and decision records.

Keep each application independently buildable and deployable.

## Product boundary

The current vertical slice is:

```text
synthetic alert
-> alert queue
-> start investigation
-> gather context through MCP
-> retrieve runbooks and policies
-> normalize evidence
-> generate structured report
-> human approve or reject
-> preserve audit history
```

Do not expand the MVP into real payment processing, autonomous remediation,
real fraud detection, multiple payment domains, or speculative infrastructure.

## Responsible-AI rules

- LLM output is advisory and requires human review.
- Every report conclusion must reference supporting evidence.
- Clearly distinguish observed facts from AI inference.
- Never fabricate missing operational data.
- Preserve model, prompt, evidence, and decision metadata for auditability.
- Never allow the model to execute irreversible operational actions.
- Never commit secrets, credentials, personal data, or real payment data.

## Engineering workflow

1. Inspect the relevant code and documentation.
2. Restate the task and surface material ambiguity.
3. Propose the smallest end-to-end change.
4. Implement without unrelated refactoring.
5. Run relevant tests and builds.
6. Review the resulting diff.
7. Update `docs/agent/STATUS.md` when project state changes.
8. Record consequential architecture decisions in an ADR.

## Dependency policy

- Reuse existing dependencies when practical.
- Explain why a new production dependency is necessary before adding it.
- Do not add infrastructure solely for hypothetical future scale.
- Keep dependency versions in Maven or npm configuration, not documentation.

## Definition of done

A change is complete only when acceptance criteria are met, relevant tests
pass, invalid inputs are handled, logs exclude sensitive information, behavior
changes are documented, and the diff contains no unrelated edits.
