# Project status

Last updated: 2026-08-20

## Current milestone

Milestone 0 — Establish the monorepo, shared context, and build boundaries.

## Completed

- Selected the Payment Incident Investigation Copilot vertical slice.
- Selected Java, Spring Boot, Spring AI, Angular, AWS, PostgreSQL, and pgvector.
- Defined separate copilot API and synthetic MCP server boundaries.
- Created the initial monorepo and coding-agent scaffold.

## In progress

- Validate the generated project structure on the development machine.

## Next

1. Initialize the Angular workspace.
2. Confirm the Java modules resolve and build.
3. Start PostgreSQL/pgvector locally.
4. Implement synthetic alert ingestion and the operator alert queue.
5. Choose one concrete payment incident scenario.

## Blockers

- None recorded.

## Known deliberate gaps

- No authentication yet.
- No MCP tools implemented yet.
- No Bedrock model configured yet.
- No knowledge-ingestion pipeline yet.
- No AWS infrastructure selected yet.

## Update rule

Keep this file factual and brief. Move implementation detail into task briefs,
architecture documents, or ADRs.
