# Task: Receive and queue a synthetic payment alert

Status: Complete
Created: 2026-08-20
Owner: Christopher Guzowski

## Goal

Accept a synthetic payment alert through the API, persist it, and display it in
the operator's alert queue.

## User story

As a payment operations analyst, I want new alerts to appear in a clear queue
so that I can identify which incident to investigate next.

## Context

This is the first executable portion of the product lifecycle. It deliberately
ends before MCP, retrieval, and report generation so the intake and queue
contract can be verified independently.

## Chosen contract

The first incident family is a synthetic payment authorization decline-rate
spike. Alert intake accepts `tenantId`, `externalAlertId`, `severity`,
`detectedAt`, `title`, and `description`. The application owns the incident
type `AUTHORIZATION_DECLINE_RATE_SPIKE` and initial status `NEW`.

Queue summaries expose only incident ID, external alert ID, incident type,
severity, status, title, detected time, and received time. Full descriptions
and tenant identifiers are not repeated in the queue projection.

## In scope

- Define the first synthetic payment-alert contract.
- Add an HTTP endpoint for alert ingestion.
- Validate required input.
- Enforce idempotency using tenant and external alert identifiers.
- Persist a new incident with status `NEW`.
- Add an endpoint that returns queue summaries.
- Generate the Angular application if it has not been initialized.
- Display the alert queue with loading, empty, success, and error states.

## Out of scope

- MCP calls
- Runbook or policy retrieval
- Bedrock integration
- Investigation reports
- Authentication
- Automatic prioritization by AI

## Constraints

- Use synthetic data only.
- Include `tenant_id` even though the UI demonstrates one tenant.
- Use Flyway for database changes.
- Do not return JPA entities from controllers.
- Duplicate alerts must not create duplicate incidents.

## Acceptance criteria

- [x] A valid alert is persisted with status `NEW`.
- [x] Repeating the same tenant and external alert ID is idempotent.
- [x] Invalid alerts receive a structured HTTP 400 response.
- [x] Queue results contain only the fields necessary for triage.
- [x] The operator console displays the queue and all asynchronous states.
- [x] Backend and frontend tests cover successful, invalid, and empty cases.

## Test plan

- Alert ingestion integration tests cover valid persistence, invalid input, and
  duplicate idempotency before the ingestion implementation is written.
- Queue contract tests cover the triage projection and empty results before the
  queue implementation is written.
- Operator-console tests cover loading, empty, success, and error states before
  the queue view is implemented.

## Expected approach

1. Agree on the smallest alert request and queue response schemas, then map
   every acceptance criterion to a named test.
2. Write and run the focused backend tests to confirm the expected failures.
3. Implement the minimum backend behavior needed to pass those tests.
4. If needed, create only the Angular and test harness scaffold; then write and
   run the focused frontend tests to confirm the expected failures.
5. Implement the minimum queue UI behavior needed to pass those tests.
6. Refactor while green, then run the broader validation commands and one
   manual synthetic alert.

## Likely files or components

- `backend/copilot-api/src/main/java/.../incident/`
- `backend/copilot-api/src/main/resources/db/migration/`
- `frontend/operator-console/src/app/features/alert-queue/`

## Validation commands

```bash
mvn clean verify
cd frontend/operator-console && npm test -- --watch=false && npm run build
```

## Decisions needed

- None. The first incident family and intake fields are recorded above.

## Progress notes

- 2026-08-20: Initial task brief created with the repository scaffold.
- 2026-08-22: Selected the authorization decline-rate spike and the six-field
  intake contract.
- 2026-08-22: Implemented the alert API, atomic tenant/external-ID
  idempotency, Flyway description migration, and narrow tenant queue.
- 2026-08-22: Initialized Angular 21.2.21 and implemented loading, empty,
  success, error/retry, age sort, and severity sort states.

## Completion evidence

- Red-phase evidence: Focused backend tests failed for missing intake classes,
  unimplemented duplicate handling, missing queue classes, and an unstructured
  malformed-severity response. Frontend tests failed because the queue feature
  modules did not yet exist.
- Green-phase evidence: With Docker Desktop available, `mvn clean verify` ran
  all 11 backend tests with 0 failures, 0 errors, and 0 skipped, including all
  4 PostgreSQL/Testcontainers cases. `npm test -- --watch=false` previously
  passed 8 frontend tests across 3 files.
- Acceptance-criteria coverage: Service and HTTP tests cover creation state,
  duplicate lookup, missing and malformed input, queue projection, and empty
  results. Component tests cover loading, empty, success, error/retry, and
  sorting. Testcontainers cases cover real persistence and idempotency when a
  Docker runtime is available.
- Full verification: Maven reactor passed; Testcontainers connected to Docker
  Desktop 29.7.2 and PostgreSQL 17.11; Flyway validated and applied migrations
  V1 and V2 to an empty PostgreSQL schema. `docker compose config` rendered
  successfully, and the Compose PostgreSQL service became healthy. `npm ci`
  previously reported zero vulnerabilities; the Angular production build
  passed at 249.83 kB initial raw output; Prettier check passed.
- Manual verification: Started the API against the Compose database using
  `POSTGRES_PORT=5433` because a native PostgreSQL process owns local port 5432.
  Posting synthetic alert `manual-auth-decline-20260822-001` twice returned 201
  then 200 with the same incident ID. Direct SQL showed exactly 1 total incident
  and exactly 1 row for the tenant/external-alert key, with status `NEW`.
- Tenant-queue verification: The requested tenant returned exactly 1 queue
  summary with only `incidentId`, `externalAlertId`, `incidentType`, `severity`,
  `status`, `title`, `detectedAt`, and `receivedAt`; a different tenant returned
  an empty array.
- Documentation updated: Project choice, factual status, active contract, and
  operator-console usage updated.
- Remaining limitations: None for this task.
