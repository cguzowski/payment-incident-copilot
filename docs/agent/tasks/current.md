# Task: Validate the application against local PostgreSQL 18

Status: Complete
Created: 2026-08-22
Owner: Christopher Guzowski

## Goal

Verify that the repository connects to the native local PostgreSQL database
using ignored `.env` configuration, confirm PostgreSQL 18 compatibility, and
run the existing application locally end to end.

Do not add product features, redesign the UI, or change the Docker PostgreSQL
17.11 baseline merely because the native server uses a newer major version.

## Current verified state

- Docker PostgreSQL 17.11 passed all 11 backend tests with 0 skipped.
- Flyway validated and applied V1 and V2.
- Compose validation and PostgreSQL health checks passed.
- Duplicate alert submission returned 201 then 200 with one persisted row.
- Tenant isolation and the narrow queue projection passed.
- Frontend tests, production build, formatting, and visual checks passed.
- Docker used host port 5433 because native PostgreSQL owns 5432.
- The Docker data volume remains preserved.

## In scope

- Protect local database credentials and restore safe example placeholders.
- Identify the connected server from server-side metadata.
- Inspect JDBC, Flyway, Spring Boot, migrations, and PostgreSQL-specific SQL.
- Run Flyway and the backend against native PostgreSQL 18.
- Verify alert persistence, atomic idempotency, tenant isolation, and queue
  projection using unique synthetic identifiers.
- Run frontend dependency installation, tests, and production build.
- Run the frontend through its intended local proxy and inspect the live queue
  and browser console against the PostgreSQL 18-backed API.
- Document native PostgreSQL 18 and Docker PostgreSQL 17.11 startup separately.

## Constraints

- Never print, log, document, stage, or commit database passwords.
- Real credentials belong only in ignored `.env`; `.env.example` contains
  placeholders.
- If credentials were committed or pushed, stop and require password rotation;
  do not rewrite Git history.
- Do not assume Spring Boot reads `.env`; determine and use the actual loading
  mechanism.
- Obtain the PostgreSQL version from the connected server, not the client.
- Stop before migrations if the database appears shared, unrelated, or has
  conflicting application objects.
- Never run `flyway clean`, drop a database or schema, delete existing records,
  or alter an already-applied migration.
- Do not upgrade dependencies unless an observed PostgreSQL 18 failure requires
  it.
- Use only unique synthetic alert and tenant identifiers for manual checks.
- Preserve the Docker PostgreSQL 17.11 repeatable baseline.

## Acceptance criteria

- [x] Real credentials exist only in ignored, untracked, unstaged `.env`.
- [x] `.env.example` contains safe placeholders and no committed revision
  contains the local password.
- [x] Server-side metadata identifies PostgreSQL 18, host, port, database,
  user, and current schema without exposing the password.
- [x] Flyway V1 and V2 validate or apply successfully against PostgreSQL 18.
- [x] The Spring Boot backend starts against PostgreSQL 18 without database
  errors.
- [x] A unique synthetic alert returns 201, and its duplicate returns 200 with
  the same incident ID.
- [x] SQL shows exactly one matching PostgreSQL 18 row.
- [x] The correct tenant queue returns one narrow projection, another tenant
  returns an empty queue, and neither tenant ID nor description is exposed.
- [x] The Angular console displays the live PostgreSQL 18-backed alert through
  its intended local proxy with no browser-console errors.
- [x] Backend tests, frontend tests, and the frontend production build pass.
- [x] Documentation distinguishes native PostgreSQL 18 on 5432 from Docker
  PostgreSQL 17.11 on 5433.
- [x] No secret appears in tracked changes.

## Compatibility review

Inspect before changing anything:

- PostgreSQL JDBC driver version.
- Flyway core and PostgreSQL module versions.
- Spring Boot datasource and environment-variable configuration.
- V1 and V2 migrations.
- Repository SQL and PostgreSQL-specific behavior.

Compatibility must be demonstrated by running the current migrations and
application against the native server. Testcontainers PostgreSQL 17 evidence
must remain explicitly separate from native PostgreSQL 18 evidence.

## Validation commands

```bash
mvn clean verify
cd frontend/operator-console
npm ci
npm test -- --watch=false
npm run build
git diff --check
git status --short
```

## Manual verification

1. Read server-side version, port, database, user, and schema metadata.
2. Start the backend against native PostgreSQL 18 using the safe local
   environment-loading mechanism.
3. Submit one unique synthetic alert twice.
4. Confirm 201 then 200, the same incident ID, and exactly one matching row.
5. Confirm one narrow queue item for the correct tenant and an empty queue for
   another tenant.
6. Start Angular with its intended proxy, inspect the live alert, and check the
   browser console.

## Out of scope

- New product behavior or UI redesign.
- PostgreSQL downgrade.
- Changing the Docker PostgreSQL 17.11 baseline.
- Unrelated production, architecture, dependency, or formatting changes.

## Progress notes

- 2026-08-22: Archived the completed synthetic-alert queue task.
- 2026-08-22: Confirmed the credential-bearing example was never committed,
  copied local values to ignored `.env`, and restored example placeholders.
- 2026-08-22: Identified native PostgreSQL 18.3 on localhost:5432 and confirmed
  the dedicated `payment_copilot` database was empty before migration.
- 2026-08-22: Installed official pgvector 0.8.6 for PostgreSQL 18, created the
  configured non-superuser application role, and enabled the extension as the
  administrator.
- 2026-08-22: Flyway applied V1 and V2 and the backend started successfully on
  PostgreSQL 18.3 without dependency or migration changes.
- 2026-08-22: Verified unique alert creation, atomic duplicate handling,
  exactly one matching row, tenant isolation, and the narrow queue projection.
- 2026-08-22: Passed frontend install, tests, production build, live proxy
  rendering, and browser-console inspection.

## Completion evidence

- Credential safety: `.env` is ignored, untracked, and unstaged;
  `.env.example` contains placeholders; all committed revisions of the example
  were inspected without finding a real password.
- Native server: PostgreSQL 18.3 (`server_version_num` 180003) at
  `localhost:5432`, database `payment_copilot`, user `payment_copilot`, schema
  `public`. The application role is a non-superuser.
- Compatibility: Spring Boot 4.1.0 resolves PostgreSQL JDBC 42.7.11 and Flyway
  12.4.0 with `flyway-database-postgresql`. No dependency change was required.
  Official pgvector 0.8.6 was built for the native PostgreSQL 18 installation.
- Flyway: V1 `baseline` and V2 `add incident description` both succeeded and
  the schema reached version 2.
- Manual API: Tenant `e5efa6a0-e54c-4f4a-a154-99703bfcac89` and external alert
  `pg18-native-70274f6ea9d04c76971211471fd4a850` returned 201 then 200 with
  incident `9381bc78-5fec-4bf9-a362-11819d19d226`; SQL returned exactly one
  matching row.
- Queue: The correct tenant returned one item containing only `incidentId`,
  `externalAlertId`, `incidentType`, `severity`, `status`, `title`,
  `detectedAt`, and `receivedAt`. A different tenant returned an empty array;
  neither `tenantId` nor `description` was exposed.
- Live frontend: The intended Angular proxy displayed one PostgreSQL 18-backed
  alert for the configured synthetic UI tenant. Browser inspection found zero
  console warnings or errors.
- Broader verification: Root `mvn clean verify` passed the full three-module
  reactor; all 11 copilot API tests passed with 0 skipped. Those Testcontainers
  tests used Docker PostgreSQL 17.11, not PostgreSQL 18. `npm ci` installed the
  lockfile with 0 vulnerabilities, all 8 frontend tests passed, and the
  production build completed at 249.83 kB initial raw output.
- Documentation: Root local-development instructions now distinguish ignored
  `.env` loading, native PostgreSQL 18 on 5432, and Docker PostgreSQL 17.11 on
  5433.

## Remaining limitations

- No remaining task limitation. Synthetic verification rows remain in the
  native development database; no existing records were deleted.
