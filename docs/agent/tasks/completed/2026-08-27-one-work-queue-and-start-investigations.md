# Task: Keep active incidents in one work queue and start investigations

Status: Complete

Archived: 2026-08-28

Created: 2026-08-27

Owner: Christopher Guzowski

## Goal

Give a payment operations analyst one tenant-scoped incident work queue that
retains every active incident as its workflow status changes, and let the
operator start or resume the single investigation associated with an incident.

Newly received incidents must appear first by default. Older unprocessed
incidents must remain visible until an operator acts on them, and starting an
investigation must update the existing row rather than make the work disappear.

## User story

As a payment operations analyst, I want one sortable queue for new and
in-progress incidents so that I can see all active work, start an investigation,
and return to it without switching between disconnected lists.

## Context

The current implementation intentionally stops at alert intake and read-only
incident detail. Its queue returns only incidents with status `NEW`. That was a
valid boundary for the first intake slice, but it is no longer the right product
model once investigations can be started.

For this MVP, `NEW` means unprocessed rather than recently received. There is
no age cutoff: an older `NEW` incident must remain in the work queue. An
incident that transitions to `INVESTIGATING` remains active work and must stay
discoverable in the same queue.

## Current verified state

- Synthetic alerts are accepted through `POST /api/alerts` and persisted with
  status `NEW`.
- Tenant and external-alert identifiers provide atomic alert idempotency.
- The current tenant-scoped queue returns only `NEW` incidents, newest detected
  first.
- Incident detail is available through
  `GET /api/incidents/{incidentId}?tenantId={tenantId}`.
- The Angular queue supports loading, empty, error/retry, age sort, severity
  sort, responsive layout, and keyboard-accessible detail links.
- No investigation record, investigation API, investigation route, or
  `INVESTIGATING` status exists yet.
- Flyway migrations V1 and V2 are the current schema baseline.

## Chosen contract

The owner approved this contract on 2026-08-27. The locked-task policy in
`AGENTS.md` now applies.

### One incident work queue

Replace the alert-specific collection endpoint with:

```http
GET /api/tenants/{tenantId}/incidents
```

The existing `/api/tenants/{tenantId}/alert-queue` endpoint is retired rather
than retained as a compatibility alias. There is no external consumer in the
MVP, and two endpoints with different names for the same queue would create
ambiguous semantics.

The response contains all active incidents for the requested tenant. In this
slice the active statuses are exactly `NEW` and `INVESTIGATING`. Do not add
future lifecycle values merely to anticipate later work.

Each queue item returns exactly:

```json
{
  "incidentId": "uuid",
  "externalAlertId": "string",
  "incidentType": "AUTHORIZATION_DECLINE_RATE_SPIKE",
  "severity": "HIGH",
  "status": "NEW",
  "title": "string",
  "detectedAt": "ISO-8601 timestamp",
  "receivedAt": "ISO-8601 timestamp",
  "activeInvestigationId": null
}
```

`activeInvestigationId` is `null` for `NEW` incidents and contains the tenant's
investigation ID for `INVESTIGATING` incidents.

The API default order is `receivedAt` descending so newly ingested incidents
appear first. It applies no age cutoff. The operator console keeps the full
response and supports client-side sorting by:

- newest received, which is the default;
- oldest received;
- newest detected;
- highest severity; and
- status.

Changing the sort order does not change membership in the queue. A manual
refresh reloads the collection and preserves the selected sort. Polling,
WebSockets, pagination, server-side sorting, search, and filtering are outside
this slice.

The operator-console heading changes from `Alert queue` to
`Incident work queue`. Queue rows remain keyboard-accessible links. A `NEW` row
opens incident detail, while an `INVESTIGATING` row also exposes a clear
`Resume investigation` router link.

### Investigation creation

The operator starts an investigation from the incident-detail page through:

```http
POST /api/incidents/{incidentId}/investigations?tenantId={tenantId}
Content-Type: application/json

{
  "operatorId": "uuid"
}
```

The frontend uses one centrally configured opaque synthetic operator ID until
authentication supplies an operator identity.

The first successful request:

- creates exactly one investigation;
- changes the incident from `NEW` to `INVESTIGATING` in the same transaction;
- returns HTTP 201;
- returns a `Location` header for `/api/investigations/{investigationId}`; and
- returns exactly `investigationId`, `incidentId`, `incidentStatus`,
  `startedBy`, and `startedAt`.

Repeating the request for the same tenant and incident is idempotent. It returns
HTTP 200 and the original investigation, including its original operator and
start timestamp. Concurrent requests must not create multiple investigations
or partially update incident state.

The MVP permits one investigation per incident. Reopening, rerunning, or
creating investigation attempts is outside this slice.

### Investigation persistence and audit-ready metadata

Add a new Flyway V3 migration. Do not edit V1 or V2.

The investigation record contains:

- stable investigation ID;
- tenant ID;
- incident ID;
- opaque synthetic operator ID;
- UTC start timestamp; and
- stable correlation ID.

The database must enforce one investigation per tenant and incident and must
prevent an investigation from referencing an incident belonging to another
tenant. The investigation creation record is immutable after insertion in this
slice. A general audit-event table and audit-timeline UI remain later work.

### Incident detail and resume behavior

The existing incident-detail response gains one field:

```json
{
  "activeInvestigationId": "uuid or null"
}
```

This is an intentional evolution of the previously exact nine-field detail
contract. It lets a refreshed detail page distinguish a startable `NEW`
incident from a resumable `INVESTIGATING` incident without invoking a mutation
endpoint to discover state.

The detail page shows:

- `Start investigation` when status is `NEW`;
- a disabled pending state while creation is in flight;
- retryable feedback when creation fails; and
- `Resume investigation` when status is `INVESTIGATING` and an active
  investigation ID is present.

The start action navigates to `/investigations/:investigationId` after success.
Repeated clicks or responses must not create duplicate records.

### Investigation workspace

Add a tenant-scoped read-only endpoint:

```http
GET /api/investigations/{investigationId}?tenantId={tenantId}
```

It returns exactly `investigationId`, `incidentId`, `incidentStatus`,
`startedBy`, and `startedAt`. The Angular route
`/investigations/:investigationId` displays these fields, links back to the
incident and work queue, and truthfully states that evidence collection has not
started. Direct navigation and browser refresh must work.

This page is a functional workflow destination, not a fake evidence or report
screen. It must not show nonfunctional evidence, report, approve, reject, or
remediation actions.

### Error behavior

- Missing, blank, or malformed tenant, incident, investigation, or operator
  identifiers return structured HTTP 400 responses.
- A nonexistent incident and a cross-tenant incident return indistinguishable
  structured HTTP 404 responses.
- A nonexistent investigation and a cross-tenant investigation return
  indistinguishable structured HTTP 404 responses.
- An unsupported or internally inconsistent state returns a structured HTTP
  409 response and does not mutate data.
- Failed creation leaves both the incident and investigation tables unchanged.

Reuse the established `application/problem+json` approach. Do not leak tenant
identity, record existence, internal SQL details, or stack traces.

## In scope

- Rename the operator surface to `Incident work queue`.
- Replace the alert-specific queue endpoint with the proposed incident
  collection endpoint.
- Return both `NEW` and `INVESTIGATING` incidents without an age cutoff.
- Preserve the row across the `NEW` to `INVESTIGATING` transition.
- Add the proposed sorting and manual-refresh behavior.
- Add `INVESTIGATING` to the backend and frontend incident-status types.
- Add Flyway V3 and tenant-safe investigation persistence.
- Implement atomic and idempotent investigation creation.
- Add audit-ready investigation creation metadata.
- Add start/resume behavior to incident detail.
- Add the tenant-scoped investigation read contract and minimal workspace.
- Add focused unit, HTTP, PostgreSQL, concurrency, Angular service, routing,
  and component tests using red-green-refactor.
- Update factual project documentation after verification.

## Out of scope

- A second alert, investigation, or personal-assignment queue.
- Authentication, authorization, user accounts, or authenticated tenant and
  operator discovery.
- Assignment, ownership, reassignment, pausing, cancellation, completion,
  reopening, or multiple investigation attempts.
- `AWAITING_REVIEW`, `APPROVED`, or `REJECTED` implementation.
- Completed-incident filtering or history behavior.
- Queue pagination, search, arbitrary filters, server-side sorting, polling,
  WebSockets, notifications, or automatic refresh.
- MCP calls or changes to `operations-mcp-server`.
- Evidence collection, normalization, or persistence.
- Knowledge ingestion, embeddings, pgvector retrieval, or RAG.
- Bedrock or other LLM integration.
- Report generation, citations, human decisions, or a general audit timeline.
- New incident families, infrastructure, deployment, or unrelated redesign.
- New production dependencies unless implementation proves the existing stack
  cannot satisfy a locked requirement.

## Constraints

- Use synthetic data and opaque synthetic identifiers only.
- Carry tenant identity through every incident and investigation query and
  persistence boundary.
- Never query an incident or investigation by its ID alone.
- Enforce tenant consistency and one-investigation-per-incident invariants in
  PostgreSQL, not only in application code.
- Make creation and the incident status transition atomic.
- Use UTC instants internally and ISO-8601 timestamps at API boundaries.
- Keep controllers thin and return explicit immutable DTOs.
- Preserve alert-ingestion and tenant-safe incident-detail behavior except for
  the explicitly documented detail-response addition.
- Do not edit Flyway V1 or V2, database versions, Docker configuration, ports,
  credentials, or `.env` behavior.
- Do not log or expose secrets, synthetic operator identifiers unnecessarily,
  or internal persistence details.
- Reuse existing Angular HTTP, signal, routing, accessibility, responsive, and
  presentation conventions.
- Do not weaken, delete, skip, or rewrite existing tests to make verification
  pass.
- No backend or frontend test may be skipped for completion. If Docker is
  unavailable, the task remains in progress.
- Do not commit, push, merge, or rewrite Git history during implementation.
- If the projected production-and-test diff exceeds 1,500 handwritten lines,
  stop before implementation and propose a narrower contract.

## Acceptance criteria

- [x] The operator sees one `Incident work queue`, not separate alert and
      investigation queues.
- [x] The queue returns all tenant-owned `NEW` and `INVESTIGATING` incidents.
- [x] Older `NEW` incidents remain present regardless of age.
- [x] The default ordering places the most recently received incident first.
- [x] The operator can sort by newest received, oldest received, newest
      detected, highest severity, and status.
- [x] Refresh reloads incidents and preserves the selected sort.
- [x] A `NEW` incident remains in the same queue after it transitions to
      `INVESTIGATING`.
- [x] Cross-tenant incidents never appear in the queue.
- [x] A tenant-owned `NEW` incident can be started exactly once.
- [x] Starting creates one investigation and atomically changes the incident
      status to `INVESTIGATING`.
- [x] Repeated and concurrent start requests return the same investigation and
      do not overwrite its original metadata.
- [x] A failed start leaves no partial investigation or incident-status change.
- [x] The database prevents a cross-tenant incident/investigation association.
- [x] Incident detail exposes a nullable active investigation ID without
      exposing tenant or persistence metadata.
- [x] The detail page shows `Start investigation` only for a startable incident
      and prevents duplicate interaction while the request is pending.
- [x] An in-progress incident exposes a working `Resume investigation` route.
- [x] Direct navigation or refresh on a tenant-owned investigation route loads
      the investigation workspace.
- [x] Cross-tenant and nonexistent incident or investigation lookups produce
      indistinguishable structured 404 responses.
- [x] Invalid identifiers produce structured 400 responses and unsupported
      states produce a structured 409 without mutation.
- [x] The queue, detail action, and workspace remain keyboard accessible and
      usable at 390 CSS pixels without horizontal page overflow.
- [x] Focused tests are demonstrated failing for the intended reason before
      production code and passing afterward.
- [x] Full backend, PostgreSQL, frontend, formatting, build, diff, and manual
      browser verification pass with no skipped tests or new warnings.
- [x] The final diff contains no secrets, generated output, unrelated changes,
      dependency additions without justification, or edits to V1/V2.

## Test plan

Write and run each focused test before its corresponding production behavior.
Record the expected red and green evidence in Progress notes.

### Backend workflow and HTTP tests

- `returnsAllActiveIncidentsForTenantWithoutAgeCutoff`
- `ordersWorkQueueByReceivedAtDescending`
- `returnsActiveInvestigationIdForInProgressIncident`
- `createsInvestigationAndReturnsCreatedContract`
- `returnsExistingInvestigationForRepeatedStart`
- `returnsIncidentOrInvestigationNotFoundWithoutTenantLeakage`
- `rejectsMalformedTenantIncidentInvestigationAndOperatorIds`
- `returnsConflictWithoutMutationForUnsupportedState`
- `returnsTenantScopedInvestigationWorkspace`

### PostgreSQL and concurrency tests

- `persistsInvestigationAndIncidentTransitionAtomically`
- `retainsNewAndInvestigatingIncidentsInWorkQueue`
- `doesNotApplyAnAgeCutoffToNewIncidents`
- `enforcesOneInvestigationPerTenantAndIncident`
- `concurrentStartsReturnOneInvestigation`
- `preservesOriginalStartMetadataOnRetry`
- `preventsCrossTenantInvestigationAssociation`
- `rollsBackInvestigationWhenIncidentTransitionFails`

The concurrency, tenant-integrity, and rollback criteria must execute against
real PostgreSQL through Testcontainers; mock-only evidence is insufficient.

### Angular API, routing, and component tests

- `requestsTenantIncidentWorkQueue`
- `sortsNewestReceivedByDefault`
- `sortsByEveryChosenQueueField`
- `refreshesWithoutResettingSort`
- `keepsInvestigatingIncidentInQueue`
- `startsNewIncidentWithConfiguredTenantAndOperator`
- `disablesStartWhileRequestIsPending`
- `showsRetryableStartFailure`
- `navigatesToInvestigationAfterStart`
- `resumesExistingInvestigation`
- `loadsInvestigationWorkspaceOnDirectRoute`
- `linksWorkspaceToIncidentAndWorkQueue`

Do not replace behavioral assertions with shallow component-existence tests.

## Expected approach

1. Confirm the proposed contract and move the task to `Ready` before changing
   executable code.
2. Map every acceptance criterion to the named tests and estimate the diff.
3. Write PostgreSQL migration and repository tests first; record the expected
   red failures before adding V3 or production workflow code.
4. Implement the minimum tenant-safe schema, repository, workflow service, DTO,
   controller, and structured errors needed to pass.
5. Prove atomicity and concurrent idempotency in PostgreSQL before proceeding
   to the UI.
6. Write the Angular API, sorting, start/resume, routing, and workspace tests;
   record the expected red failures.
7. Implement the minimum one-queue UI and functional investigation workspace.
8. Refactor only while focused tests stay green.
9. Run the focused suites, full verification commands, and manual database and
   browser scenarios.
10. Review the final diff for tenant leakage, partial state, duplicate creation,
    contract drift, test weakening, secrets, generated output, and unrelated
    changes.
11. Update `current.md`, `STATUS.md`, and architectural documentation with
    factual completion evidence only.

## Likely files or components

- `backend/copilot-api/src/main/java/.../incident/`
- `backend/copilot-api/src/main/java/.../investigation/`
- `backend/copilot-api/src/test/java/.../incident/`
- `backend/copilot-api/src/test/java/.../investigation/`
- `backend/copilot-api/src/main/resources/db/migration/V3__*.sql`
- `frontend/operator-console/src/app/core/models/incident.ts`
- `frontend/operator-console/src/app/core/config/`
- `frontend/operator-console/src/app/features/alert-queue/`
- `frontend/operator-console/src/app/features/incident-detail/`
- `frontend/operator-console/src/app/features/investigation-workspace/`
- `frontend/operator-console/src/app/app.routes.ts`
- `docs/agent/STATUS.md`
- `docs/agent/tasks/current.md`

Do not modify `operations-mcp-server`, infrastructure, Docker, AWS, dependency
versions, or existing Flyway migrations for this task.

## Validation commands

Run from the repository root unless a command changes directory:

```bash
mvn clean verify
docker compose config

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
unavailable, the task remains in progress rather than complete.

## Manual verification

1. Load multiple synthetic incidents for the configured tenant, including an
   older unprocessed incident.
2. Confirm all `NEW` incidents appear with the newest received first.
3. Exercise every sort option and confirm refresh preserves the selection.
4. Open a `NEW` incident and start its investigation.
5. Confirm the action is pending-safe and navigates to the investigation route.
6. Confirm the same incident remains in the work queue as `INVESTIGATING` with
   the same incident identity and a resume route.
7. Repeat the start request and confirm the same investigation, operator, and
   timestamp are returned without a new row.
8. Issue concurrent start requests and confirm one investigation row exists.
9. Verify incident and investigation tenant IDs match and the incident status
   changed atomically.
10. Verify cross-tenant incident and investigation requests return the same 404
    shapes as nonexistent records and cause no mutation.
11. Refresh the investigation URL directly and confirm the workspace reloads.
12. Inspect desktop and 390-pixel queue, detail, pending/error, and workspace
    states for accessibility, overflow, console errors, and warnings.

## Decisions needed

None. The owner approved the queue, endpoint, persistence, operator identity,
response, and workspace contracts before implementation began.

## Progress notes

- 2026-08-27: Proposed after owner discussion established that `NEW` is a
  workflow state rather than an age limit and that one queue should retain all
  active incident work through status transitions.
- 2026-08-27: Owner approved the proposed contract and authorized
  implementation. Task moved to `In Progress`; all behavioral contract sections
  are now locked.
- 2026-08-27: Work-queue backend red phase: focused Maven test compilation
  failed for the expected missing `IncidentWorkQueueController`, service,
  response item, and `INVESTIGATING` status. No work-queue production behavior
  had been added.
- 2026-08-27: Investigation backend red phase: a clean focused Maven run failed
  during test compilation for the expected missing investigation workflow,
  repository, DTOs, controller, structured exceptions, and active-investigation
  detail projection. PostgreSQL tests for concurrency, tenant integrity, and
  transactional rollback were present before investigation production code.
- 2026-08-27: Frontend red phase: `npm test -- --watch=false` failed during
  Angular compilation for the expected missing active-investigation models,
  operator configuration, investigation API service, workspace component, and
  route. The new queue and start/resume tests were present first.
- 2026-08-27: Backend green phase: focused unit and HTTP tests passed 18/18.
  The full Maven reactor then passed 39/39 with zero skips, including all 16
  PostgreSQL/Testcontainers migration, tenant-integrity, concurrency, retry,
  and rollback tests.
- 2026-08-27: Frontend green phase: after `npm ci`, all 26 Angular tests passed,
  the production build passed, and `npx prettier --check .` passed.
- 2026-08-27: Manual verification exercised all five sort modes, sort-preserving
  refresh, start, retained `INVESTIGATING` membership, resume, idempotent retry,
  direct workspace refresh, tenant-safe 404s, responsive queue/detail/workspace
  and retryable error states, and successful-path console output.
- 2026-08-27: Final invalid-input audit added a JSON-null regression test. It
  failed with the expected alert-specific problem type, then passed after
  unreadable investigation bodies were routed to the structured investigation
  400 response. The zero-skip full Maven suite passed again afterward.

## Completion evidence

- Red-phase evidence: Focused backend and frontend compilation failed for the
  expected missing queue, investigation, status, API, route, and workspace
  types before their production implementations were added.
- Green-phase evidence: Focused backend tests passed 18/18; PostgreSQL tests
  passed 16/16; the full Maven reactor passed 39/39 with zero skipped tests;
  Angular tests passed 26/26.
- Acceptance-criteria coverage: Unit, HTTP, PostgreSQL, concurrency, Angular
  API/component/routing tests and the live browser flow cover every criterion.
- Full verification: `mvn clean verify`, `docker compose config --quiet`,
  `npm ci`, `npm test -- --watch=false`, `npx prettier --check .`,
  `npm run build`, and `git diff --check` passed. The production-and-test diff
  is exactly 1,500 handwritten added lines, within the locked guardrail.
- Manual verification: The live Angular/API/PostgreSQL flow passed on desktop
  and at 390 CSS pixels with no horizontal page overflow or unexpected console
  output. The backend-down error state was also intentionally exercised and
  remained usable at 390 pixels.
- Documentation updated: README, project, domain, architecture, status, and
  this task reflect the one-queue and investigation-start contract.
- Remaining limitations: MCP evidence collection, retrieval, report generation,
  human decision, complete lifecycle states, and general audit history remain
  future slices.
