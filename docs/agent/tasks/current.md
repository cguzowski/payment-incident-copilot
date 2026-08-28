# Task: Collect and display recent service-error evidence

Status: In Progress

Created: 2026-08-28

Owner: Christopher Guzowski

## Goal

Give a payment operations analyst the first complete operational-evidence
workflow: from an existing investigation, invoke one deterministic read-only
MCP tool, preserve the result and its provenance in PostgreSQL, and present the
observed evidence or its explicit availability failure in the investigation
workspace.

## User story

As a payment operations analyst, I want to collect and inspect recent
payment-authorization service errors from an investigation so that I can begin
triage from sourced observations while unavailable or incomplete evidence
remains explicit.

## Context

The application can receive a synthetic authorization-decline alert, retain it
in one tenant-scoped work queue, open its incident detail, and start or resume
its investigation. The investigation workspace intentionally stops at
`Evidence collection has not started`.

The operations MCP server is configured for Streamable HTTP but exposes no
tools. The copilot API has no MCP client, evidence persistence, or evidence API.
This task establishes that boundary with one tool before knowledge retrieval,
Bedrock report generation, or human decisions are introduced.

`getRecentServiceErrors` is the first tool because the chosen incident family
is an authorization decline-rate spike and the incident does not yet carry a
transaction or payment-attempt identifier required by narrower transaction or
gateway lookups.

## Strict scope boundary

- The previous task, `Polish the operator workflow presentation`, is complete
  and archived. This task does not reopen, extend, or revise that work.
- The only new visible frontend capability authorized by this task is the
  service-error evidence collection and history section inside the existing
  investigation workspace.
- References to the queue, incident detail, existing workspace metadata,
  keyboard access, responsive behavior, and existing tests are regression
  requirements only. They do not authorize further styling, spacing, wording,
  fixture-label, control, timestamp, or layout changes to completed surfaces.
- Do not modify the queue or incident-detail presentation. Do not redesign the
  application shell or the existing investigation metadata presentation.
- If the evidence section cannot be integrated without changing a completed
  product or presentation contract, stop and request an explicit task-contract
  decision instead of expanding scope.

## Chosen contract

The owner approved this contract on 2026-08-28. Implementation has begun, so
the behavioral contract and scope are locked under the repository task-update
policy.

### Operator trigger and HTTP API

- The investigation workspace exposes an explicit
  `Collect service-error evidence` action. Loading or refreshing a GET route
  never invokes an MCP tool or mutates evidence state.
- `POST /api/investigations/{investigationId}/evidence-collections` with the
  required `tenantId` query parameter creates one collection attempt. The
  request body is absent. The client cannot submit a tool name, source,
  scenario reference, correlation ID, or tool-call ID; the application derives
  and owns all of them and chooses `getRecentServiceErrors`.
- `GET /api/investigations/{investigationId}/evidence-collections` with the
  required `tenantId` query parameter returns every attempt newest first.
- A deliberate retry creates a new attempt. Previous attempts are never
  overwritten or hidden.
- A successfully recorded attempt returns `201 Created` even when its terminal
  evidence status is partial, not found, unavailable, timed out, or malformed.
  Invalid identifiers return a structured `400`. Cross-tenant and nonexistent
  investigations return indistinguishable structured `404` responses without
  calling the MCP server or creating an attempt.
- The existing investigation-start and investigation-workspace response
  contracts remain unchanged. The incident remains `INVESTIGATING`; this task
  introduces no report or review lifecycle state.
- Each collection response exposes exactly `evidenceId`, `status`,
  `sourceSystem`, `sourceTool`, `toolCallId`, `requestedAt`, nullable
  `retrievedAt`, nullable `completedAt`, `contentSchemaVersion`, nullable
  `content`, and nullable `statusDetail`. It does not expose `tenantId`, the
  investigation correlation ID, the scenario reference, database keys, or raw
  MCP content.
- Non-null `content` contains exactly `serviceName`, `observedFrom`,
  `observedTo`, and `errors`. Each error contains exactly `sourceEventId`,
  `observedAt`, `errorCode`, and `count`.

### MCP tool

- The operations MCP server exposes exactly one read-only tool named
  `getRecentServiceErrors` through its existing Streamable HTTP transport.
- The input contains exactly the tenant ID, synthetic scenario reference,
  investigation correlation ID, and tool-call ID. The scenario reference is
  the incident's synthetic `externalAlertId` and remains an opaque lookup key.
- Tenant, correlation, and tool-call identifiers are UUIDs. The scenario
  reference is a non-blank string with the existing external-alert maximum of
  120 characters. No additional or unknown input field is accepted.
- The result returns the source system, source tool, retrieval timestamp,
  echoed correlation and tool-call identifiers, availability status, an
  optional safe status detail, and bounded structured content.
- Structured content uses an application-owned `service-errors/v1` schema with
  service name, observed time window, and zero or more error observations. Each
  observation contains an opaque source event ID, observed timestamp, error
  category or code, and aggregate count.
- Tool data is deterministic for a scenario. Runtime retrieval metadata may
  reflect the actual invocation time through an injected UTC clock.
- Repository-owned synthetic fixtures cover available errors, available with
  no errors, partial data, unknown scenario, unavailable source, timeout, and
  malformed fixture data.
- An available result with zero errors is successful negative evidence. It is
  not represented as not found or unavailable.

### Evidence status and persistence

- Collection attempts use exactly these statuses: `STARTED`, `AVAILABLE`,
  `PARTIAL`, `NOT_FOUND`, `UNAVAILABLE`, `TIMED_OUT`, and `MALFORMED`.
- Flyway V4 creates tenant-safe evidence-collection persistence. PostgreSQL
  enforces that each attempt's tenant and investigation belong together.
- Every attempt has stable evidence and tool-call identifiers, the internal
  investigation correlation ID, source system, source tool, scenario
  reference, requested/retrieved/completed UTC timestamps, content schema
  version, normalized content, and a bounded safe status detail where
  applicable.
- The copilot API inserts `STARTED` before invoking MCP, performs network I/O
  outside a database transaction, and then moves only that attempt to one
  terminal status. An interrupted `STARTED` attempt remains visible and a new
  retry may be created.
- The API validates the tool result, including echoed identifiers and bounded
  content, before persistence. It never fabricates observations from missing,
  malformed, or contradictory data.
- Persist only the validated application-owned representation. Do not persist
  arbitrary unbounded MCP payloads, raw stack traces, tenant secrets, or
  sensitive data.

### Investigation workspace

- The workspace adds a clearly labeled `Observed evidence` section separate
  from any future AI inference or recommendation.
- It provides independent loading, not-collected, collecting, available,
  partial, empty, unavailable, timed-out, malformed, and API-error states.
- Available evidence shows the source system, tool name, status, retrieval
  timestamp, tool-call ID, observed window, and structured error observations.
- Partial and unavailable states remain visible with their safe explanation.
  Retrying does not remove previous attempts.
- While a POST is pending, the action is disabled to prevent accidental repeat
  interaction. The UI still treats server-side attempts as append-oriented
  history rather than assuming client-side duplicate prevention is an audit
  boundary.
- The workspace explicitly states that the displayed data is observed
  synthetic evidence and that no inference or recommendation has been
  generated.
- Existing queue, incident detail, start/resume navigation, investigation
  metadata, not-found behavior, keyboard access, and responsive behavior remain
  intact as regression baselines. They receive no new presentation behavior.

### Local runtime

- Add the synchronous Spring AI MCP client starter to the copilot API and use
  a named Streamable HTTP connection with a configurable base URL and request
  timeout.
- Invoke the known tool deterministically through a narrow application gateway;
  do not involve a chat model or allow a model to choose the tool.
- Extend the Windows local launcher to preflight, start, and health-check the
  operations MCP server before the copilot API. Keep Docker Compose limited to
  required PostgreSQL infrastructure.
- The unauthenticated MCP endpoint remains local-development-only. This task
  does not claim that it is safe for public or AWS exposure.

## In scope

- One deterministic `getRecentServiceErrors` MCP tool and readable synthetic
  fixtures.
- Actual MCP tool discovery and invocation contract tests over Streamable HTTP.
- A synchronous copilot MCP client gateway with bounded timeout and explicit
  status mapping.
- Flyway V4 and tenant-safe, append-oriented evidence-attempt persistence.
- Tenant-scoped evidence collection and history APIs.
- Observed-evidence workspace states, retry behavior, and provenance display.
- Styles and markup strictly local to the new workspace evidence section, plus
  reuse of existing shared styles without changing their existing behavior.
- Local startup configuration and launcher support for the MCP server.
- Focused, full-suite, database, protocol, responsive, and manual verification.
- Factual documentation updates after implementation and verification.

## Out of scope

- Additional MCP tools or incident families.
- Automatic collection during investigation start or workspace GET.
- Bedrock configuration, LLM calls, prompt templates, or report schemas.
- Markdown knowledge ingestion, embeddings, pgvector retrieval, or retrieval
  ranking.
- Probable cause, inference, recommendation, report generation, or evidence
  citations inside a report.
- `AWAITING_REVIEW`, `APPROVED`, or `REJECTED` states and human decisions.
- A general audit-event table or audit-timeline UI.
- Authentication, MCP endpoint security, AWS infrastructure, or deployment.
- Adding incident title, severity, type, alert ID, or breadcrumbs to the
  investigation metadata contract.
- Any additional polish or redesign of the queue, incident detail, application
  shell, existing investigation metadata, timestamps, controls, spacing,
  status pills, labels, or existing synthetic incident fixtures.
- Changes to Flyway V1, V2, or V3.
- New infrastructure such as Kafka, Redis, Kubernetes, queues, or schedulers.

## Constraints

- Synthetic data only. The tool is read-only and cannot process payments or
  execute remediation.
- Preserve observed facts separately from future AI inference.
- Missing, partial, unavailable, timed-out, and malformed evidence must remain
  explicit and must never produce fabricated observations.
- Carry tenant identity through tool invocation, persistence, and retrieval.
- Preserve correlation, investigation, evidence, and tool-call identifiers
  needed for auditability without exposing tenant or persistence metadata in
  public responses.
- Do not hold a PostgreSQL transaction open across an MCP network call.
- Validate all external input and MCP output at their boundaries.
- Keep the operator console, copilot API, and operations MCP server independently
  buildable and deployable.
- Follow red-green-refactor one behavior at a time.
- Do not add a dependency that is not required by this slice.
- No backend or frontend test may be skipped for completion.
- Archived polishing acceptance criteria are regression baselines, not work
  items. Passing them does not authorize related production changes.

## Acceptance criteria

- [ ] The MCP server lists exactly one application tool named
      `getRecentServiceErrors` with the approved, stable input schema.
- [ ] A known synthetic scenario returns deterministic structured service-error
      observations with source and retrieval metadata.
- [ ] Available-empty, partial, not-found, unavailable, timeout, malformed, and
      invalid-input cases are distinguishable and contract-tested.
- [ ] A tenant-owned investigation can create a service-error evidence
      collection attempt through the copilot API.
- [ ] The copilot API records `STARTED` before tool invocation and never holds a
      database transaction open across the network call.
- [ ] Every terminal result preserves the evidence ID, tool-call ID,
      investigation correlation ID, source, scenario reference, timestamps,
      schema version, and explicit status.
- [ ] MCP output with mismatched identifiers, invalid status, invalid fields,
      or excessive content is stored as `MALFORMED` without fabricated
      observations.
- [ ] An available response with no errors is preserved as successful negative
      evidence rather than missing evidence.
- [ ] Multiple attempts are append-oriented and returned newest first; a retry
      never overwrites an earlier result.
- [ ] An interrupted `STARTED` attempt remains visible and does not prevent a
      later retry.
- [ ] PostgreSQL prevents evidence from being associated with an investigation
      owned by another tenant.
- [ ] Cross-tenant and nonexistent investigations produce indistinguishable
      structured `404` responses and cause no tool call or persistence.
- [ ] Malformed tenant and investigation identifiers produce structured `400`
      responses without a tool call or persistence.
- [ ] The workspace exposes collection, retry, history, loading, empty,
      partial, unavailable, timeout, malformed, and API-error behavior without
      hiding prior attempts.
- [ ] Observed evidence visibly includes provenance and is clearly separated
      from absent AI inference and recommendation.
- [ ] Collection controls are pending-safe, keyboard accessible, and usable at
      390 CSS pixels without horizontal page overflow.
- [ ] Existing alert intake, queue, incident detail, investigation start/resume,
      direct workspace navigation, and tenant-safe lookup behavior remain
      unchanged.
- [ ] No queue, incident-detail, application-shell, existing workspace-metadata,
      or existing synthetic incident-fixture presentation change appears in the
      production diff.
- [ ] Focused tests fail for the intended reason before each production
      behavior and pass afterward.
- [ ] Full backend, PostgreSQL, MCP protocol, frontend, formatting, build,
      diff, and manual live verification pass with no skipped tests or new
      warnings.
- [ ] The final diff contains no secrets, real payment data, generated output,
      unrelated refactoring, or edits to V1-V3.

## Test plan

Write and run the smallest focused test before each corresponding production
change. Record the expected red and green evidence in Progress notes.

### Operations MCP server

- `listsGetRecentServiceErrorsWithStableSchema`
- `returnsDeterministicErrorsForKnownScenario`
- `returnsAvailableEmptyEvidenceWithoutReportingMissingSource`
- `distinguishesPartialNotFoundUnavailableAndTimeout`
- `rejectsInvalidToolArguments`
- `detectsMalformedFixtureData`

At least one test must boot the real server on a random port and use an MCP
client to discover and invoke the tool over Streamable HTTP.

### Copilot workflow and HTTP

- `collectsServiceErrorsForTenantOwnedInvestigation`
- `persistsStartedAttemptBeforeCallingSource`
- `mapsEveryToolAndTransportOutcomeWithoutFabrication`
- `marksMismatchedOrInvalidToolResultsMalformed`
- `doesNotCallMcpForCrossTenantInvestigation`
- `returnsCreatedForRecordedUnavailableAttempt`
- `returnsEvidenceAttemptsNewestFirst`
- `rejectsMalformedEvidenceCollectionIdentifiers`
- `returnsInvestigationNotFoundWithoutTenantLeakage`
- `doesNotChangeInvestigationOrIncidentState`

### PostgreSQL and transaction boundary

- `persistsEvidenceAttemptWithTenantAndProvenance`
- `preventsCrossTenantEvidenceAssociation`
- `preservesEveryRetryAttempt`
- `retainsStartedAttemptWhenCollectionDoesNotComplete`
- `updatesOnlyTheMatchingStartedAttempt`
- `doesNotHoldDatabaseTransactionAcrossMcpCall`

Tenant integrity and transaction behavior must be proven with PostgreSQL through
Testcontainers; mock-only evidence is insufficient.

### Angular API and workspace

- `requestsEvidenceHistoryForConfiguredTenant`
- `startsEvidenceCollectionForConfiguredTenant`
- `rendersAvailableEvidenceWithProvenance`
- `rendersAvailableEmptyEvidenceAsSuccessfulObservation`
- `distinguishesPartialUnavailableTimedOutAndMalformedEvidence`
- `disablesCollectionWhilePending`
- `retriesWithoutHidingPreviousAttempts`
- `showsRetryableEvidenceApiFailure`
- `keepsObservedEvidenceSeparateFromInferenceAndRecommendation`

Re-run all existing routing, loading, not-found, start/resume, retry, and
responsive tests.

## Expected approach

1. Obtain owner approval for this contract and move the task to `Ready` before
   editing executable files.
2. Implement producer contract tests and deterministic fixtures before the MCP
   tool implementation.
3. Prove actual tool discovery and invocation over Streamable HTTP.
4. Add PostgreSQL tests before Flyway V4 or evidence repositories.
5. Add a narrow copilot MCP gateway and test validation and status mapping
   independently from workflow orchestration.
6. Implement evidence-attempt persistence and the insert-call-update sequence
   with no transaction spanning the network call.
7. Add tenant-scoped HTTP tests before controllers and response DTOs.
8. Add Angular API and workspace tests before implementing only the new
   workspace evidence section.
9. Extend the local launcher and exercise all three applications together.
10. Run focused tests, then every broader verification command and manual
    responsive scenario.
11. Review the diff for tenant leakage, fabricated evidence, hidden failures,
    unbounded payloads, transaction misuse, secrets, generated output, and
    unrelated changes.
12. Update task and project documentation with factual completion evidence only.

## Likely files or components

- `backend/operations-mcp-server/src/main/java/.../mcp/`
- `backend/operations-mcp-server/src/main/resources/fixtures/`
- `backend/operations-mcp-server/src/test/java/.../mcp/`
- `backend/copilot-api/pom.xml`
- `backend/copilot-api/src/main/resources/application.yml`
- `backend/copilot-api/src/main/resources/db/migration/V4__*.sql`
- `backend/copilot-api/src/main/java/.../incident/`
- `backend/copilot-api/src/test/java/.../incident/`
- `frontend/operator-console/src/app/features/investigation-workspace/`
- `.env.example`
- `scripts/start-local.ps1`
- `README.md`
- `docs/agent/STATUS.md`
- `docs/agent/tasks/current.md`

Do not modify Dockerfiles, Docker Compose service scope, infrastructure, AWS
configuration, existing Flyway migrations, alert-queue production files,
incident-detail production files, application-shell production files, or
unrelated incident features. A required exception must be approved by the owner
as a task-contract change before implementation continues.

## Validation commands

Run from the repository root unless a command changes directory:

```bash
mvn -pl backend/operations-mcp-server test
mvn -pl backend/copilot-api test
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
unavailable, the task remains incomplete.

## Manual verification

1. Start PostgreSQL, the operations MCP server, the copilot API, and the
   operator console through the updated local workflow.
2. Confirm the MCP client discovers only the approved tool and invokes it over
   Streamable HTTP.
3. Open a tenant-owned investigation and collect available service-error
   evidence.
4. Confirm the workspace shows source, status, tool-call ID, retrieval time,
   observed window, and structured observations without an AI conclusion.
5. Exercise available-empty, partial, not-found, unavailable, timed-out, and
   malformed scenarios.
6. Retry a failed collection and confirm both attempts remain visible.
7. Confirm cross-tenant and invalid requests create no evidence and make no
   tool call.
8. Confirm alert intake, queue sorting, detail, start/resume, and direct
   workspace refresh still work.
9. Inspect workspace states at desktop and 390 CSS pixels for keyboard access,
   horizontal overflow, browser errors, and warnings.
10. Inspect persisted records and structured logs for tenant, investigation,
    evidence, correlation, and tool-call provenance without sensitive payloads.

## Decisions needed

None. The owner approved the explicit collection action, use of
`externalAlertId` as the opaque synthetic scenario reference, append-oriented
retry history, exact evidence statuses, and strict separation from the
completed presentation-polish task.

## Progress notes

- 2026-08-28: Repository review identified the first deterministic read-only
  MCP evidence tool as the highest-value next vertical slice. The owner asked
  for the proposed contract to be recorded for review; no executable change has
  begun.
- 2026-08-28: Clarified that the completed presentation-polish task is closed,
  that existing UI requirements are regression-only, and that this task may add
  visible behavior only inside the new investigation evidence section.
- 2026-08-28: Owner approved the strict contract and authorized implementation.
  Task moved to `In Progress` on branch
  `codex/mcp-service-error-evidence`; all behavioral contract sections are now
  locked.
- 2026-08-28: MCP producer red phase first failed test compilation for the
  missing tool, result, status, scenario, and repository types. The minimum
  known-scenario behavior then passed its focused test.
- 2026-08-28: Fixture-repository red phase failed compilation for the missing
  classpath repository. Tenant-scoped available, available-empty, partial,
  unavailable, timeout, and malformed synthetic fixtures were then added; the
  focused fixture and tool tests passed 7/7.
- 2026-08-28: Live MCP red phase discovered the tool and exact four-field input
  schema but failed because the generated output schema rejected a nullable
  status detail. Nullable fields were made optional and omitted when absent;
  a second focused red check caught incorrect default tool safety hints. The
  final Streamable HTTP contract marks the tool read-only, non-destructive,
  idempotent, and closed-world. The operations MCP module passed 9/9 tests with
  zero skips.

## Completion evidence

- Red-phase evidence:
- Green-phase evidence:
- Acceptance-criteria coverage:
- Full verification:
- Manual verification:
- Documentation updated:
- Remaining limitations:
