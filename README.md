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
-> incident work queue
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
| `frontend/operator-console` | Active incident triage, investigation review, and human decisions |
| `backend/copilot-api` | Workflow, persistence, retrieval, report generation, and audit history |
| `backend/operations-mcp-server` | Deterministic synthetic evidence exposed through read-only MCP tools |
| PostgreSQL/pgvector | Application state, approved knowledge, and vector retrieval |
| Amazon Bedrock | Titan V2 knowledge embeddings and Nova 2 Lite evidence-linked report generation |

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
- Node.js 24.14.1 and npm 10.8.3
- PowerShell 7 on non-Windows hosts
- Either native PostgreSQL 18 with pgvector or Docker with Docker Compose

Create ignored local configuration from the safe placeholders:

```powershell
Copy-Item .env.example .env
```

Put real local values only in `.env`. Never put them in `.env.example`.
Git ignores `.env`, but verify with `git check-ignore .env` before adding new
local variables.

On Windows, after configuring `.env` and the database, double-click
`start-local.bat` in the repository root. The launcher checks the local tools
and database, installs locked frontend dependencies when needed, starts the
operations MCP server, copilot API, and operator console in separate terminals,
waits for all three to be ready in dependency order, and opens
`http://localhost:4200`. Press `Ctrl+C` in each service terminal, or close the
terminals, to stop the application.

Run `start-local.bat --CheckOnly` from a terminal to perform the startup
preflight without starting any service.

### Native PostgreSQL 18 on port 5432

Create a dedicated database and non-superuser login matching `.env`. Install
pgvector for PostgreSQL 18 by following the
[official Windows instructions](https://github.com/pgvector/pgvector#installation-notes---windows),
then enable `vector` once in the project database as a database administrator.
Do not put a password directly in SQL or shell history; use the interactive
`psql` password prompt when provisioning the local role.

Spring Boot does not load `.env` automatically. From the repository root, load
the file into the current PowerShell process without printing its values, then
start the API:

```powershell
$projectEnv = Resolve-Path .env
Get-Content -LiteralPath $projectEnv | ForEach-Object {
  if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') {
    [Environment]::SetEnvironmentVariable(
      $matches[1],
      $matches[2].Trim().Trim('"').Trim("'"),
      'Process'
    )
  }
}
Push-Location backend/copilot-api
../../mvnw.cmd spring-boot:run
Pop-Location
```

The current native verification target is PostgreSQL 18 on `localhost:5432`.
Flyway applies V1 through V6 when the API starts.

### Docker PostgreSQL 17.11 on port 5433

The repeatable Docker baseline remains `pgvector/pgvector:pg17`, currently
verified as PostgreSQL 17.11. When native PostgreSQL owns 5432, override only
the Docker host port:

```powershell
$env:POSTGRES_PORT = '5433'
docker compose up -d postgres
docker compose ps
docker compose port postgres 5432
```

After loading `.env` as shown above, point a backend process at Docker without
changing tracked configuration:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5433/payment_copilot'
Push-Location backend/copilot-api
../../mvnw.cmd spring-boot:run
Pop-Location
```

Compose reads `.env` automatically; Spring Boot does not. PostgreSQL container
initialization variables apply only when a data volume is first created, so a
preserved volume must be used with the credentials that initialized it.

Run the authoritative repository verification from the root:

```powershell
./verify.ps1
```

It uses the pinned Maven Wrapper, locked frontend installation, backend and
frontend zero-skip checks, formatting, the Angular production build, Compose
validation, and diff-integrity checks. `-Scope Backend`, `-Scope Frontend`, and
`-Scope Repository` are available for focused work; the unscoped command is the
completion gate and is also used by CI.

Build both Java services directly when iterating on backend code. Use
`./mvnw.cmd` on Windows PowerShell and `sh ./mvnw` on non-Windows PowerShell:

```powershell
./mvnw.cmd clean verify
```

Run each service in a separate terminal after loading the required environment:

```powershell
./mvnw.cmd -pl backend/operations-mcp-server -am spring-boot:run
./mvnw.cmd -pl backend/copilot-api -am spring-boot:run
```

Default application ports are `8080` for the copilot API and `8081` for the MCP
server. Native PostgreSQL uses `5432`; the Docker baseline uses `5433` when both
servers coexist.

### Synthetic HTTP request context

Application HTTP calls carry the demonstration tenant in the required
`X-Synthetic-Tenant-Id` header. Operator-attributed mutations carry
`X-Synthetic-Operator-Id`; investigation start and explicit report generation
are the current operator-attributed mutations.
Resource identifiers remain in paths, queue reads use `GET /api/incidents`,
and tenant/operator identity is not accepted in resource paths, query
parameters, or request bodies. These caller-supplied synthetic headers are a
local portfolio convention, not authentication or a production authorization
claim.

### Approved-knowledge ingestion and Bedrock smoke test

Normal startup does not call Bedrock or mutate the knowledge index. In an
authorized AWS environment, use the default AWS credential chain and enable
Titan V2 explicitly; never add credentials to `.env` or tracked files.

After loading `.env` and pointing the API at a running PostgreSQL/pgvector
database, run the safe model-contract smoke test as a one-shot application:

```powershell
$env:AI_MODEL_EMBEDDING = 'bedrock-titan'
$env:APP_KNOWLEDGE_EMBEDDING_SMOKE_TEST_ENABLED = 'true'
Push-Location backend/copilot-api
../../mvnw.cmd "-Dspring-boot.run.arguments=--spring.main.web-application-type=none" spring-boot:run
Pop-Location
```

The smoke test sends one fixed synthetic string and reports only the model ID,
dimension count, and normalization result. To explicitly ingest the two
repository-owned approved Markdown sources, use the same command with
`APP_KNOWLEDGE_EMBEDDING_SMOKE_TEST_ENABLED=false` and
`APP_KNOWLEDGE_INGESTION_ENABLED=true`. Re-importing unchanged sources is
idempotent; changed content or embedding metadata requires a new document
version.

Normal startup also keeps chat calls disabled. In an authorized AWS environment,
run the one-shot report contract smoke after loading `.env` and pointing the API
at PostgreSQL:

```powershell
$env:AI_MODEL_CHAT = 'bedrock-converse'
$env:BEDROCK_CHAT_MODEL = 'global.amazon.nova-2-lite-v1:0'
$env:APP_REPORT_SMOKE_TEST_ENABLED = 'true'
Push-Location backend/copilot-api
../../mvnw.cmd "-Dspring-boot.run.arguments=--spring.main.web-application-type=none" spring-boot:run
Pop-Location
```

The report smoke sends one bounded synthetic incident/evidence snapshot, asks
for prompt-guided `report-v1` JSON, and applies the same strict schema and
citation validation as the HTTP workflow. It logs only the model ID, schema
version, disposition, and validation result—not the prompt, source content,
model payload, credentials, or provider error details.

Run the Angular operator console in another terminal:

```bash
cd frontend/operator-console
npm ci
npm start
```

The development server proxies `/api` requests to the copilot API on port
`8080`. See the [operator console guide](frontend/operator-console/README.md)
for its test and production-build commands.

## Agent development

`AGENTS.md` is the repository-wide source of truth. It routes agents to the
minimum required project context, enforces test-driven development, and defines
completion evidence. Nested `AGENTS.md` files add only service-specific rules.

Start with the [agent context map](docs/agent/README.md). Keep durable facts in
their canonical files rather than copying them into prompts or task notes.

## Author

Christopher Guzowski
