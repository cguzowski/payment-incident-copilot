# Payment Incident Investigation Copilot

An auditable copilot for investigating synthetic payment incidents:

**alert → incident work queue → evidence → approved knowledge → advisory report
→ human decision → audit timeline**

The core MVP is implemented, and a live local S001 report has been demonstrated.
The retrieval benchmark still fails all three quality thresholds; one complete
live-model terminal-decision proof and broader evaluation remain outstanding.
See [current status](docs/agent/STATUS.md) and the
[roadmap](docs/agent/ROADMAP.md).

## What it does

- Keeps active and completed incidents in one tenant-scoped operator queue.
- Collects read-only synthetic service-error evidence through MCP.
- Retrieves approved Markdown/PDF knowledge with immutable source provenance.
- Generates schema- and citation-validated reports through local Ollama.
- Requires an attributable human approval or rejection with a reason.
- Preserves attempt outcomes, missing evidence, and the audit timeline.

All organizations and data are fictional. The application does not process
payments, move money, or execute recommendations. Caller-supplied synthetic
identity headers are not authentication.

## Runtime boundaries

| Component | Responsibility |
|---|---|
| `frontend/operator-console` | Angular queue, investigation, reports, decisions, and audit UI |
| `backend/copilot-api` | Java 21/Spring Boot workflow, persistence, Spring AI, and retrieval |
| `syntheticIncidentGenerator` | Standalone alert generator and matching MCP evidence on port 8082 |
| `backend/operations-mcp-server` | Legacy fixture provider and MCP compatibility checks on port 8081 |
| PostgreSQL + pgvector | Application state and tenant-filtered hybrid knowledge retrieval |
| Ollama | Local embeddings and advisory report generation |

The launcher uses the generator as the API's evidence provider. The legacy
provider remains independently runnable. See
[architecture](docs/agent/ARCHITECTURE.md) for feature ownership and data flow.

The [SynTen Inc corpus](SynTen%20Inc/README.md) contains 30 PDF versions and
705 page-aware chunks. Its README owns the recorded benchmark results and
their evidence limitations.

## Run locally on Windows

Prerequisites:

- Java 21 and system Maven 3.9+ (`mvn.cmd` must be on PATH for the launcher).
- Node.js 24.14.1 and npm 10.8.3.
- Windows PowerShell for the batch launcher; PowerShell 7 for the documented
  verification workflow.
- Running PostgreSQL with pgvector, either native or through Docker Compose.
- Ollama with `nomic-embed-text` and `qwen3:8b-q4_K_M` installed.

Create configuration once, then edit the ignored file for your local database:

```powershell
Copy-Item .env.example .env
```

Keep database URL, username, password, and port consistent. For a new
Compose-managed database, start the required infrastructure with:

```powershell
docker compose up -d postgres
```

Do not start another database on a port already used by native PostgreSQL.
Changing Compose environment values does not reset an existing database volume.

Install the models explicitly, and ensure Ollama is running. Start
`ollama serve` only if the desktop application/service is not already serving:

```powershell
ollama pull nomic-embed-text
ollama pull qwen3:8b-q4_K_M
```

Check local prerequisites:

```powershell
.\start-local.bat --CheckOnly
```

For the first run against a new database, prepare the knowledge catalog and
embeddings before the application starts:

```powershell
.\start-local.bat -PrepareKnowledge
```

This imports the validated PDF catalog, then calls the local embedding model
for the catalog's 705 chunks. It can take substantial time. Exact complete
reruns are no-ops; incompatible or partial catalog/embedding state fails closed
rather than being silently overwritten. It never downloads models.

For subsequent starts:

```powershell
.\start-local.bat
```

Normal startup does not import knowledge. The batch launcher starts or reuses
the generator first, then checks Ollama, database reachability, and other
prerequisites before starting the API and console. A failed preflight may
leave the generator running. The launcher loads `.env` and selects the
generator's MCP endpoint explicitly.

| Surface | URL |
|---|---|
| Operator console | http://localhost:4200 |
| Copilot API | http://localhost:8080 |
| Generator UI and MCP evidence | http://localhost:8082 |

Use the generator's red button to create an incident. Its answer key is
collapsed in the browser, not isolated from the reviewer; see
[evaluation limitations](docs/agent/STATUS.md).

## Independent development and verification

The generator is outside the root Maven reactor. Each component remains
independently buildable:

```powershell
.\mvnw.cmd -pl backend/copilot-api -am clean verify
.\mvnw.cmd -pl backend/operations-mcp-server -am clean verify
.\mvnw.cmd -f syntheticIncidentGenerator/pom.xml clean verify
```

For the console, run `npm ci` and `npm start` from
`frontend/operator-console`; its development proxy forwards `/api` to port
8080. See the [console README](frontend/operator-console/README.md) and
[generator README](syntheticIncidentGenerator/README.md).

The authoritative repository completion gate is:

```powershell
.\verify.ps1
```

[QUALITY.md](docs/agent/QUALITY.md) documents scopes, prerequisites, and the
documentation-only exception. Tests use deterministic model doubles; dependency
installation and container images may still require network access.

## Engineering documentation

- [PROJECT.md](docs/agent/PROJECT.md): durable product scope and non-goals.
- [STATUS.md](docs/agent/STATUS.md): current facts, verification, and limitations.
- [ROADMAP.md](docs/agent/ROADMAP.md): ordered future outcomes.
- [ARCHITECTURE.md](docs/agent/ARCHITECTURE.md): ownership, lifecycle, and data flow.
- [Agent context map](docs/agent/README.md): rules and canonical documentation.
- [Current task](docs/agent/tasks/current.md): latest authorized task contract.
