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
-> the configured Spring AI chat model proposes an evidence-linked report
-> operator approves or rejects
-> the final decision and complete projected audit timeline remain reviewable
```

## Components

| Component | Responsibility |
|---|---|
| `frontend/operator-console` | Active incident triage, investigation review, and human decisions |
| `backend/copilot-api` | Workflow, persistence, retrieval, report generation, and audit history |
| `backend/operations-mcp-server` | Deterministic synthetic evidence exposed through read-only MCP tools |
| PostgreSQL/pgvector | Application state, approved knowledge, and vector retrieval |
| Ollama | Local `nomic-embed-text` embeddings for live approved-knowledge retrieval |
| Amazon Bedrock | Optional production provider profile deferred until deployment work |

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
| AI and integration | Spring AI, Ollama locally, MCP; optional Bedrock production profile deferred |
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
- Ollama with `nomic-embed-text` for live approved-knowledge retrieval

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
waits for all three to be ready in dependency order, then starts the synthetic
incident generator. It opens the operator console at `http://localhost:4200`
and the generator at `http://localhost:8082`. Press `Ctrl+C` in each service
terminal, or close the terminals, to stop the application.

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
Flyway applies V1 through V9 when the API starts. V8 adds append-only final
human decisions plus nullable historical attribution for evidence and retrieval
attempts; V9 adds the page-aware PDF knowledge catalog and retrieval provenance.
New attempts always persist their requesting operator.

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
`X-Synthetic-Operator-Id`; investigation start, evidence collection, knowledge
retrieval, report generation, and final human decisions all require it.
Resource identifiers remain in paths, queue reads use `GET /api/incidents`,
and tenant/operator identity is not accepted in resource paths, query
parameters, or request bodies. These caller-supplied synthetic headers are a
local portfolio convention, not authentication or a production authorization
claim.

### Local Ollama models and approved-knowledge ingestion

Install Ollama outside the repository, then make the pinned local embedding
model available before invoking live approved-knowledge retrieval:

```powershell
ollama pull nomic-embed-text
ollama serve
```

The API uses Ollama at `http://localhost:11434`. It never pulls models
automatically. Normal startup does not invoke a model or mutate the knowledge
index, and no AWS credential is required for local development.

Each Generate action makes one auditable provider call. Report generation has
a two-minute total deadline so provider retries or a stalled model cannot leave
the operator console busy indefinitely. A refused provider is recorded as
`UNAVAILABLE`; exceeding the deadline is recorded as `TIMED_OUT`, and either
outcome can be retried explicitly. Slower local hardware can set the positive
Spring duration `REPORT_GENERATION_TIMEOUT` (for example `3m`) in `.env`.

After loading `.env` and pointing the API at a running PostgreSQL/pgvector
database, run the safe model-contract smoke test as a one-shot application:

```powershell
$env:APP_KNOWLEDGE_EMBEDDING_SMOKE_TEST_ENABLED = 'true'
Push-Location backend/copilot-api
../../mvnw.cmd "-Dspring-boot.run.arguments=--spring.main.web-application-type=none" spring-boot:run
Pop-Location
```

The smoke test sends one fixed synthetic string and reports only the model ID,
dimension count, and normalization result. To explicitly ingest the two
repository-owned approved Markdown sources, use the same command with
`APP_KNOWLEDGE_EMBEDDING_SMOKE_TEST_ENABLED=false` and
`APP_KNOWLEDGE_INGESTION_ENABLED=true`.

#### SynTen PDF embedding and retrieval evaluation

K4 live work must use a fresh database named `payment_copilot_k4_eval` or a
name beginning with `payment_copilot_k4_eval_`. Set `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` to that database,
and set `SYNTEN_CORPUS_ROOT` to the absolute `SynTen Inc/corpus` directory.
Never point the K4 runner at a retained development or production database.

First run the explicit, model-free PDF catalog command as a one-shot
application. It applies Flyway V1-V9 and imports exactly 30 PDF document
versions and 705 stable page-aware chunks; normal startup never runs it.

```powershell
$env:APP_KNOWLEDGE_PDF_CATALOG_ENABLED = 'true'
$env:SPRING_AI_MODEL_CHAT = 'none'
$env:SPRING_AI_MODEL_EMBEDDING = 'none'
Push-Location backend/copilot-api
../../mvnw.cmd "-Dspring-boot.run.arguments=--spring.main.web-application-type=none" spring-boot:run
Pop-Location
$env:APP_KNOWLEDGE_PDF_CATALOG_ENABLED = 'false'
```

After the `nomic-embed-text` smoke test passes, run the all-or-none embedding
backfill. The command validates the complete persisted catalog, prepares and
validates all 705 normalized 768-dimensional vectors in memory, and only then
writes them in one transaction. It makes no chat-model call, retries no model
call, and a same-model repeat is an exact no-op.

```powershell
$env:SPRING_AI_MODEL_CHAT = 'none'
$env:SPRING_AI_MODEL_EMBEDDING = 'ollama'
$env:APP_KNOWLEDGE_PDF_BACKFILL_ENABLED = 'true'
Push-Location backend/copilot-api
../../mvnw.cmd "-Dspring-boot.run.arguments=--spring.main.web-application-type=none" spring-boot:run
Pop-Location
$env:APP_KNOWLEDGE_PDF_BACKFILL_ENABLED = 'false'
```

The default per-call timeout is 30 seconds and the whole-operation deadline is
eight hours. Override them only with positive Spring durations in
`SYNTEN_PDF_EMBEDDING_CALL_TIMEOUT` and
`SYNTEN_PDF_EMBEDDING_OPERATION_TIMEOUT`. Provider unavailability, timeout,
malformed vectors, catalog drift, a partial tuple, or conflicting model
metadata aborts without a partial write. Audit and restore an explicitly valid
all-absent or all-complete state before retrying; the command never repairs or
overwrites a conflicting state.

For retrieval evaluation, start the standalone synthetic generator and the
copilot API against the dedicated database with every knowledge command
disabled. The API must use the generator endpoint through
`OPERATIONS_MCP_BASE_URL`. Then seed and verify all 36 reviewed scenarios plus
the KQ-023 exclusion probe through public HTTP boundaries:

```powershell
$env:SYNTEN_EVALUATION_DATABASE_NAME = 'payment_copilot_k4_eval'
$env:OPERATIONS_MCP_BASE_URL = 'http://127.0.0.1:8082'
./scripts/evaluation/run-synten-retrieval-evaluation-v1.ps1 `
  -CopilotApiBaseUrl http://127.0.0.1:8080 `
  -GeneratorMcpBaseUrl http://127.0.0.1:8082
```

The runner preflights both health endpoints and the process
`SPRING_DATASOURCE_URL`, persists every incident/investigation/evidence chain,
reads it back, and writes a non-overwriting seed manifest under the operating
system temporary directory. Copy the printed manifest path into
`SYNTEN_RETRIEVAL_EVALUATION_SEED_MANIFEST`, stop the API process used for
seeding, and run the evaluator as a one-shot application:

```powershell
$env:SYNTEN_RETRIEVAL_CASES_PATH = (Resolve-Path 'SynTen Inc/evaluation/retrieval-cases.md').Path
$env:SYNTEN_SCENARIO_CATALOG_PATH = (Resolve-Path 'syntheticIncidentGenerator/src/main/resources/scenarios/catalog.json').Path
$env:SYNTEN_RETRIEVAL_EVALUATION_RESULTS_DIR = (Join-Path (Resolve-Path 'SynTen Inc/evaluation').Path 'results')
$env:APP_KNOWLEDGE_RETRIEVAL_EVALUATION_ENABLED = 'true'
$env:SPRING_AI_MODEL_CHAT = 'none'
$env:SPRING_AI_MODEL_EMBEDDING = 'ollama'
Push-Location backend/copilot-api
../../mvnw.cmd "-Dspring-boot.run.arguments=--spring.main.web-application-type=none" spring-boot:run
Pop-Location
```

The evaluator uses the same query, embedding fallback, PostgreSQL hybrid
search, RRF ordering, and runbook/policy selector as product retrieval. It
publishes one bounded, deterministically ordered JSON result under
`SynTen Inc/evaluation/results` using an atomic move and never overwrites a
prior run. A threshold miss intentionally exits nonzero only after preserving
the complete `FAIL` artifact and naming the failed case/variant pairs. Do not
change labels, ranking constants, source eligibility, or the artifact by hand;
review the recorded ranks and open a separately approved follow-up.

K5 uses `knowledge-query/v2` and `postgres-hybrid-rrf/v2`: applicable evidence
queries emphasize the incident description, service, status, and exact error
counts; lexical and vector depth is applied separately to RUNBOOK and POLICY;
and final context prefers distinct document versions before repeated chunks.
The fixed live rerun is retained as
`375ebc04ba894e84b2d18aeb6bc4d3cb-FAIL.json`. It improved primary-runbook
coverage from 9/22 to 19/22 and required-policy coverage from 1/20 to 12/20,
kept primary-over-weak coverage at 16/21, returned zero ineligible candidates,
and preserved partial, unavailable, and superseded-source semantics. It remains
a factual FAIL because the original thresholds were not weakened.

For the accepted S001 proof, open its investigation in the operator console and
click the existing **Retrieve approved knowledge** button. A successful K5 run
shows an `AVAILABLE` attempt using `nomic-embed-text` and
`postgres-hybrid-rrf/v2`, with RB-002 first and its PDF filename, SHA-256,
page/block location, identifiers, approval actor, and effective time. This is a
retrieval display only; it does not generate or approve a report.

The current K5 live path deliberately runs with `SPRING_AI_MODEL_CHAT=none` and
`SPRING_AI_MODEL_EMBEDDING=ollama`: `nomic-embed-text` supplies retrieval
embeddings but cannot generate report text. Selecting and evaluating a live
chat model is a separate future decision. The existing report adapter and its
deterministic tests remain available without making a chat model a K5
prerequisite.

`nomic-embed-text` uses the current 768-dimensional index contract. Flyway V7
preserves historical 1,024-dimensional Titan rows, and vector scoring compares
only matching model/dimension pairs; lexical retrieval can still return older
approved chunks. Re-importing an unchanged document version is idempotent, so
an existing Titan-indexed local database is not silently re-embedded. For full
local semantic coverage, use a fresh synthetic database or publish a new
document version and run the explicit importer. Do not delete or rewrite an
index without reviewing its retained audit history.

Flyway V6 is the immutable, provider-neutral report-persistence migration from
the preserved production branch. Keeping its original checksum allows the same
local database to move between that history and this Ollama branch without
`flyway repair`; V7 contains the Ollama vector change.

Automated tests set both Spring AI providers to `none` and use mocked or
deterministic model responses, so neither Ollama nor Bedrock is contacted by
the test suite. Bedrock remains a future optional production profile to be
designed near the deployment milestone.

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
