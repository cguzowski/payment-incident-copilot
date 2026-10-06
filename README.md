# Payment Incident Investigation Copilot

A local demo that investigates synthetic payment incidents using sourced evidence,
approved PDF guidance and Ollama. Reports are advisory; a human approves or rejects
with a reason. The application never processes payments or executes recommendations.
All organizations and records are fictional. Synthetic identity headers are not
production authentication.

## Run from a Git clone on Windows

Install Java 21, Node.js 24.14.1 with npm 10.8.3, PowerShell 7, Docker Desktop
(or a native PostgreSQL installation with pgvector), and Ollama. Make `java`,
`node`, `npm`, and `pwsh` available on PATH. Maven is installed by the included wrapper.
Internet access is needed for the first dependency build and model downloads.

```powershell
git clone https://github.com/cguzowski/payment-incident-copilot.git
cd payment-incident-copilot
Copy-Item .env.example .env
```

Edit `.env` for your database. For a new Docker database, keep its `POSTGRES_*`
settings consistent with `SPRING_DATASOURCE_*`, then start Docker Desktop and run:

```powershell
docker compose up -d postgres
ollama pull nomic-embed-text
ollama pull qwen3:8b-q4_K_M
.\start-local.bat --CheckOnly
.\start-local.bat -PrepareKnowledge
```

Ollama must be running. Knowledge preparation explicitly imports and embeds the
30-document authorization catalog and 16-document payment library; the first run
can take substantial time. Existing compatible catalogs are reused. For native
PostgreSQL, configure `.env` for that database and omit Docker startup. Do not start
a second database on an occupied port. Changing `.env` does not reset Docker volumes.

For subsequent runs:

```powershell
.\start-local.bat
```

The launcher exports and builds three independent systems under the ignored
`.migration-workspaces/independent-v1/` directory, installs console dependencies,
checks knowledge readiness, and opens four owning terminals. Close a terminal to
stop that service. Rerunning startup reuses matching services still running.

| Surface | Address |
|---|---|
| Operator console | http://localhost:4200 |
| Investigation API | http://localhost:8080 |
| Synthetic generator and MCP evidence | http://localhost:8082 |
| Evaluator | http://127.0.0.1:8083 |

Use the generator's red button, investigate in the console, and record an explicit
human decision. The generator screen can then reveal the synthetic answer and
request advisory report/decision scores from the evaluator.

## What is included

Only the current systems' source/build inputs, startup scripts, public contract
pins, configuration templates, and required knowledge assets are maintained.
Tests, evaluation archives, migration experiments, backup copies and obsolete
launchers are removed. PDF sources and manifests are required for validated
knowledge import. Historical PDF versions remain available for existing citations.
The source and evaluator share source inputs through explicit hash-pinned export
lists, while their exported applications build and run independently.

Java dependencies, `node_modules`, generated workspaces, build output, `.env`,
logs, local comparison results, database state and Ollama models are excluded from
Git. A fresh clone downloads dependencies and models separately. Runtime disk
usage will therefore be larger than the source checkout. To reclaim generated
files, first close all four service terminals, then delete `.migration-workspaces`
and any `target`, `node_modules`, `.angular`, `dist` or `tmp` directories. They are
recreated when needed. Back up comparison results if you want to retain them.

Reports have a maximum 150-second generation deadline. GPU/model throughput can
still cause timeouts or malformed output. Scores and schema-valid reports do not
prove correctness. Knowledge and report quality have known limitations; this is
a synthetic local demonstration, without production authentication or deployment.
Keep the portable GPU defaults in `.env.example` unless tuning your own hardware.
