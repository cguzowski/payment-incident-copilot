# Project status

Last updated: 2026-08-28

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
  `.env`, checks prerequisites, and starts the API and operator console.
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

## In progress

- The approved first deterministic MCP evidence slice is in implementation on
  branch `codex/mcp-service-error-evidence`. Its locked contract is recorded in
  `docs/agent/tasks/current.md`.

## Next

1. Implement and contract-test `getRecentServiceErrors` over Streamable HTTP.
2. Implement tenant-safe evidence persistence, collection APIs, and
   observed-evidence workspace states through red-green-refactor.
3. Defer workspace incident context, knowledge retrieval, Bedrock reports, and
   human decisions to later product tasks.

## Blockers

- None.

## Known deliberate gaps

- No authentication yet.
- No MCP tools implemented yet.
- No Bedrock model configured yet.
- No knowledge-ingestion pipeline yet.
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

## Update rule

Keep this file factual and brief. Move implementation detail into task briefs,
architecture documents, or ADRs.
