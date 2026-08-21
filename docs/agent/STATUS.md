# Project status

Last updated: 2026-08-21

## Current milestone

Milestone 0 — Validate the foundation and agent workflow.

## Completed

- Selected the Payment Incident Investigation Copilot vertical slice.
- Selected Java, Spring Boot, Spring AI, Angular, AWS, PostgreSQL, and pgvector.
- Defined separate copilot API and synthetic MCP server boundaries.
- Created the initial monorepo and coding-agent scaffold.
- Confirmed the Maven reactor builds locally.
- Confirmed the latest pushed commit passes GitHub Actions CI.

## In progress

- Complete local PostgreSQL/pgvector validation.
- Choose the first payment incident family and alert contract.

## Next

1. Install or expose the Docker CLI and validate the Compose configuration.
2. Choose one concrete payment incident scenario.
3. Implement synthetic alert ingestion and the operator queue test-first.
4. Initialize the Angular workspace only when the queue tests require it.

## Blockers

- Docker CLI is not available in the current development environment, so local
  Compose and PostgreSQL validation remain unverified.
- The active task requires a decision on the first incident family and sample
  alert fields before implementation.

## Known deliberate gaps

- No authentication yet.
- No MCP tools implemented yet.
- No Bedrock model configured yet.
- No knowledge-ingestion pipeline yet.
- No AWS infrastructure selected yet.

## Update rule

Keep this file factual and brief. Move implementation detail into task briefs,
architecture documents, or ADRs.
