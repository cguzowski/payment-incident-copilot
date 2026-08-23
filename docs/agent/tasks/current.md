# Task: Open an incident from the alert queue

Status: Complete

Created: 2026-08-22

Owner: Christopher Guzowski

## Goal

Allow a payment operations analyst to select a synthetic incident from the
operator alert queue and open a tenant-scoped, read-only incident-detail page.

The page must expose the fuller incident context intentionally omitted from the
queue projection, including the alert description, without starting an
investigation or changing persisted state.

Do not add investigation creation, incident status transitions, MCP calls,
retrieval, report generation, authentication, or unrelated UI redesign.

## User story

As a payment operations analyst, I want to open an alert-queue item and review
its complete incident details so that I can understand the signal before
deciding whether to start an investigation.

## Current verified state

- Synthetic alerts can be accepted through `POST /api/alerts`.
- Valid alerts are persisted with incident type
  `AUTHORIZATION_DECLINE_RATE_SPIKE` and status `NEW`.
- Tenant and external-alert identifiers provide atomic idempotency.
- The tenant-scoped queue returns a narrow triage projection.
- The Angular operator console renders loading, empty, success, error/retry,
  responsive, age-sort, and severity-sort queue states.
- The current queue displays live data from native PostgreSQL 18 through the
  Angular development proxy and Spring Boot API.
- Flyway V1 and V2 are applied successfully.
- Root Maven verification passes 11 backend tests with 0 skipped.
- Frontend verification passes 8 tests, the production build, and formatting.
- Docker PostgreSQL 17.11 remains the repeatable Testcontainers and Compose
  baseline; native PostgreSQL 18.3 compatibility is verified separately.

## Chosen contract

### Operator route

The operator console route is:

```text
/incidents/:incidentId
```

The incident title in the alert queue must be rendered as a real Angular router
link to this route. Do not implement navigation using only a table-row click
handler. The link must remain keyboard accessible and have a meaningful
accessible name.

### Detail API

The detail endpoint is:

```http
GET /api/incidents/{incidentId}?tenantId={tenantId}
```

The tenant ID remains explicit because authentication and authenticated tenant
resolution are outside the current MVP. The frontend must use the same
configured synthetic tenant identity already used by the queue.

A successful response returns HTTP 200 with exactly these fields:

```json
{
  "incidentId": "uuid",
  "externalAlertId": "string",
  "incidentType": "AUTHORIZATION_DECLINE_RATE_SPIKE",
  "severity": "HIGH",
  "status": "NEW",
  "title": "string",
  "description": "string",
  "detectedAt": "ISO-8601 timestamp",
  "receivedAt": "ISO-8601 timestamp"
}
```

Do not include `tenantId`, persistence metadata, internal database fields, or
future investigation fields in the response.

### Error behavior

- A missing or blank `tenantId` returns a structured HTTP 400 response.
- A malformed incident UUID returns a structured HTTP 400 response.
- A nonexistent incident returns a structured HTTP 404 response.
- An incident belonging to another tenant returns the same structured HTTP 404
  response as a nonexistent incident.
- The 404 response must not reveal whether an incident exists for another
  tenant.
- Reuse the established structured API-error envelope. Do not introduce a
  second error format for this endpoint.

### Read-only behavior

This endpoint and page are strictly read-only:

- Do not insert, update, or delete any database row.
- Do not change incident status from `NEW`.
- Do not create an investigation record.
- Repeated detail requests must have no persisted side effects.

## In scope

- Add the tenant-scoped incident-detail API contract.
- Add the minimum repository query needed to load one incident by both tenant
  ID and incident ID.
- Add a service that distinguishes a successful lookup from a tenant-safe
  not-found result.
- Add a controller that returns the chosen detail DTO and established structured
  errors.
- Link each queue incident title to its incident-detail route.
- Add Angular routing for `/incidents/:incidentId`.
- Add an incident-detail API service and response model.
- Add a read-only incident-detail page.
- Display title, description, external alert ID, incident type, severity,
  status, detected time, and received time.
- Add loading, success, not-found, and retryable API-error states.
- Add a clear router link back to the alert queue.
- Support direct navigation and browser refresh on a valid detail URL in the
  existing local development environment.
- Preserve the existing desktop and 390-pixel responsive presentation.
- Add focused backend, PostgreSQL integration, Angular service, routing, and
  component tests using TDD.
- Update task and factual project-status documentation after verification.

## Out of scope

- Creating, starting, assigning, pausing, completing, or reopening an
  investigation.
- Adding an `investigation` table or investigation ID.
- Changing incident status or adding new incident statuses.
- MCP calls or changes to `operations-mcp-server`.
- Evidence collection or evidence normalization.
- Runbook or policy ingestion and retrieval.
- pgvector queries, embeddings, or RAG.
- Bedrock or any other LLM integration.
- Investigation-report generation.
- Human approval, rejection, or request-changes actions.
- Audit-trail implementation.
- Authentication, authorization, user accounts, or tenant discovery from an
  authenticated principal.
- Queue pagination, search, filtering, additional sorting, or prioritization.
- Editing or deleting incidents.
- Adding new incident families.
- New production dependencies.
- Broad refactoring, visual redesign, or unrelated formatting changes.

## Constraints

- Use synthetic data only.
- Treat `tenantId` as required input until authentication provides tenant
  context in a later slice.
- Never query incident detail by incident ID alone; the data-access predicate
  must include both tenant ID and incident ID.
- Return the same 404 contract for nonexistent and cross-tenant incidents.
- Do not return persistence entities or internal repository models from a
  controller.
- Use an explicit immutable response DTO matching the chosen contract.
- Use UTC/offset-aware timestamps and preserve the established JSON timestamp
  representation.
- Preserve existing alert-ingestion and queue contracts.
- Do not modify V1 or V2 Flyway migrations.
- No database migration should be required for this read-only slice. If a
  schema change appears necessary, stop and explain why before creating one.
- Do not change PostgreSQL versions, Docker configuration, ports, credentials,
  or `.env` behavior.
- Never print, log, stage, or commit secrets.
- Do not add dependencies merely for routing, mapping, state management, or
  styling when the current stack already provides the capability.
- Do not weaken, delete, skip, or rewrite existing tests merely to make the
  build pass.
- Do not raise Angular build budgets to conceal a style warning.
- Reuse established UI tokens and patterns. Do not copy the entire alert-queue
  stylesheet into the detail feature.
- Do not add nonfunctional buttons or placeholder actions for later phases.
- Do not commit, push, merge, or rewrite Git history during implementation.
- Target 300-800 handwritten added lines across production code and tests. If
  the implementation is projected to exceed 1,000 handwritten added lines,
  stop, report the cause, and propose a narrower plan before continuing.

## Acceptance criteria

- [x] Each queue incident title is a keyboard-accessible Angular router link to
  `/incidents/:incidentId`.
- [x] Selecting a queue incident opens the corresponding detail page without a
  full-page browser reload.
- [x] Direct navigation or refresh on a valid detail URL loads the incident.
- [x] `GET /api/incidents/{incidentId}?tenantId={tenantId}` returns HTTP 200 for
  an incident owned by the requested tenant.
- [x] The success response contains exactly `incidentId`, `externalAlertId`,
  `incidentType`, `severity`, `status`, `title`, `description`, `detectedAt`, and
  `receivedAt`.
- [x] The success response does not expose `tenantId` or internal persistence
  fields.
- [x] Missing or blank `tenantId` produces the established structured HTTP 400
  response.
- [x] A malformed incident UUID produces the established structured HTTP 400
  response.
- [x] A nonexistent incident produces the established structured HTTP 404
  response.
- [x] A cross-tenant lookup produces an indistinguishable structured HTTP 404
  response.
- [x] The repository lookup includes both tenant ID and incident ID.
- [x] Detail requests do not modify incident state or create database records.
- [x] The detail page renders title, full description, external alert ID,
  incident type, severity, status, detected time, and received time.
- [x] The detail page has distinct loading, success, not-found, and retryable
  error states.
- [x] The retry action repeats the detail request without navigating away.
- [x] A clear router link returns the operator to the alert queue.
- [x] The detail page remains usable at 390 CSS pixels without horizontal page
  overflow, clipped content, or inaccessible controls.
- [x] Browser inspection finds no console errors and no new console warnings.
- [x] Focused tests are demonstrated failing for the expected reasons before
  implementation and passing afterward.
- [x] Root backend verification, frontend tests, formatting, and production
  build all pass without skipped tests or new warnings.
- [x] The final diff contains no secrets, generated build output, unrelated
  changes, dependency additions, or migration edits.

## Test plan

Write the focused tests before the corresponding implementation. Record the
expected red-phase failures in Progress notes before writing production code.

### Backend service and contract tests

Cover these named behaviors, adapting method names only when repository naming
conventions require it:

- `returnsIncidentDetailForOwningTenant`
- `returnsNotFoundWhenIncidentDoesNotExist`
- `returnsNotFoundWhenIncidentBelongsToAnotherTenant`
- `returnsBadRequestWhenTenantIdIsMissingOrBlank`
- `returnsBadRequestWhenIncidentIdIsMalformed`
- `detailResponseContainsOnlyChosenFields`

Verify exact HTTP status codes and response fields. For nonexistent and
cross-tenant incidents, verify the same status and safe error shape.

### PostgreSQL integration tests

Cover these behaviors against Testcontainers PostgreSQL:

- `findsIncidentDetailByTenantAndIncidentId`
- `doesNotFindIncidentForDifferentTenant`
- `detailLookupDoesNotModifyPersistedIncident`

The integration tests must prove that the query is tenant-scoped in real
PostgreSQL, not only in mocks.

### Angular API-service and routing tests

Cover these behaviors:

- `requestsIncidentDetailWithIncidentIdAndConfiguredTenantId`
- `queueIncidentTitleLinksToIncidentDetailRoute`
- `loadsDetailComponentForIncidentRoute`

Verify URL encoding and do not duplicate tenant configuration inside multiple
components.

### Angular component tests

Cover these behaviors:

- `showsLoadingStateWhileDetailRequestIsPending`
- `rendersFullIncidentDetailOnSuccess`
- `showsNotFoundStateForHttp404`
- `showsRetryableErrorStateForOtherFailures`
- `retriesTheDetailRequest`
- `linksBackToAlertQueue`

Do not make component tests pass by replacing behavioral assertions with
shallow existence checks.

## Expected approach

1. Read applicable `AGENTS.md` files and the referenced project, architecture,
   domain, constraint, quality, status, and current-task documentation.
2. Inspect the existing alert queue, tenant handling, API-error envelope,
   repository SQL style, tests, Angular routes, API service, and UI conventions.
3. Confirm the chosen contract fits the existing code. If it conflicts with an
   established contract, stop and report the exact conflict before editing.
4. Map every acceptance criterion to a focused backend, integration, service,
   routing, or component test.
5. Write and run the backend and PostgreSQL tests; record the expected failures.
6. Implement the minimum tenant-scoped repository, service, DTO, and controller
   behavior required to pass.
7. Refactor while green without expanding the response or endpoint scope.
8. Write and run the Angular service, routing, queue-link, and component tests;
   record the expected failures.
9. Implement the minimum detail route and read-only UI behavior required to
   pass.
10. Refactor shared presentation only when necessary to avoid duplication; do
    not redesign the queue.
11. Run focused tests, broader backend/frontend validation, and diff checks.
12. Verify the complete flow manually against native PostgreSQL 18 using an
    existing or newly ingested synthetic incident.
13. Review the final diff for tenant leakage, response overexposure, status
    mutation, unrelated changes, secrets, generated output, and test weakening.
14. Update `current.md` and `STATUS.md` with factual evidence only. Mark the task
    Complete only when every acceptance criterion is verified.

## Likely files or components

Changes should remain primarily within:

- `backend/copilot-api/src/main/java/.../incident/`
- `backend/copilot-api/src/test/java/.../incident/`
- `frontend/operator-console/src/app/app.routes.ts`
- `frontend/operator-console/src/app/features/alert-queue/`
- `frontend/operator-console/src/app/features/incident-detail/`
- `docs/agent/STATUS.md`
- `docs/agent/tasks/current.md`

Do not modify `operations-mcp-server`, infrastructure, Docker, AWS, dependency
versions, or existing Flyway migrations for this task.

## Validation commands

Run from the repository root unless a command changes directory:

```bash
mvn clean verify

cd frontend/operator-console
npm ci
npm test -- --watch=false
npx prettier --check .
npm run build

cd ../..
git diff --check
git status --short
```

No backend or frontend test may be skipped. If Docker/Testcontainers is
unavailable, the task remains In progress rather than Complete.

## Manual verification

1. Confirm native PostgreSQL 18 is running on `localhost:5432` and the backend
   is configured through the ignored local `.env` loading mechanism.
2. Start the Spring Boot API and confirm Flyway validates the existing schema
   without creating a new migration.
3. Start Angular using the intended proxy configuration.
4. Open the alert queue and confirm at least one synthetic incident is visible.
5. Navigate using the incident-title router link; do not type the detail URL for
   this first check.
6. Confirm the selected incident's title, description, external alert ID,
   incident type, severity, status, detected time, and received time match the
   database-backed API response.
7. Refresh the browser directly on `/incidents/{incidentId}` and confirm the
   detail view reloads successfully.
8. Use the back-to-queue router link and confirm the queue returns normally.
9. Request the detail endpoint with the correct tenant and confirm HTTP 200 and
   the exact chosen field set.
10. Request the same incident with a different synthetic tenant and confirm the
    safe HTTP 404 response.
11. Request a nonexistent incident and confirm its HTTP 404 response is
    indistinguishable from the cross-tenant response.
12. Request a malformed incident ID and a blank tenant ID and confirm structured
    HTTP 400 responses.
13. Query PostgreSQL before and after repeated detail requests and confirm no
    row counts, incident values, or statuses changed.
14. Inspect the populated detail page, loading behavior, not-found state, and
    retryable error state at desktop and 390-pixel widths.
15. Confirm the browser console contains no errors and no new warnings.

## Decisions needed

None. The route, endpoint, tenant behavior, response fields, error behavior,
read-only boundary, and UI states are fixed above.

If existing repository behavior directly conflicts with this task, stop and
report the conflict rather than silently changing the contract.

## Progress notes

- 2026-08-22: Task brief created after completing the synthetic-alert queue and
  native PostgreSQL 18 validation slices.
- 2026-08-22: Backend red phase: `mvn -pl backend/copilot-api
  "-Dtest=IncidentDetailServiceTest,IncidentDetailControllerTest,AlertApiPostgresIntegrationTest"
  test` failed during test compilation for the expected missing incident-detail
  controller, service, response, not-found type, and tenant-plus-incident
  repository method. Production code had not been added.
- 2026-08-22: Backend green phase: the focused service and HTTP contract run
  passed 7 tests with 0 skipped. The PostgreSQL 17.11 Testcontainers run passed
  all 7 integration tests with 0 skipped, including tenant-and-incident lookup,
  cross-tenant exclusion, and repeated read-only detail requests.
- 2026-08-22: Frontend red phase: `npx ng test --watch=false` with the focused
  incident-detail, alert-queue, and route includes failed during Angular
  compilation for the expected missing shared tenant configuration and
  incident-detail API service, model, component, and route modules. Frontend
  production behavior had not been added.
- 2026-08-22: Frontend green phase: 15 focused API-service, routing,
  alert-queue, and incident-detail component tests passed across 5 test files.
  The suite covers encoded tenant-scoped requests, real router links, direct
  route activation, full detail rendering, all four page states, retry, and
  back navigation.
- 2026-08-22: Broader verification passed: `mvn clean verify` ran 21 copilot
  API tests with 0 skipped and built both Java services; `npm ci` installed the
  lockfile with 0 vulnerabilities; all 17 Angular tests, Prettier, and the
  269.17 kB production build passed without budget warnings; `git diff --check`
  passed.
- 2026-08-22: Native PostgreSQL 18.3 with pgvector 0.8.6 and Flyway V2 served
  the live detail contract. Correct-tenant, cross-tenant, nonexistent,
  malformed-ID, missing-tenant, and blank-tenant requests matched the chosen
  statuses and problem shapes. Two repeated detail requests preserved the full
  incident-row hash, `NEW` status, and incident count.
- 2026-08-22: Browser verification passed for queue-title navigation, direct
  route refresh, back navigation, loading, success, not-found, retryable error,
  and retry recovery. At 390 CSS pixels the document client and scroll widths
  were both 390 and all controls remained inside the viewport. A fresh normal
  flow produced 0 console errors and 0 warnings.

Add dated progress notes for:

- Contract inspection and any confirmed compatibility findings.
- Red-phase backend and PostgreSQL test evidence.
- Green-phase backend evidence.
- Red-phase frontend test evidence.
- Green-phase frontend evidence.
- Broader verification and manual browser/database evidence.

Do not record a test as passing if it was skipped, not executed, mocked instead
of integrated where integration is required, or inferred from another check.

## Completion evidence

- Final endpoint: `GET /api/incidents/{incidentId}?tenantId={tenantId}`. HTTP
  200 contains exactly `incidentId`, `externalAlertId`, `incidentType`,
  `severity`, `status`, `title`, `description`, `detectedAt`, and `receivedAt`;
  it excludes tenant and persistence metadata.
- New backend coverage: `returnsIncidentDetailForOwningTenant`,
  `returnsNotFoundWhenIncidentDoesNotExist`,
  `returnsNotFoundWhenIncidentBelongsToAnotherTenant`,
  `returnsBadRequestWhenTenantIdIsMissingOrBlank`,
  `returnsBadRequestWhenIncidentIdIsMalformed`,
  `detailResponseContainsOnlyChosenFields`, the structured-not-found contract,
  `findsIncidentDetailByTenantAndIncidentId`,
  `doesNotFindIncidentForDifferentTenant`, and
  `detailLookupDoesNotModifyPersistedIncident`. Full Maven result: 21 tests,
  0 failures, 0 errors, 0 skipped.
- New frontend coverage: `requestsIncidentDetailWithIncidentIdAndConfiguredTenantId`,
  `queueIncidentTitleLinksToIncidentDetailRoute`,
  `loadsDetailComponentForIncidentRoute`,
  `showsLoadingStateWhileDetailRequestIsPending`,
  `rendersFullIncidentDetailOnSuccess`, `showsNotFoundStateForHttp404`,
  `showsRetryableErrorStateForOtherFailures`, `retriesTheDetailRequest`, and
  `linksBackToAlertQueue`. Full Angular result: 17 tests across 6 files passed.
- Live HTTP evidence: correct tenant returned 200 with the exact nine-field
  response. Cross-tenant and nonexistent requests both returned the same safe
  `urn:problem:incident-not-found` 404. Malformed incident ID and missing or
  blank tenant returned `urn:problem:invalid-incident-request` 400 responses.
- Native database evidence: PostgreSQL 18.3, pgvector 0.8.6, and Flyway V2.
  Before and after repeated detail requests, the incident-row hash was
  `1026aef18475ab3f8e483e8eeb738ce3`, status remained `NEW`, and incident count
  remained 3.
- Browser evidence: desktop and 390-pixel detail layouts displayed every field;
  direct refresh and both router links worked; loading, not-found, retryable
  error, and retry recovery were observed; 390-pixel client and scroll widths
  matched; a fresh success flow had 0 console errors and 0 warnings.
- Validation evidence: locked install reported 0 vulnerabilities; Maven,
  Angular tests, Prettier, production build, and `git diff --check` passed. The
  production build was 269.17 kB and emitted no budget warning.
- Final audited Git-style physical-line diff, including blank lines and
  excluding the pre-existing untracked completed-task file: production 22
  files, 618 added lines, and 97 removed lines; tests 8 files, 497 added lines,
  and 2 removed lines; documentation 2 files, 449 added lines, and 135 removed
  lines. Production and test additions total 1,115 physical lines, exceeding
  the 1,000-line review threshold by 115 lines.
- No dependency, Flyway migration, Docker, MCP, investigation, retrieval, AI,
  authentication, or infrastructure change was introduced.

## Remaining limitations

- Investigation creation and the `NEW` to `INVESTIGATING` transition remain the
  next planned vertical slice.
- MCP evidence collection, retrieval, report generation, human review, and
  audit history remain future slices.
