# Payment Incident Investigation Copilot

An auditable, human-in-the-loop application for investigating synthetic payment
incidents. It gathers operational evidence, retrieves approved knowledge,
generates a structured AI-assisted report, and requires an operator decision.

This is a portfolio project under active development, not a production payment
system. See the factual [project status](docs/agent/STATUS.md) and the active
[task brief](docs/agent/tasks/current.md).

## Intended workflow

```text
synthetic alert
-> operator queue
-> operator starts investigation
-> read-only MCP tools gather evidence
-> approved runbooks and policies are retrieved
-> Amazon Bedrock proposes an evidence-linked report
-> operator approves or rejects
-> evidence, model metadata, report, and decision remain auditable
```

## Components

| Component | Responsibility |
|---|---|
| `frontend/operator-console` | Alert triage, investigation review, and human decisions |
| `backend/copilot-api` | Workflow, persistence, retrieval, report generation, and audit history |
| `backend/operations-mcp-server` | Deterministic synthetic evidence exposed through read-only MCP tools |
| PostgreSQL/pgvector | Application state, approved knowledge, and vector retrieval |
| Amazon Bedrock | Planned embeddings and structured report generation |

The applications share one repository but remain independently buildable and
deployable. See [Architecture](docs/agent/ARCHITECTURE.md) for ownership and
data flow.

## Non-negotiable boundaries

- Synthetic data only; the application does not process or move money.
- AI output is advisory and always requires human review.
- Observed evidence remains distinct from AI inference.
- Missing or contradictory evidence stays visible.
- No autonomous remediation or approval.
- Evidence, model, prompt, retrieval, and decision provenance is retained.

See [Constraints](docs/agent/CONSTRAINTS.md) for the complete contract.

## Technology

| Area | Technology |
|---|---|
| Backend | Java 21, Spring Boot, Maven |
| Frontend | Angular, TypeScript, SCSS |
| Data | PostgreSQL, pgvector, Flyway |
| AI and integration | Spring AI, Amazon Bedrock, MCP |
| Delivery | Docker Compose, GitHub Actions, AWS |

Versions belong in Maven or npm configuration, not in documentation.

## Repository layout

```text
backend/
  copilot-api/              Main workflow application
  operations-mcp-server/    Synthetic operational tools
frontend/
  operator-console/         Angular application boundary
docs/agent/                 Canonical project and agent context
infra/                      Deployment infrastructure when selected
.github/workflows/          Continuous integration
```

## Local development

Prerequisites:

- Java 21
- Maven 3.9+
- Docker with Docker Compose
- Node.js when the Angular workspace is introduced

Create local configuration:

```bash
cp .env.example .env
```

Start PostgreSQL/pgvector:

```bash
docker compose up -d postgres
docker compose ps
```

Build both Java services:

```bash
mvn clean verify
```

Run each service in a separate terminal:

```bash
mvn -pl backend/copilot-api -am spring-boot:run
mvn -pl backend/operations-mcp-server -am spring-boot:run
```

Default ports are `8080` for the copilot API, `8081` for the MCP server, and
`5432` for PostgreSQL.

The Angular workspace is intentionally deferred until the active task needs its
test harness. See [operator console setup](frontend/operator-console/README.md).

## Agent development

`AGENTS.md` is the repository-wide source of truth. It routes agents to the
minimum required project context, enforces test-driven development, and defines
completion evidence. Nested `AGENTS.md` files add only service-specific rules.

Start with the [agent context map](docs/agent/README.md). Keep durable facts in
their canonical files rather than copying them into prompts or task notes.

## Author

Christopher Guzowski
