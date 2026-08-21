# Payment Incident Investigation Copilot

An auditable, human-in-the-loop investigation assistant for synthetic payment
incidents. The application gathers operational evidence, retrieves approved
runbooks and policies, produces a structured AI-assisted investigation report,
and presents it to an operator for approval or rejection.

This repository is intentionally a monorepo. Its applications remain separate
build and deployment units.

## Repository map

```text
frontend/operator-console/          Angular operator interface
backend/copilot-api/                Main Spring Boot workflow and AI service
backend/operations-mcp-server/      Deterministic synthetic operational tools
infra/                              Local and AWS infrastructure
docs/agent/                         Durable context for humans and coding agents
```

## Current vertical slice

```text
Synthetic alert
  -> Operator alert queue
  -> Investigation started
  -> Context gathered through MCP tools
  -> Runbooks and policies retrieved
  -> Evidence normalized
  -> Structured report generated
  -> Operator approves or rejects
  -> Decision and evidence preserved in audit history
```

## Prerequisites

- Java 21
- Maven 3.9+
- Docker with Docker Compose
- Node.js and Angular CLI when the frontend is generated
- AWS account with Amazon Bedrock model access when AI integration begins

## Start local PostgreSQL with pgvector

```bash
cp .env.example .env
docker compose up -d postgres
```

## Build the Java services

```bash
mvn clean verify
```

## Run a service

```bash
mvn -pl backend/copilot-api -am spring-boot:run
mvn -pl backend/operations-mcp-server -am spring-boot:run
```

## Frontend initialization

The Angular source has deliberately not been generated yet. From
`frontend/operator-console`, initialize it with the Angular CLI using routing,
SCSS, strict TypeScript, and no nested Git repository. Preserve the existing
`AGENTS.md` after generation.

## Before contributing

Read `AGENTS.md`, followed by `docs/agent/STATUS.md` and
`docs/agent/tasks/current.md`. The closest nested `AGENTS.md` contains
service-specific rules.

## Safety boundary

This is a portfolio demonstration using synthetic data. It does not process
payments, perform fraud detection, execute remediation, or use real customer
financial information.
