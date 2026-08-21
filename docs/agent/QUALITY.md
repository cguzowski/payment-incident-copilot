# Quality and validation

Last reviewed: 2026-08-20

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

## Testing expectations

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
- Relevant automated tests pass.
- Failure and invalid-input behavior is intentional.
- Database changes use migrations.
- Public API changes are documented.
- Audit-impacting behavior is tested.
- Documentation reflects the resulting system.
- The final diff contains no secrets, generated build output, or unrelated
  refactoring.
