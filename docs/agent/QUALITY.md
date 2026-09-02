# Quality and validation

Last reviewed: 2026-08-31

## Standard commands

The authoritative completion gate is:

```powershell
./verify.ps1
```

It verifies Java 21, Node.js 24.14.1, npm 10.8.3, the verification script's
PowerShell tests, the pinned Maven Wrapper build, zero skipped backend and
frontend tests, locked frontend installation, Prettier, the Angular production
build, Compose configuration, and `git diff --check`. CI delegates to this same
implementation.

Focused scopes are available during development:

```powershell
./verify.ps1 -Scope Backend
./verify.ps1 -Scope Frontend
./verify.ps1 -Scope Repository
```

Focused scopes do not replace the unscoped completion gate.

## Test-driven development

All production behavior changes follow a red-green-refactor cycle:

1. Translate the user story and each acceptance criterion into named test
   cases at the lowest suitable level.
2. Write one focused test for the next behavior and run it to confirm that it
   fails for the intended reason.
3. Implement only enough production code to make that test pass.
4. Refactor without changing behavior while keeping the tests green.
5. Repeat for the remaining criteria, then run the broader relevant suite.

For a defect, first add a failing regression test that reproduces it. Do not
weaken a valid test to accommodate an implementation. If an acceptance
criterion requires manual validation, record why it cannot be automated and
add the closest meaningful automated coverage.

Changes with no executable behavior, such as documentation-only edits, do not
require artificial tests. Run the relevant static or structural validation and
record that evidence instead.

## Model-provider testing

- Automated tests explicitly set `spring.ai.model.chat=none` and
  `spring.ai.model.embedding=none`.
- Tests at a model-facing boundary use mocked responses or deterministic
  doubles and cover malformed output, unavailability, and timeout behavior.
- Normal automated verification never depends on Ollama, Bedrock, AWS
  credentials, model downloads, or external network access.
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
- The completed deterministic end-to-end path remains green while the SynTen
  Inc PDF and live-model path is added
- Static corpus checks for manifest membership, source/PDF pairing, synthetic
  metadata, text extraction, rendering, and source-location provenance

## Observability expectations

- Use structured logs.
- Include correlation, incident, investigation, and tool-call identifiers.
- Never log credentials, prompts containing sensitive data, or full model
  payloads indiscriminately.
- Expose health and readiness information suitable for container deployment.

## Definition of done

- Acceptance criteria are demonstrably satisfied.
- Each acceptance criterion is mapped to automated coverage or a documented
  manual-verification reason.
- Red-phase and green-phase evidence is recorded for behavior changes.
- Relevant automated tests pass.
- Failure and invalid-input behavior is intentional.
- Database changes use migrations.
- Public API changes are documented.
- Audit-impacting behavior is tested.
- Documentation reflects the resulting system.
- The final diff contains no secrets, generated build output, or unrelated
  refactoring.
