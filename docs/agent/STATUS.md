# Project status

Last updated: 2026-08-31

## Current milestone

Milestone 2 — Establish the SynTen Inc knowledge corpus and live AI path.

## Completed

- Selected the Payment Incident Investigation Copilot vertical slice.
- Selected Java, Spring Boot, Spring AI, Angular, AWS, PostgreSQL, and pgvector.
- Defined separate copilot API and synthetic MCP server boundaries.
- Created the initial monorepo and coding-agent scaffold.
- Confirmed the Maven reactor builds locally.
- Confirmed the latest pushed commit passes GitHub Actions CI.
- Selected the payment authorization decline-rate spike as the first incident
  family.
- Implemented the synthetic alert intake, idempotent incident persistence,
  tenant-scoped queue API, and operator queue in the working tree.
- Verified backend unit and HTTP tests plus frontend tests, build, formatting,
  and responsive visual states.
- Verified all PostgreSQL/Testcontainers tests with Docker, Flyway migrations
  V1 and V2 against PostgreSQL 17.11, and the Docker Compose configuration and
  database health check.
- Manually submitted the same synthetic alert twice, confirmed one persisted
  incident, and verified the tenant-scoped queue projection and cross-tenant
  exclusion.
- Secured native database credentials in ignored `.env` and restored safe
  `.env.example` placeholders; no committed revision contains the local
  password.
- Verified the application against native PostgreSQL 18.3 on port 5432 with
  pgvector 0.8.6, Flyway V1/V2, persistence, atomic idempotency, tenant queue
  isolation, and the live Angular proxy path.
- Re-ran the full Maven reactor with Docker PostgreSQL 17.11 Testcontainers and
  the frontend test and production-build suites.
- Implemented and verified the tenant-scoped, read-only incident-detail API and
  Angular detail route, including safe not-found behavior, responsive states,
  and queue/back router navigation.
- Added a Windows local-development launcher that safely loads the ignored
  `.env`, checks prerequisites, and starts the operations MCP server, API, and
  operator console in dependency order.
- Selected one tenant-scoped incident work queue that retains active incidents
  across status changes instead of separate alert and investigation queues.
- Implemented the tenant-scoped incident work queue for `NEW` and
  `INVESTIGATING` incidents with no age cutoff, five client-side sort modes,
  sort-preserving refresh, and start/resume navigation.
- Added Flyway V3, tenant-safe one-investigation-per-incident persistence,
  atomic/idempotent investigation creation, structured errors, and the minimal
  read-only investigation workspace.
- Verified the completed slice through unit, HTTP, PostgreSQL/Testcontainers,
  concurrency, Angular, build, formatting, and responsive browser checks.
- Polished the operator workflow so queue ordering is explained by visible
  Received and Detected timestamps, controls share intentional accessible
  styling, status/action layouts are compact, and desktop spacing is denser.
- Verified the polished queue, detail, and workspace at desktop and 390 CSS
  pixels, and relabeled the two local synthetic verification records used for
  review with plausible payment-operations wording.
- Implemented the first deterministic read-only MCP evidence slice with
  `getRecentServiceErrors`, tenant-safe append-only persistence, explicit
  collection/history APIs, and an observed-evidence workspace that remains
  separate from future AI inference.
- Verified every evidence availability outcome, MCP discovery and invocation,
  Flyway V4, transaction boundaries, tenant isolation, retry history, the
  three-service local workflow, and desktop/390-pixel UI regressions.
- Implemented the approved operational-knowledge slice: explicit versioned
  Markdown ingestion, validated Titan V2 embedding requests, Flyway V5,
  tenant-safe exact hybrid PostgreSQL retrieval with RRF, persisted retrieval
  snapshots/history, and a responsive approved-knowledge workspace.
- Verified the complete deterministic slice through unit, HTTP, PostgreSQL/
  pgvector integration, transaction-boundary, Angular, build, formatting,
  Compose, scope, secret, and desktop/390-pixel browser checks.
- Implemented the B01-B06 codebase boundaries: enforced copilot feature
  ownership, tenant-scoped incident/evidence snapshot ports, composed Angular
  investigation panels, centralized synthetic HTTP request context, one
  immutable MCP v1 contract artifact, and a shared local/CI verification gate.
- Pinned Node.js 24.14.1, npm 10.8.3, Maven Wrapper 3.9.16, Spotless 3.9.0,
  and Palantir Java Format 2.96.0; CI delegates its backend, frontend, and
  repository checks to the root verification implementation.
- Completed B01-B06 acceptance after the Windows restart: the authoritative
  aggregate gate passed every backend, frontend, formatting, build, Compose,
  and diff check with zero skipped tests, followed by live desktop and
  390-CSS-pixel workspace verification.
- Accepted ADR-0006 with Nova 2 Lite as the original report-provider choice and
  `INSUFFICIENT_EVIDENCE` as a reviewable outcome that moves the incident to
  `AWAITING_REVIEW`; ADR-0007 now selects Ollama for active local development.
- Implemented P2 evidence-linked report generation with exact tenant-scoped
  evidence/knowledge snapshots, strict `report-v1` validation, append-only
  Flyway V6 persistence, explicit create/history APIs, atomic
  `AWAITING_REVIEW`, and an independently loading Angular report panel.
- Verified P2 through focused and unscoped backend, PostgreSQL, HTTP,
  architecture, Angular, formatting, build, Compose, diff, desktop, and
  390-CSS-pixel checks with zero skipped tests.
- Switched the active local Spring AI provider to Ollama with `qwen3.5:4b`
  report generation and normalized 768-dimensional `nomic-embed-text`
  embeddings while keeping automated tests network-free.
- Preserved report-persistence Flyway V6 and added Flyway V7 for compatible
  local embedding dimensions and model/dimension-filtered vector scoring.
- Bounded every report model invocation with a configurable two-minute total
  deadline, disabled hidden Spring AI retries inside one auditable attempt, and
  verified that the report panel recovers from every terminal response and HTTP
  error while preserving attempt history.
- Completed P3 human decisions, terminal lifecycle, projected audit timeline,
  operator attribution, and Active/Completed queue behavior. The authoritative
  gate passes every backend, PostgreSQL, HTTP, concurrency, architecture,
  frontend, formatting, build, Compose, and diff check with zero skipped tests.
- Added lifecycle-bound five-second polling to the incident work queue so new
  alerts appear without a manual refresh while visible queue results remain
  stable during background requests and transient failures.
- Consolidated Windows startup so the root launcher starts and opens the
  synthetic incident generator last, with no separate generator launcher.
- Completed the initial synthetic end-to-end vertical slice from alert intake
  through human decision and audit timeline. The owner accepted the recorded
  deterministic implementation and verification evidence on 2026-08-31.
- Defined `synten-auth-knowledge/v1`: one fictional tenant profile, an exact
  30-document inventory, a realistic PDF authoring standard, all 36 scenario
  mappings, all 49 error codes, and 23 retrieval-evaluation cases.
- Generated and validated 22 runbook PDFs and 8 policy PDFs with maintained
  Markdown authorities, stable SHA-256 hashes, 27 approved and 3 superseded
  versions, full-page visual review, and no document over 4 pages.

## In progress

- Selecting the PDF extraction, source-locator, and deterministic chunking
  contract before changing the existing knowledge-catalog ingestion path.

## Next

1. Accept a PDF extraction/chunking/provenance ADR, then implement the tested
   catalog path without changing retrieval or report ownership.
2. Ingest the 30-document corpus with stable PDF locators and deterministic,
   versioned chunks while preserving approval and tenant filters.
3. Install Ollama outside the repository, index the corpus with
   `nomic-embed-text`, measure hybrid retrieval, and run a cited report through
   `qwen3.5:4b` for human review.

## Blockers

- No blocker prevents the PDF extraction/chunking design or its deterministic
  implementation slice.
- Ollama and its pinned models are not installed in this task environment, so
  the later live embedding, retrieval, and report evaluation remains external.

## Known deliberate gaps

- No authentication yet.
- Only `getRecentServiceErrors` is implemented; additional evidence domains and
  operator-selected investigation areas remain future product tasks.
- The knowledge catalog still ingests only two Markdown documents. The SynTen
  Inc PDF corpus now exists, but no accepted PDF extraction/locator contract or
  application ingestion path exists yet.
- Live Ollama embedding and report behavior has not yet been verified on this
  machine; local and CI verification use mocked or deterministic model doubles.
- Historical Titan rows remain auditable and lexically retrievable but are not
  vector-compared with `nomic-embed-text` queries or silently re-embedded.
- Knowledge ingestion is intentionally explicit and disabled during normal
  startup; there is no continuous content-management pipeline yet.
- No AWS infrastructure selected yet.
- The investigation workspace intentionally does not repeat incident context;
  that design and any API evolution are deferred to a future task.

## Latest local verification

- 2026-08-27: `mvn clean verify` passed 39/39 tests with zero skips, including
  16 PostgreSQL 17.11 Testcontainers tests and Flyway V1-V3.
- 2026-08-27: Native PostgreSQL 18.3 also migrated through V3 and passed all 16
  PostgreSQL integration scenarios with zero skips.
- 2026-08-27: After `npm ci`, Angular tests passed 26/26; Prettier and the
  production build passed with zero npm audit vulnerabilities.
- 2026-08-27: Docker Compose configuration, `git diff --check`, the full live
  browser workflow, and 390-pixel responsive checks passed without horizontal
  page overflow or unexpected successful-path console output.
- 2026-08-28: Angular tests passed 30/30; Prettier, the warning-free production
  build, and `git diff --check` passed.
- 2026-08-28: Live desktop and 390-pixel queue/detail/workspace checks passed
  with both timestamps visible, compact status/action layouts, no horizontal
  overflow, and no browser warnings or errors.
- 2026-08-28: `mvn clean verify` passed 72/72 copilot API and operations MCP
  tests with zero skips, including PostgreSQL 17.11 Testcontainers and Flyway
  V1-V4. The MCP client started without the new sampling/elicitation warnings.
- 2026-08-28: After `npm ci`, Angular tests passed 38/38 with zero skips;
  Prettier and the production build passed, as did Docker Compose validation,
  launcher parsing/preflight, and `git diff --check`.
- 2026-08-28: Live MCP/API/PostgreSQL/console verification passed all seven
  evidence outcomes, append-only retry history, structured 400/404 boundaries,
  persisted provenance, queue/detail/resume/direct-refresh regressions, and
  desktop/390-pixel checks with no browser warnings or overflowing elements.
- 2026-08-28: `mvn clean verify` passed 97 copilot API and 9 operations MCP
  tests with zero failures, errors, or skips, including PostgreSQL 17.11,
  pgvector exact cosine search, Flyway V1-V5, filtered FTS/vector ranking, RRF,
  retrieval persistence, and the full retrieval API path.
- 2026-08-28: After a clean `npm ci` with zero audit vulnerabilities, Angular
  passed 46/46 tests; Prettier and the 299.28 kB production build passed.
- 2026-08-28: Docker Compose validation, `git diff --check`, changed-file
  sensitive-pattern review, and desktop/390-pixel approved-knowledge checks
  passed with one panel after observed evidence, no horizontal overflow or
  off-viewport actions, and no browser warnings or errors.
- 2026-08-29: `./verify.ps1 -Scope Repository` passed the PowerShell verification
  contract, Maven-wrapper portability regression, Docker Compose configuration,
  and `git diff --check`.
- 2026-08-29: The repository scope also passed under Windows PowerShell 5.1 and
  PowerShell Core after a regression exposed and removed a Core-only platform
  variable from wrapper selection.
- 2026-08-29: `./verify.ps1 -Scope Frontend` passed 45/45 Angular tests with
  zero skips, Prettier, and the 306.66 kB production build after `npm ci`
  reported zero vulnerabilities.
- 2026-08-29: `./verify.ps1` compiled both Java deployables, passed all 85
  non-PostgreSQL copilot tests and all 9 operations MCP tests, including two
  live random-port MCP contract tests and eight architecture rules. The gate
  then failed as designed because Docker was unavailable and 31 PostgreSQL
  tests were skipped; this is not recorded as a backend pass.
- 2026-08-29: After the Windows restart, `./verify.ps1` passed 116/116 copilot
  API tests and 9/9 operations MCP tests with zero failures, errors, or skips,
  including PostgreSQL 17.11 Testcontainers, Flyway V1-V5, tenant isolation,
  snapshot semantics, transaction boundaries, architecture rules, and live MCP
  contracts. It then passed 45/45 Angular tests with zero skips, npm audit,
  Spotless, Prettier, the 306.66 kB production build, Compose validation, and
  `git diff --check`.
- 2026-08-29: Live direct-route investigation verification at 1280x720 and
  390x844 rendered the investigation shell plus independently loaded evidence
  and approved-knowledge histories with no horizontal overflow, off-viewport
  interactive controls, browser warnings, or browser errors.
- 2026-08-29: Corrected the Ubuntu repository-job failure caused by constructing
  a synthetic Windows wrapper path through the host PowerShell drive provider.
  The strengthened nonexistent-drive regression passed in PowerShell Core and
  Windows PowerShell 5.1; repository verification passed, followed by 116/116
  copilot and 9/9 MCP-server backend tests with zero skips.
- 2026-08-29: The authoritative `./verify.ps1` gate passed 147/147 copilot API,
  9/9 operations MCP server, and 53/53 Angular tests with zero failures, errors,
  or skips. Flyway V1-V6, PostgreSQL 17.11 Testcontainers, ten architecture
  rules, Spotless, Prettier, the 324.41 kB production build, zero-vulnerability
  npm audit, Compose validation, and `git diff --check` all passed.
- 2026-08-29: A bounded synthetic P2 workspace rendered the report after
  evidence and approved knowledge at 1280x720 and 390x844 with adjacent source
  references, visible keyboard focus, no horizontal overflow or off-viewport
  controls, and no browser warnings or errors.
- 2026-08-30: Recovered the completed P2 report snapshot onto `main` and the
  active local Ollama branch; the active AWS Bedrock branch already contained
  the identical report implementation. The local adapter now uses Ollama while
  preserving the report UI/API, V6 persistence checksum, prompt, and schema.
- 2026-08-30: The authoritative `./verify.ps1` gate passed 150/150 copilot API,
  9/9 operations MCP server, and 53/53 Angular tests with zero failures,
  errors, or skips. Flyway V1-V7, PostgreSQL 17.11 Testcontainers, Spotless,
  Prettier, the 324.41 kB production build, zero-vulnerability npm audit,
  Compose validation, and `git diff --check` all passed.
- 2026-08-30: The bounded-generation `./verify.ps1` gate passed 154/154 copilot
  API, 9/9 operations MCP server, and 60/60 Angular tests with zero failures,
  errors, or skips. Spotless, Prettier, the 324.41 kB production build,
  zero-vulnerability npm audit, Compose validation, and `git diff --check` all
  passed; the same fix was verified on all three active local branches.
- 2026-08-30: P3 focused backend decision, timeline, request-context, queue, and
  architecture tests passed. The backend build executed 136/182 copilot tests
  plus 9/9 MCP tests without failures, and Spotless passed, but the backend gate
  correctly failed because Docker unavailability skipped 46 PostgreSQL tests,
  including all 7 new decision/timeline persistence and concurrency scenarios.
- 2026-08-30: `./verify.ps1 -Scope Frontend` passed 75/75 Angular tests with
  zero skips, locked installation with zero vulnerabilities, Prettier, and the
  386.21 kB production build. `./verify.ps1 -Scope Repository` also passed
  verification-contract tests, Compose validation, and `git diff --check`.
- 2026-08-30: The production API applied Flyway V8 to native PostgreSQL 18.3 and
  started successfully. Native smokes passed approval, rejection, exact replay,
  conflicting and truly concurrent decisions, transactional rollback after a
  forced lifecycle race, exact report binding, preserved report content, actor
  persistence, tenant isolation, Active/Completed discovery, and direct refresh.
- 2026-08-30: A historical native timeline returned all 30 expected unique
  events oldest-first, including 12 evidence, 8 retrieval, and 8 report attempts,
  28 failure/unavailable states, and 20 honest `UNATTRIBUTED` actors. Live
  1280x720 and 390x844 approval/completed/direct-refresh checks had no horizontal
  overflow, off-viewport decision controls, browser warnings, or browser errors.
  The approval and rejection records remain as synthetic review fixtures; the
  internal concurrency and rollback fixtures were removed after verification.
- 2026-08-30: Reproduced the pushed backend CI failure locally with Docker and
  corrected five stale operator-attributed HTTP fixtures plus one duplicate
  investigation-correlation fixture. The focused 22-test PostgreSQL/API run,
  `./verify.ps1 -Scope Backend`, and authoritative `./verify.ps1` gate pass:
  182/182 copilot API, 9/9 MCP server, and 75/75 Angular tests with zero failures,
  errors, or skips, plus formatting, builds, npm audit, Compose, verification
  contracts, and diff checks.
- 2026-08-31: Queue auto-refresh red-green regressions passed 11/11 focused
  tests. The frontend gate passed 77/77 tests, Prettier, and the production
  build; the authoritative `./verify.ps1` gate passed 182/182 copilot API, 9/9
  MCP server, and 77/77 Angular tests with zero skips, plus all repository
  checks.
- 2026-08-31: The synthetic generator's launcher regression failed before the
  consolidation, then its full standalone gate passed 16/16 tests with
  Spotless. The root launcher's `--CheckOnly` preflight also passed.
- 2026-08-31: K1 corpus-contract checks passed for 30 unique versions, all 36
  scenario mappings, all 49 error codes, 23 retrieval cases, sensitive-pattern
  review, diff checks, and repository verification.
- 2026-08-31: K2 passed 6/6 focused PDF tests and validated 30 deterministic,
  unencrypted, text-extractable PDFs totaling 112 pages (3-4 each). All 112
  rendered pages passed visual review, including every superseded banner and
  replacement reference.

## Update rule

Keep this file factual and brief. Move implementation detail into task briefs,
architecture documents, or ADRs.
