# Quality and validation

Last reviewed: 2026-08-21

## Standard commands

From the repository root:

```bash
mvn clean verify
docker compose config
```

After the Angular application is generated:

```bash
cd frontend/operator-console
npm ci
npm test -- --watch=false
npm run build
```

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
