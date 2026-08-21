# Copilot API instructions

These instructions extend the repository root `AGENTS.md` for work under this
service.

## Service responsibility

This service owns alert intake, incident state, investigation orchestration,
knowledge retrieval, report generation, human decisions, and audit history.
It consumes MCP tools but does not implement synthetic source systems.

## Java and Spring rules

- Use Java 21, Spring Boot, and Spring AI versions managed by the root POM.
- Organize code by feature rather than by global technical layer.
- Keep controllers thin and place workflow decisions in application services.
- Do not return JPA entities from API endpoints; use explicit request and
  response records.
- Validate all external input at the HTTP boundary.
- Use Flyway for schema evolution and set Hibernate DDL behavior to `validate`.
- Use UTC instants internally and ISO-8601 timestamps at boundaries.
- Use stable incident, investigation, evidence, and correlation identifiers.
- Prefer structured errors based on Spring `ProblemDetail`.

## AI rules

- Validate generated reports against a predefined schema.
- Store source references with every evidence-backed assertion.
- Represent missing evidence explicitly; never ask the model to fill gaps.
- Keep prompts versioned and testable.
- Do not allow a model response to change incident state without an explicit
  operator decision.

## Testing

- Unit-test domain and workflow decisions without starting Spring.
- Use integration tests for persistence, migrations, HTTP contracts, and MCP
  boundaries.
- Prefer Testcontainers PostgreSQL for database integration tests.
- Cover failure and idempotency paths, not only successful requests.
