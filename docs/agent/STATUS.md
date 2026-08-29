# Project status

Last updated: 2026-08-29

## Current milestone

Milestone 1 — Establish the operator investigation workflow.

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

## In progress

- Authorized live Bedrock smoke verification remains pending as the preserved
  external limitation from the completed approved-knowledge task.

## Next

1. Run the documented one-shot embedding smoke and explicit importer in an
   environment authorized for `amazon.titan-embed-text-v2:0`.
2. Prepare report generation as a separate proposed task after owner review.
3. Keep human decisions and additional evidence tools out of that proposal
   unless separately approved.

## Blockers

- No authorized AWS credential/profile was available in this task environment,
  so live Titan V2 smoke verification remains external.

## Known deliberate gaps

- No authentication yet.
- Only `getRecentServiceErrors` is implemented; additional evidence domains and
  operator-selected investigation areas remain future product tasks.
- Live Amazon Titan Text Embeddings V2 authorization, region availability, and
  returned-vector behavior have not yet been verified in an authorized AWS
  environment; local and CI verification use the deterministic embedding double.
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

## Update rule

Keep this file factual and brief. Move implementation detail into task briefs,
architecture documents, or ADRs.
