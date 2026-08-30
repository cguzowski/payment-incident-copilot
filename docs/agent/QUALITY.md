# Quality and validation

Last reviewed: 2026-08-30

## Standard commands

The authoritative completion gate is:

```powershell
./verify.ps1
```

It verifies Java 21, Node.js 24.14.1, npm 10.8.3, the verification script's
PowerShell tests, repository credential safety, the pinned Maven Wrapper build,
zero skipped backend and frontend tests, locked frontend installation,
Prettier, the Angular production build, Compose configuration, and
`git diff --check`. CI delegates to this same implementation.

Focused scopes are available during development:

```powershell
./verify.ps1 -Scope Backend
./verify.ps1 -Scope Frontend
./verify.ps1 -Scope Repository
```

Focused scopes do not replace the unscoped completion gate.

## Test-driven development

All production behavior changes follow a red-green-refactor cycle:

1. Translate the user story and each acceptance criterion into named test
   cases at the lowest suitable level.
2. Write one focused test for the next behavior and run it to confirm that it
   fails for the intended reason.
3. Implement only enough production code to make that test pass.
4. Refactor without changing behavior while keeping the tests green.
5. Repeat for the remaining criteria, then run the broader relevant suite.

For a defect, first add a failing regression test that reproduces it. Do not
weaken a valid test to accommodate an implementation. If an acceptance
criterion requires manual validation, record why it cannot be automated and
add the closest meaningful automated coverage.

Changes with no executable behavior, such as documentation-only edits, do not
require artificial tests. Run the relevant static or structural validation and
record that evidence instead.

## Model-provider testing

- Automated tests explicitly set `spring.ai.model.chat=none` and
  `spring.ai.model.embedding=none`.
- Tests at a model-facing boundary use mocked responses or deterministic
  doubles and cover malformed output, unavailability, and timeout behavior.
- Normal automated verification never depends on Ollama, Bedrock, AWS
  credentials, model downloads, or external network access.
- Live Ollama smoke checks are explicit local-development checks and do not
  replace the deterministic completion gate.

## Coverage expectations

- Unit tests for domain rules and state transitions
- Integration tests for HTTP contracts, PostgreSQL, Flyway, and MCP boundaries
- Contract tests for report schemas and MCP tool schemas
- Repeatable synthetic scenarios for demonstrations
- Failure tests for unavailable sources, incomplete evidence, invalid model
  output, duplicate alerts, and rejected reports
- End-to-end happy path after the vertical slice stabilizes

## Observability expectations

- Use structured logs.
- Include correlation, incident, investigation, and tool-call identifiers.
- Never log credentials, prompts containing sensitive data, or full model
  payloads indiscriminately.
- Expose health and readiness information suitable for container deployment.

## Definition of done

- Acceptance criteria are demonstrably satisfied.
- Each acceptance criterion is mapped to automated coverage or a documented
  manual-verification reason.
- Red-phase and green-phase evidence is recorded for behavior changes.
- Relevant automated tests pass.
- Failure and invalid-input behavior is intentional.
- Database changes use migrations.
- Public API changes are documented.
- Audit-impacting behavior is tested.
- Documentation reflects the resulting system.
- The final diff contains no secrets, generated build output, or unrelated
  refactoring.
