# Task: Record the human decision and audit trail

Status: In Progress
Created: 2026-08-30
Owner: Christopher Guzowski

## Goal

Give a payment operations analyst one explicit, attributable way to approve or
reject the exact proposed report under review, then keep the complete synthetic
incident history accessible as a tenant-scoped, immutable-style timeline.

The owner approved the five recommended contract decisions and explicitly
authorized implementation on 2026-08-30. This is now the active, locked
behavioral contract.

## User story

As a payment operations analyst, I want to record my approval or rejection with
my reason and review the complete incident timeline, so the final outcome is a
human decision with clear provenance rather than an action attributed to the AI.

## Context

P2 persists one exact, schema-valid `AVAILABLE` report as the review candidate
and atomically moves the incident to `AWAITING_REVIEW`. It preserves every
evidence, knowledge-retrieval, and report attempt needed to explain that
candidate. It intentionally contains no human-decision controls or unified
timeline.

P3 closes that workflow. The current suggested terminal lifecycle is:

```text
NEW -> INVESTIGATING -> AWAITING_REVIEW -> APPROVED
                                     \-> REJECTED
```

The accepted report remains an AI-generated proposed artifact. A separate human
decision references it; approval does not make model output observed fact and
does not execute the recommendation.

## Chosen contract

### Human-decision command

- Add `POST /api/investigations/{investigationId}/decisions` with the existing
  synthetic tenant and operator headers.
- Accept exactly one JSON object containing:
  - `outcome`: `APPROVED` or `REJECTED`.
  - `reason`: trimmed, nonblank operator text of at most 1,000 characters.
- Do not accept tenant, operator, incident, report, model, evidence, knowledge,
  or lifecycle identifiers from the body. The application resolves the single
  tenant-owned `AVAILABLE` review candidate on the server.
- A newly recorded decision returns `201 Created`. An exact replay by the same
  operator with the same outcome and normalized reason returns the existing
  decision as `200 OK`; it does not append another record.
- A different second decision, an incident outside `AWAITING_REVIEW`, a missing
  `AVAILABLE` report, or a stale concurrent submission returns `409 Conflict`.
- Invalid identifiers, outcome, reason, JSON, or unknown fields return `400`.
  A nonexistent or cross-tenant investigation returns the same `404` response.
- `GET /api/investigations/{investigationId}/decisions` returns zero or one
  decision for direct-route refresh and independently loading UI state.

### Decision persistence and lifecycle

- Add Flyway V8 with an append-only `human_decision` table containing the
  decision, tenant, incident, investigation, investigation correlation, exact
  report-attempt, deciding operator, normalized reason, outcome, and UTC time.
- Enforce tenant-safe foreign keys and uniqueness for one decision per
  investigation and per review candidate.
- Persist the decision and conditionally transition the incident from
  `AWAITING_REVIEW` to the matching `APPROVED` or `REJECTED` status in one
  transaction. A failed transition rolls the decision back.
- Keep incident lifecycle mutation owned by `incident` behind a narrow decision
  lifecycle port. Decision persistence owns and queries only decision storage.
- Preserve the report attempt and report document unchanged. The decision
  references the report; it does not mutate report status or content.
- Treat both terminal outcomes as final for this MVP. Reopening, replacing a
  report after rejection, amending, or overturning a decision requires a future
  explicitly approved workflow.
- Do not log the reason, request body, report content, prompt, model output, or
  arbitrary provider metadata.

### Operator attribution gap

- Extend the existing evidence-collection and knowledge-retrieval POST paths to
  require `X-Synthetic-Operator-Id`, matching investigation start, report
  generation, and the proposed decision command.
- Add nullable `requested_by` columns to the existing evidence and retrieval
  attempt tables in V8. New application writes always populate them; historical
  rows remain nullable so the migration does not fabricate an actor.
- Extend the frontend request-context interceptor to attach the operator header
  to those two existing mutation families.
- The timeline labels a historical null actor as `UNATTRIBUTED`. It must not
  relabel such an event as a system or operator action without evidence.

### Audit timeline

- Add `GET /api/investigations/{investigationId}/timeline`, requiring only the
  tenant header and preserving indistinguishable cross-tenant `404` behavior.
- Return application-owned, bounded timeline events oldest first with a stable
  tie-breaker. Each event contains a stable source identifier, event type,
  occurred/completed times where applicable, actor kind and identifier when
  known, terminal status or outcome, investigation correlation identifier, and
  any resulting incident status.
- Include every authoritative record for the incident path:
  - synthetic alert receipt (`NEW`);
  - investigation start and operator (`INVESTIGATING`);
  - every evidence collection attempt, including tool-call identifier and all
    terminal or interrupted statuses;
  - every approved-knowledge retrieval attempt and terminal status;
  - every report-generation attempt, requesting operator, model/schema/prompt
    versions, disposition when available, and `AWAITING_REVIEW` transition;
  - the human decision, reason, operator, and terminal incident status.
- Represent one long-running attempt as one timeline event with start and
  completion fields instead of fabricating two stored audit records.
- Build the timeline as a read projection over authoritative feature-owned
  records. Do not add a second generic audit-event table or copy arbitrary JSON.
- Each source feature publishes a narrow tenant-scoped timeline snapshot port.
  The new `audit` feature composes those ports and never queries another
  feature's tables directly.
- Use fixed enums and safe application-owned descriptions. Do not expose raw
  evidence content, knowledge text, report content, prompts, malformed model
  output, stack traces, credentials, or unbounded status detail in the timeline.

### Operator console

- Add an independently loading decision panel after the report panel. It is
  available only for a review candidate or an already terminal decision.
- Use one explicit form with an Approve/Reject choice, required reason, and a
  `Record final decision` submit action. Do not use a one-click terminal action.
- State clearly that the report is advisory, the operator owns the decision,
  recording the decision is final in this MVP, and no recommendation will be
  executed.
- While submission is pending, preserve the report and prior history, prevent a
  duplicate local submit, and recover from `400`, `404`, `409`, and other HTTP
  errors. On conflict, reload the authoritative decision and investigation.
- After success or exact replay, show a read-only decision card with outcome,
  exact reason, operator, time, and report-attempt identifier. Do not offer edit,
  delete, undo, or execute controls.
- Add an independently loading audit-timeline panel after the decision panel.
  It supports loading, empty, success, not-found, and error states and refreshes
  after a decision is recorded.
- Render the timeline as semantic chronological content with readable UTC times,
  actor attribution, status/outcome text, and stable record identifiers. Do not
  rely on color alone.

### Completed-incident discovery

- Extend `GET /api/incidents` with one validated `view` value:
  `active` (default) or `completed`.
- `active` preserves the current `NEW`, `INVESTIGATING`, and `AWAITING_REVIEW`
  membership. `completed` returns only `APPROVED` and `REJECTED` incidents.
- Add Active and Completed views to the existing incident work queue rather
  than creating a second queue or route. Preserve the selected sort within a
  view and keep active work as the default.
- Completed rows retain their investigation link with a `View decision and
  timeline` label. Incident detail and direct investigation routes remain
  available after the terminal transition.

### Feature ownership

- Add a `decision` feature that owns the decision domain, persistence, command,
  history API, and response contracts. It may depend only on published report
  review-candidate and incident decision-lifecycle ports.
- Add an `audit` feature that owns timeline composition and its read API. It may
  depend only on published timeline snapshot ports from incident, evidence,
  knowledge retrieval, report, and decision.
- Upstream features do not depend on decision or audit implementation types.
- Architecture tests enforce those directions and ensure the decision
  persistence adapter does not query or decode upstream feature tables.

## In scope

- `APPROVED` and `REJECTED` incident states.
- One immutable and attributable decision for the exact review candidate.
- Required reason capture for both outcomes.
- Exact-replay idempotency and conflicting/concurrent-decision behavior.
- Atomic decision persistence and terminal incident transition.
- Operator attribution for new evidence and knowledge-retrieval attempts while
  preserving missing attribution on historical rows.
- Tenant-scoped timeline composition across every existing attempt history.
- Active/completed discovery in the existing work-queue surface.
- Decision and timeline panels with responsive and accessible terminal states.
- Flyway V8, unit, HTTP, PostgreSQL, concurrency, architecture, Angular, and
  responsive verification.
- Factual updates to architecture, domain, roadmap, status, README, and the
  activated task after behavior is implemented and verified.

## Out of scope

- Authentication, authorization, roles, permissions, or production identity.
- Editing, deleting, undoing, superseding, or reopening a human decision.
- Regenerating or replacing a report after a terminal decision.
- Treating approval as verification that every report claim is true.
- Executing a recommendation, remediation, MCP mutation, or money movement.
- A generic event bus, event-sourcing framework, outbox, Kafka, or second audit
  write model.
- New evidence tools, incident families, model calls, prompts, schemas,
  retrieval behavior, or knowledge content.
- AWS infrastructure or Bedrock deployment work.
- A full browser end-to-end automation framework; P4 owns closed-loop hardening.
- Editing Flyway V1-V7 or backfilling an invented operator onto existing rows.

## Constraints

- Follow red-green-refactor one behavior at a time.
- Carry `tenant_id` through decision, lifecycle, timeline, and queue boundaries.
- Preserve cross-tenant indistinguishable not-found behavior.
- Human input is the only authority for the decision outcome. No report/model
  field may select, trigger, or override it.
- Keep reports, decisions, incident status, and timeline projections distinct.
- Preserve missing, failed, interrupted, and contradictory history.
- Use UTC instants and stable identifiers at every boundary.
- Validate and bound all external input; render reasons as text, not HTML.
- Keep every deployable independently buildable and automated tests
  deterministic, network-free, and independent of Ollama.

## Acceptance criteria

- [x] An owned `AWAITING_REVIEW` investigation with one `AVAILABLE` report can
      record one `APPROVED` or `REJECTED` decision with a required reason and
      synthetic operator identity.
- [x] The server resolves and stores the exact review candidate; the client
      cannot choose or inject report, tenant, operator, model, or source IDs.
- [x] Decision insert and incident transition commit atomically to the matching
      terminal status, and a failed transition leaves neither change committed.
- [x] An exact same-operator replay returns the existing decision without a
      second row; a different, stale, or concurrent decision returns a safe
      conflict and never changes the recorded outcome.
- [x] Invalid paths, headers, bodies, enums, blank/oversized reasons, unknown
      fields, cross-tenant IDs, and unmet workflow prerequisites have explicit
      `400`, `404`, or `409` behavior.
- [x] The original report remains unchanged and no decision path calls a model,
      MCP tool, retrieval, or operational action.
- [x] New evidence and retrieval attempts store the requesting operator;
      historical missing attribution remains visible as `UNATTRIBUTED`.
- [x] The timeline returns every alert, investigation, evidence, retrieval,
      report, and decision record in deterministic chronological order with
      stable provenance and all failure/retry states preserved.
- [x] Timeline composition uses tenant-scoped published ports and no audit or
      decision persistence adapter queries another feature's storage.
- [x] Timeline responses omit raw prompts, model payloads, evidence/knowledge
      content, stack traces, secrets, and unbounded detail.
- [x] The decision form preserves report/history context, prevents accidental
      one-click submission, and reaches a visible terminal UI state for success,
      exact replay, validation, not-found, conflict, and other HTTP failures.
- [x] Direct refresh shows the stored terminal decision and timeline without
      offering edit, delete, undo, regeneration, or execution controls.
- [x] Active work remains the default queue view; approved and rejected
      incidents remain discoverable in the completed view and open the existing
      investigation route.
- [ ] Backend, PostgreSQL, HTTP, concurrency, architecture, frontend,
      formatting, build, Compose, diff, desktop, and 390-CSS-pixel checks pass
      with zero skipped tests.

## Test plan

### Domain and application

- `requiresBoundedReasonForApprovalAndRejection`
- `recordsDecisionAgainstServerResolvedAvailableReport`
- `approvesAwaitingReviewIncidentAtomically`
- `rejectsAwaitingReviewIncidentAtomically`
- `rollsBackDecisionWhenLifecycleTransitionFails`
- `returnsExistingDecisionForExactSameOperatorReplay`
- `rejectsDifferentOrStaleSecondDecision`
- `doesNotCallModelRetrievalMcpOrOperationalAction`

### PostgreSQL and concurrency

- `appliesFlywayV8AndPreservesExistingAttemptHistory`
- `persistsTenantSafeDecisionReportAndCorrelationReferences`
- `enforcesOneDecisionPerInvestigationAndReportAttempt`
- `commitsDecisionAndTerminalStateInOneTransaction`
- `allowsExactlyOneOfTwoConcurrentConflictingDecisions`
- `doesNotExposeOrMutateCrossTenantDecision`
- `storesNewAttemptActorsAndPreservesHistoricalNullActors`
- `decisionPersistenceOwnsOnlyDecisionStorage`

### HTTP and request context

- `createsApprovalAndRejectionWithSyntheticOperatorContext`
- `returnsExistingDecisionForExactReplay`
- `returnsDecisionForDirectRefresh`
- `rejectsClientSuppliedOrUnknownDecisionFields`
- `rejectsMissingMalformedOrOversizedDecisionInput`
- `returnsConflictOutsideAwaitingReviewOrWithoutAvailableReport`
- `keepsCrossTenantAndMissingInvestigationIndistinguishable`
- `requiresOperatorForEvidenceRetrievalAndDecisionMutations`
- `listsActiveAndCompletedIncidentsThroughValidatedViews`

### Timeline and architecture

- `composesEveryAuthoritativeRecordOldestFirstWithStableTieBreaker`
- `preservesEveryFailureRetryAndInterruptedAttempt`
- `showsKnownOperatorsAndExplicitUnattributedHistory`
- `mapsLifecycleMilestonesWithoutFabricatingStoredEvents`
- `omitsRawContentPromptsPayloadsSecretsAndUnboundedDetail`
- `isolatesTimelineByTenant`
- ArchUnit rules for decision/audit ownership and published-port-only
  dependencies.

### Angular

- `submitsExplicitOutcomeAndRequiredReasonWithoutClientOwnedIds`
- `preventsBlankOversizedAndDuplicatePendingSubmission`
- `rendersStoredApprovalAndRejectionAsReadOnlyHumanDecisions`
- `recoversFromValidationNotFoundConflictAndOtherHttpErrors`
- `preservesReportAndHistoryThroughoutDecisionSubmission`
- `loadsTimelineIndependentlyAndRendersEveryEventKind`
- `rendersUnattributedEventsWithoutInventingAnActor`
- `refreshesInvestigationAndTimelineAfterDecision`
- `keepsTerminalIncidentsDiscoverableInCompletedView`
- `usesNoEditUndoRegenerateOrExecuteControlsAfterDecision`
- Interceptor tests for the expanded operator-attributed mutation set.

### Manual verification

- Exercise approval, rejection, exact replay, conflicting second submission,
  stale route, cross-tenant access, and historical unattributed events.
- Verify direct refresh and Active/Completed navigation at 1280x720 and 390x844.
- Verify keyboard-only form operation, visible focus, semantic status/error
  announcements, readable actor/outcome distinctions, no horizontal overflow,
  no off-viewport controls, and no browser warnings or errors.
- If Ollama and the pinned models are locally available, exercise the full live
  alert-to-decision path. This is additive; deterministic completion does not
  depend on a live model.

## Expected implementation process

1. Obtain owner decisions below and accept a focused ADR for decision ownership,
   terminal lifecycle, and the projected timeline approach.
2. Preserve the completed current task under `tasks/completed/`, promote this
   proposal to `tasks/current.md`, and lock its behavioral contract before any
   executable change.
3. Add failing decision-domain and service tests for outcome/reason rules,
   exact-candidate binding, replay semantics, conflicts, and atomic lifecycle.
4. Add failing architecture tests and the narrow incident/report ports; keep
   production behavior unchanged until the dependency rules are red for the
   intended missing feature.
5. Add failing PostgreSQL/Flyway V8 tests, then the minimal decision table,
   attribution columns, repositories, and transactional lifecycle integration.
6. Add failing request-context and HTTP tests, then implement decision history,
   command, active/completed queue view, and operator attribution changes.
7. Add failing timeline snapshot, composition, safety, ordering, and tenant
   tests one source feature at a time, then implement each published port and the
   read-only audit API.
8. Add failing Angular API/interceptor and decision-panel tests before the form,
   then add timeline-panel tests before its UI and refresh behavior.
9. Add queue/detail/workspace regressions for both terminal states and direct
   refresh, then implement the Active/Completed discovery and final labels.
10. Run focused backend, PostgreSQL, HTTP, architecture, and frontend tests;
    refactor only with those tests green.
11. Run the authoritative repository gate and perform desktop/mobile manual
    verification, including concurrency/conflict and cross-tenant scenarios.
12. Review the final diff for scope, tenant leaks, mutable decisions, fabricated
    actors, unsafe reason logging/rendering, dual-write audit state, secrets,
    generated output, and unrelated cleanup. Update factual documentation and
    acceptance evidence only after each verification has passed.

## Likely files or components

- `backend/copilot-api/src/main/resources/db/migration/V8__*.sql`
- `backend/copilot-api/src/main/java/.../decision/`
- `backend/copilot-api/src/main/java/.../audit/`
- Published timeline/read-candidate ports under `incident`, `evidence`,
  `knowledge.retrieval`, `report`, and `decision`
- Incident lifecycle, work-queue, request-context, and architecture tests
- PostgreSQL/Testcontainers decision and timeline integration tests
- `frontend/operator-console/src/app/core/models/incident.ts`
- `frontend/operator-console/src/app/core/http/synthetic-request-context.interceptor.ts`
- `frontend/operator-console/src/app/features/alert-queue/`
- `frontend/operator-console/src/app/features/incident-detail/`
- `frontend/operator-console/src/app/features/investigation-workspace/decision-panel/`
- `frontend/operator-console/src/app/features/investigation-workspace/audit-timeline-panel/`
- `docs/agent/decisions/ADR-0008-human-decision-and-audit-timeline.md`
- `README.md`, `docs/agent/ARCHITECTURE.md`, `docs/agent/DOMAIN.md`,
  `docs/agent/ROADMAP.md`, `docs/agent/STATUS.md`, and the activated task

Do not modify the operations MCP server, immutable MCP v1 artifact, approved
knowledge content, report schema/prompt, provider adapters, AWS infrastructure,
or Flyway V1-V7 for this slice.

## Validation commands

```powershell
./mvnw.cmd -pl backend/copilot-api -Dtest=FeatureArchitectureTest,*Decision*,*Timeline*,*RequestContext*,*IncidentWorkQueue* test
./verify.ps1 -Scope Backend
./verify.ps1 -Scope Frontend
./verify.ps1 -Scope Repository
./verify.ps1
```

The unscoped command is the authoritative completion gate. Focused commands are
development feedback and do not replace it.

## Decisions needed

None. On 2026-08-30 the owner authorized implementation and thereby accepted
all five recommended choices: required bounded reasons for both outcomes;
final non-amendable terminal states; a projected timeline over authoritative
feature records; honest operator attribution with historical nulls preserved;
and Active/Completed views in the existing work queue.

## Progress notes

- 2026-08-30: Prepared this P3 proposal from the implemented P2 report,
  incident lifecycle, persistence, request-context, queue, and panel contracts.
  No executable implementation or active-task change has begun.
- 2026-08-30: Review found that evidence and knowledge-retrieval mutations are
  currently operator-triggered but do not store operator identity. The proposal
  preserves historical absence explicitly and offers attribution for new rows.
- 2026-08-30: The owner authorized implementation, accepted ADR-0008 and all
  recommended contract choices, preserved the completed bounded-generation
  task, and promoted this task as the active locked contract.
- 2026-08-30: Completed red-green cycles for decision validation and service
  behavior, transactional persistence/concurrency fixtures, request-context and
  HTTP behavior, timeline composition/safety, queue views, Angular decision and
  timeline panels, workspace refresh, and architecture boundaries. Each
  production behavior began with an intended missing-symbol or behavior failure
  before its focused test passed.
- 2026-08-30: Added Flyway V8, one immutable human decision bound to the exact
  tenant-owned report attempt, atomic terminal lifecycle integration, honest
  nullable historical attempt actors, feature-owned timeline ports, a projected
  audit API, completed-queue discovery, and independently loading responsive
  decision/timeline panels.
- 2026-08-30: The focused deterministic backend suite and all 14 architecture
  rules pass. The full Maven build compiles and packages both deployables and
  executes 136 copilot plus 9 MCP tests without failures, but Docker
  unavailability skips 46 PostgreSQL tests; the backend zero-skip gate therefore
  remains correctly red.
- 2026-08-30: Frontend repository verification passed 75/75 tests with zero
  skips, npm audit with zero vulnerabilities, Prettier, and the 386.21 kB
  production build. Repository verification passed its PowerShell contract,
  Docker Compose validation, and `git diff --check`.
- 2026-08-30: The production application applied Flyway V8 to native PostgreSQL
  18.3 and started with the complete decision/audit graph. Native HTTP/database
  smokes passed approval, rejection, exact replay, safe conflicts, a true
  two-request race with exactly one winner, a forced stale-transition rollback,
  exact report binding, unchanged report content, operator attribution, and
  tenant-isolated direct reads.
- 2026-08-30: Native timeline verification matched 30/30 authoritative records
  oldest-first for an existing retry-heavy investigation: 12 evidence, 8
  retrieval, and 8 report attempts, including 28 failure/unavailable states and
  20 historical `UNATTRIBUTED` actors. Cross-tenant timeline and completed-queue
  access excluded the fixture.
- 2026-08-30: Live browser checks passed approval, terminal direct refresh, and
  Active/Completed discovery at 1280x720 and 390x844 with semantic status/error
  regions, visible focus, no edit/undo/regenerate/execute controls, no horizontal
  overflow, no off-viewport decision controls, and no clean-run browser warnings
  or errors. The automation surface could not drive Tab/Space in the live form,
  so a final hands-on keyboard-only submission remains outstanding.

## Completion evidence

- Red-phase evidence: Focused domain, service, persistence, HTTP,
  request-context, timeline, architecture, interceptor, queue, decision-panel,
  timeline-panel, and workspace tests failed for their intended missing behavior
  before production changes.
- Green-phase evidence: Focused deterministic backend and architecture tests
  pass; 75/75 Angular tests pass with zero skips; frontend and repository scopes
  pass. Native PostgreSQL 18.3 migration, transaction, rollback, concurrency,
  tenant, timeline, and queue smokes pass.
- Acceptance-criteria coverage: Every behavioral criterion is checked through
  passing deterministic tests plus native PostgreSQL/HTTP/browser evidence. The
  aggregate zero-skip completion criterion remains unchecked until Docker-backed
  Testcontainers execute.
- Full verification: `./verify.ps1 -Scope Backend` fails only at
  `backend-no-skips` after 136/182 copilot tests and 9/9 MCP tests pass; 46
  PostgreSQL tests are skipped because no valid Docker environment is available.
  The unscoped authoritative gate is therefore not recorded as passing.
- Manual verification: Native PostgreSQL approval, rejection, replay, conflict,
  concurrent conflict, stale-transition rollback, cross-tenant access,
  historical unattributed retries, direct refresh, and Active/Completed
  navigation pass. Desktop and 390-CSS-pixel checks pass without overflow,
  off-viewport controls, warnings, or errors. A hands-on keyboard-only form
  submission remains because browser automation could not drive Tab/Space.
- Documentation updated: ADR-0008, architecture, domain, roadmap, status,
  README, completed-task archive, and this active evidence record match the
  implemented working tree and verification state.
- Remaining limitations: The authoritative gate is still red because Docker
  Desktop 4.87.0 repeatedly fails while creating local Unix-socket reparse
  points, causing 46 PostgreSQL tests to skip. A hands-on keyboard-only form
  smoke also remains. Recoverable runtime-directory backups remain at
  `run.stale-20260830-1427` and `docker-secrets-engine.stale-20260830-1436` in
  the user's Docker app-data directories.
