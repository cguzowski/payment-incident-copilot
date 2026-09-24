# Quality and validation

Last reviewed: 2026-09-24

## Standard commands

The authoritative completion gate is:

```powershell
./verify.ps1
```

It verifies Java 21, Node.js 24.14.1, npm 10.8.3, the verification script's
PowerShell tests, the pinned Maven Wrapper build, zero skipped backend and
frontend tests, standalone generator tests, locked frontend installation,
Prettier, the Angular production build, Compose configuration, and
`git diff --check`. CI delegates to this same implementation.

Focused scopes are available during development:

```powershell
./verify.ps1 -Scope Backend
./verify.ps1 -Scope Frontend
./verify.ps1 -Scope Repository
```

For executable changes, focused scopes do not replace the unscoped completion
gate. Documentation-only changes use relevant structural/static checks and the
Repository scope; they do not require a new full application test run.

The gate requires Docker and may download Maven/npm dependencies and container
images when absent from local caches. It has no failing npm-audit step; advisory
output from installation is not a clean security assessment.

## Test-driven development

Follow the red-green-refactor and acceptance-mapping requirements in
[AGENTS.md](../../AGENTS.md). Record focused failures, passing coverage, and
manual-verification exceptions in the task's completion evidence.

Changes with no executable behavior, such as documentation-only edits, do not
require artificial tests. Run the relevant static or structural validation and
record that evidence instead.

## Model-provider testing

- Automated tests explicitly set `spring.ai.model.chat=none` and
  `spring.ai.model.embedding=none`.
- Tests at a model-facing boundary use mocked responses or deterministic
  doubles and cover malformed output, unavailability, and timeout behavior.
- Automated tests never require live Ollama/Bedrock, AWS credentials, or model
  downloads. Dependency installation and container provisioning may need network
  access; this is distinct from deterministic model-provider tests.
- Live Ollama smoke checks are explicit local-development checks and do not
  replace the deterministic completion gate.
- Live corpus evaluations record the exact source-corpus version, extraction
  and chunking strategy versions, embedding and chat model identifiers, index
  version, test query, expected sources, actual selected sources, and observed
  limitations.

## PDF catalog testing

- PDF parser tests use repository-owned synthetic fixtures and never require a
  network call, live embedding model, or chat model.
- Contract coverage includes exact source/PDF hashes, 1-15 page bounds,
  encryption and malformed-input rejection, empty/scanned-only rejection,
  exact generated header/footer removal, retained superseded banners, ordered
  table text, and repeatable page/block output.
- Chunker coverage proves page confinement, section carry-forward,
  deterministic order and IDs, the 400/600/50 token contract, short-tail
  behavior, and exact PDF locators.
- PostgreSQL coverage proves atomic manifest import, nullable all-or-none
  embedding tuples, approved lexical eligibility, superseded exclusion, tenant
  isolation, and immutable retrieval snapshots carrying PDF provenance.
- Operator-console coverage displays PDF filename, SHA-256, page, and block
  range while preserving line locators for historical Markdown results.

## Coverage expectations

- Unit tests for domain rules and state transitions
- Integration tests for HTTP contracts, PostgreSQL, Flyway, and MCP boundaries
- Contract tests for report schemas and MCP tool schemas
- Repeatable synthetic scenarios for demonstrations
- Failure tests for unavailable sources, incomplete evidence, invalid model
  output, duplicate alerts, and rejected reports
- Preserve the deterministic end-to-end path and PDF/live-model integration
  boundaries without requiring a live provider in tests
- Static corpus checks for manifest membership, source/PDF pairing, synthetic
  metadata, text extraction, rendering, and source-location provenance

## Observability expectations

- Use structured logs.
- Include correlation, incident, investigation, and tool-call identifiers.
- Never log credentials, prompts containing sensitive data, or full model
  payloads indiscriminately.
- Expose health and readiness information suitable for container deployment.

## Definition of done

Use the completion standard in [AGENTS.md](../../AGENTS.md). In addition,
database changes use migrations, public API changes are documented, and
audit-impacting behavior has regression coverage. Record exact commands and
remaining risks when a required check cannot run.
