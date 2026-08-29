# Task: Generate a reviewable evidence-linked incident report

Status: Complete
Created: 2026-08-29
Owner: Christopher Guzowski

## Goal

Give a payment operations analyst one explicit, auditable action that turns the
persisted incident, observed evidence, and approved-knowledge retrieval snapshot
into a schema-valid proposed incident report whose claims can be reviewed at
their sources before any human decision is recorded.

The owner authorized implementation on 2026-08-29. This is now the active,
locked behavioral contract.

## User story

As a payment operations analyst, I want to generate and inspect a proposed
incident report with evidence references beside every conclusion so that I can
evaluate the copilot's reasoning without mistaking AI inference for observed
fact or approved guidance.

## Context

The product now persists a tenant-scoped incident and investigation, append-only
service-error evidence attempts, and immutable approved-knowledge retrieval
snapshots. B01-B06 established the internal ports and UI composition boundaries
needed for a report feature to consume those inputs without querying another
feature's tables.

This is roadmap slice P2. It should prove whether the current evidence and
knowledge are sufficient for the selected decline-rate incident family before
the project adds more MCP tools. Human approval/rejection and the complete audit
timeline remain P3.

The accepted architectural contract and model choice are in
`docs/agent/decisions/ADR-0006-evidence-linked-report-generation.md`.

## Chosen contract

### Explicit trigger and HTTP behavior

- The workspace exposes an explicit `Generate proposed report` action. Loading
  or refreshing a route never invokes a model.
- `POST /api/investigations/{investigationId}/reports` has no body and requires
  the existing synthetic tenant and operator request-context headers.
- `GET /api/investigations/{investigationId}/reports` returns every recorded
  attempt newest first. Cross-tenant and nonexistent investigations remain
  indistinguishable `404` responses.
- A recorded terminal outcome returns `201 Created`, including a safe
  unavailable, timed-out, or malformed outcome. Invalid identifiers return
  `400`; unmet workflow prerequisites or concurrent generation return `409`
  without a model call or persisted attempt.
- Only one generation may be in progress for an investigation. A terminal
  failure may be retried while the incident remains `INVESTIGATING`; a
  successful report becomes the single review candidate and cannot be silently
  replaced before a later human decision.

### Prerequisites and exact input snapshot

- Generation requires an `INVESTIGATING` incident, at least one terminal
  evidence attempt, and at least one terminal knowledge-retrieval attempt. A
  route that has never attempted either prerequisite is not ready.
- Evidence publishes a report snapshot containing the newest terminal attempt,
  the newest applicable `AVAILABLE` or `PARTIAL` attempt, its exact normalized
  observations and provenance, and explicit missing/degraded status. A failed
  retry never erases an earlier applicable observation.
- Knowledge retrieval publishes the newest terminal retrieval snapshot only,
  including its exact status and selected persisted chunks. Report generation
  never reruns retrieval and never silently substitutes an older retrieval.
- A terminal unavailable, no-match, partial, or malformed prerequisite remains
  part of the prompt as an explicit limitation. It does not cause the
  application to fabricate context or add an ineligible chunk.
- The report feature composes tenant-scoped incident, evidence, and retrieval
  ports before persisting `STARTED`. Its persistence adapter owns only report
  tables and does not query incident, evidence, or knowledge tables.

### Application-owned report schema

- The top-level disposition is `PROPOSED` or `INSUFFICIENT_EVIDENCE`.
- The report separates a cited summary, observations, inferences, probable
  cause, confidence assessment, recommendation, contradictions, and evidence
  gaps. Probable cause and recommendation are absent when the available inputs
  do not support them.
- Every summary, observation, inference, probable-cause, confidence, and
  recommendation claim contains at least one evidence identifier from the
  exact input snapshot. Recommendation claims additionally reference at least
  one selected approved-knowledge chunk.
- Observations may cite evidence only. Inferences may cite evidence and selected
  knowledge. Knowledge is guidance for interpretation or action, not proof that
  an incident fact occurred.
- The application rejects unknown, cross-snapshot, cross-tenant, duplicate, or
  structurally invalid references. It resolves displayed provenance from
  persisted sources; model-supplied excerpts or source metadata are never
  trusted as citations.
- `INSUFFICIENT_EVIDENCE` requires low confidence and no probable cause or
  recommendation. Missing, unavailable, degraded, and contradictory inputs
  remain visible in the stored report and UI.
- The report is always labeled AI-generated, advisory, and not yet reviewed.
  No recommendation is executed automatically.

### Model, prompt, and generation lifecycle

- Use Amazon Bedrock Converse with the configurable default
  `${BEDROCK_CHAT_MODEL:global.amazon.nova-2-lite-v1:0}`.
- Use one immutable `report-v1` JSON Schema and one versioned prompt template.
  Nova 2 Lite does not support Bedrock native structured outputs, so the prompt
  includes the schema and requires one JSON object with no preamble. The
  application strictly parses the result and validates the JSON Schema,
  conditional rules, tenant/source membership, and citation semantics before
  persistence.
- Use deterministic, bounded inference settings: temperature `0`, no tool
  calls, no hidden model repair call, no model reasoning trace persisted, and
  an output budget sized to the bounded schema. Citations are application-owned
  source identifiers in the schema.
- Persist `STARTED` before the Bedrock call, perform network I/O outside a
  database transaction, and terminally update only that attempt.
- Terminal statuses are exactly `STARTED`, `AVAILABLE`, `UNAVAILABLE`,
  `TIMED_OUT`, and `MALFORMED`. Schema-valid output with invalid source
  references is `MALFORMED` and is never exposed as a proposed report.
- Automated tests use a deterministic report-model double. An explicitly
  invoked authorized smoke path verifies the real selected global model,
  prompt-guided JSON contract, strict application validation, and safe failure
  mapping without printing a prompt, model payload, credentials, or source
  content.

### Persistence, lifecycle, and operator experience

- Add a Flyway V6 migration for append-only report attempts, the validated
  report document, and tenant-safe normalized claim/source references.
- Persist report, attempt, tenant, investigation, correlation, and requesting
  operator identifiers; timestamps; exact evidence and retrieval snapshot
  identifiers; model/inference-profile identifier; inference settings; prompt
  and schema versions and hashes; validated report content; safe provider
  metadata when available; and safe terminal status detail.
- Do not persist credentials, arbitrary provider payloads, malformed raw model
  output, chain-of-thought, stack traces, or an unbounded rendered prompt.
- A successful validated report and the incident transition from
  `INVESTIGATING` to `AWAITING_REVIEW` commit atomically. The explicit operator
  generation command authorizes this workflow transition; model content never
  selects or directly mutates incident state.
- `AWAITING_REVIEW` remains in the tenant work queue and opens the existing
  investigation route with a `Review proposed report` action label.
- Add an independently loading report panel after approved knowledge. It shows
  generation history, every terminal state, claim type, confidence,
  disposition, limitations, and source references adjacent to each claim.
- The panel contains no approve/reject controls. It preserves keyboard access,
  visible focus, semantic status/error messaging, and usability at 390 CSS
  pixels without horizontal page overflow.

## In scope

- One report-generation feature inside the copilot API and its enforced package
  dependencies.
- Narrow tenant-scoped report input ports from incident, evidence, and
  knowledge retrieval.
- Versioned prompt and JSON Schema, prompt-guided JSON generation, strict
  application validation, and deterministic model double.
- Append-only report-generation persistence and exact source-reference
  provenance through Flyway V6.
- Explicit report create/history APIs using synthetic tenant/operator context.
- `AWAITING_REVIEW` lifecycle, queue/detail action behavior, and an independent
  report workspace panel.
- Focused, PostgreSQL, HTTP, Angular, architecture, provider-adapter, responsive,
  and authorized Bedrock smoke verification.
- Factual documentation updates after implementation and verification.

## Out of scope

- Operator approval, rejection, reason capture, decision state, or final report
  status.
- A general audit-event table or audit-timeline UI.
- Automatic remediation, tool execution from the model, or any operational
  side effect from a recommendation.
- Additional MCP tools, evidence types, incident families, or retrieval reruns.
- Authentication, authorization, AWS infrastructure, or deployment selection.
- Prompt-management services, Bedrock Agents, Knowledge Bases, Guardrails, or a
  model-evaluation platform.
- Streaming generation, background jobs, WebSockets, or speculative scaling
  infrastructure.
- Editing V1-V5 or weakening the existing external Titan V2 smoke limitation.

## Constraints

- Follow red-green-refactor for every production behavior change.
- Carry `tenant_id` explicitly through every input, persistence, citation, and
  lookup boundary.
- Treat all model output as untrusted until schema, semantic, source-reference,
  tenant, length, and enum validation passes.
- Preserve facts, inference, approved guidance, and human decision as separate
  concepts in code, persistence, APIs, and UI.
- Every report conclusion must reference supporting persisted evidence; no
  source may be invented, paraphrased as a citation, or silently refreshed.
- Preserve every previous attempt and exact source snapshot needed to explain a
  successful or failed generation.
- Keep model I/O outside database transactions and never let model content
  choose workflow state.
- Use synthetic data only. Never log or commit credentials, prompts, provider
  payloads, or real payment/customer data.
- Keep all three deployables independently buildable and preserve the B01-B06
  architecture and verification boundaries.

## Acceptance criteria

- [x] An owned `INVESTIGATING` investigation with terminal evidence and
      knowledge attempts can explicitly create one report-generation attempt
      without client-supplied prompt, model, schema, or source parameters.
- [x] Generation composes exact tenant-scoped incident, newest/applicable
      evidence, and newest terminal retrieval snapshots without querying
      another feature's storage or rerunning MCP/retrieval.
- [x] Never-attempted prerequisites, invalid state, concurrent generation, and
      a prior successful report return structured `409` responses without a
      model call or persisted attempt.
- [x] Cross-tenant and nonexistent investigations return indistinguishable
      `404` responses without a model call or persisted attempt.
- [x] The API records `STARTED` before Bedrock and holds no database transaction
      across the model call.
- [x] The versioned application schema separates cited observations from cited
      inference, probable cause, confidence, recommendation, contradictions,
      and gaps.
- [x] The exact `report-v1` schema is included in the versioned prompt; the
      application strictly parses one JSON object and independently validates
      schema, semantic invariants, tenant/source membership, and references.
- [x] Unknown, duplicate, unavailable, cross-snapshot, and cross-tenant source
      references cannot produce or expose an `AVAILABLE` report.
- [x] Missing or degraded inputs can produce an explicit low-confidence
      `INSUFFICIENT_EVIDENCE` report but cannot fabricate probable cause or a
      recommendation.
- [x] Unavailable, timeout, malformed, and interrupted attempts remain visible,
      and retry appends history without hiding or overwriting an earlier attempt.
- [x] A validated report and the `AWAITING_REVIEW` transition commit atomically;
      a failed generation leaves the incident `INVESTIGATING`.
- [x] `AWAITING_REVIEW` incidents remain in the work queue and route to the
      report workspace with review-oriented copy.
- [x] The workspace displays the proposed report after evidence and approved
      knowledge, labels AI output as advisory/unreviewed, and shows provenance
      adjacent to every claim.
- [x] The workspace renders loading, not-generated, generating, available,
      insufficient-evidence, unavailable, timed-out, malformed, interrupted,
      conflict, not-found, and API-error states without decision controls.
- [x] Every attempt preserves model, prompt/schema, source-snapshot, operator,
      timing, status, and safe provider metadata required for later audit.
- [x] Existing alert, queue, detail, investigation, evidence, knowledge,
      transaction, tenant-isolation, contract, and responsive behavior remains
      unchanged except for the explicit `AWAITING_REVIEW` additions.
- [x] Focused and full verification pass with zero skipped tests; the authorized
      Bedrock smoke either passes or is recorded as an exact external limitation.
- [x] The final diff contains no secrets, real payment data, provider payloads,
      generated output, unrelated refactoring, or edits to V1-V5.

## Test plan

### Schema, validation, and prompt

- `buildsVersionedBoundedReportInputFromExactSnapshots`
- `usesVersionedReportSchemaPromptWithDeterministicSettings`
- `acceptsCitedProposedReportMatchingReportV1`
- `acceptsExplicitInsufficientEvidenceWithoutCauseOrRecommendation`
- `rejectsObservationThatReferencesKnowledgeAsObservedFact`
- `rejectsConclusionWithoutEvidenceReference`
- `rejectsRecommendationWithoutApprovedKnowledgeReference`
- `rejectsUnknownDuplicateOrCrossSnapshotReference`
- `rejectsUnsupportedEnumOversizedTextAndConditionalSchemaViolation`
- `doesNotPersistOrLogMalformedRawModelOutput`

### Workflow and persistence

- `composesReportSnapshotsBeforeRecordingStarted`
- `recordsStartedBeforeCallingBedrockWithoutOpenTransaction`
- `persistsValidatedReportClaimsAndTenantSafeReferences`
- `preservesEveryFailedAndInterruptedAttemptNewestFirst`
- `rejectsConcurrentGenerationBeforeSecondModelCall`
- `allowsRetryAfterTerminalFailure`
- `preventsReplacementAfterAvailableReport`
- `atomicallyPersistsReportAndMovesIncidentToAwaitingReview`
- `leavesIncidentInvestigatingWhenGenerationFails`
- `doesNotQueryIncidentEvidenceOrKnowledgeTablesFromReportPersistence`

Use PostgreSQL/Testcontainers for Flyway V6, constraints, snapshot references,
concurrency, atomic lifecycle changes, ordering, and tenant isolation. Mock-only
tests are not sufficient for those criteria.

### HTTP and architecture

- `createsReportForTenantOwnedReadyInvestigation`
- `returnsReportAttemptsNewestFirst`
- `requiresSyntheticOperatorForReportMutation`
- `rejectsMalformedReportIdentifiers`
- `returnsConflictWhenEvidenceOrKnowledgeWasNeverAttempted`
- `doesNotCallModelOrPersistForCrossTenantInvestigation`
- Architecture rules allow `report` to depend only on published incident,
  evidence, and retrieval ports and keep its persistence adapter report-owned.

### Angular

- `loadsReportHistoryIndependently`
- `generatesReportWithoutClientPromptOrSourceParameters`
- `rendersObservationsInferenceRecommendationAndGapsSeparately`
- `rendersEvidenceAndKnowledgeProvenanceBesideEveryClaim`
- `distinguishesInsufficientUnavailableTimedOutMalformedAndInterruptedStates`
- `disablesGenerationWhilePendingAndHandlesConflictSafely`
- `keepsAwaitingReviewIncidentInQueueWithReviewRoute`
- `containsNoHumanDecisionControls`

### External and manual verification

- Run the explicit authorized Bedrock report smoke against
  `global.amazon.nova-2-lite-v1:0` and verify prompt-guided JSON, strict
  application validation, and safe failures.
- Exercise available, insufficient-evidence, unavailable, timed-out, malformed,
  interrupted, retry, concurrency, prior-success, and cross-tenant scenarios.
- Inspect the queue and workspace at desktop and 390 CSS pixels for claim/source
  association, keyboard access, overflow, off-viewport actions, warnings, and
  errors.

## Expected approach

1. Promote this ready proposal only after explicit owner activation.
2. Add red schema/semantic/citation tests and lock the immutable `report-v1`
   contract.
3. Add red feature-port and architecture tests before report context assembly.
4. Add red PostgreSQL tests before Flyway V6 and report-owned persistence.
5. Add the deterministic model port and red lifecycle/transaction tests before
   the Bedrock Converse adapter.
6. Add HTTP contract tests before the controller and synthetic operator context
   behavior.
7. Add `AWAITING_REVIEW` state/queue tests before the lifecycle change.
8. Add Angular report-panel and queue tests before presentation behavior.
9. Run focused scopes, `./verify.ps1`, the authorized provider smoke when
   credentials are available, and live responsive verification.
10. Review the final diff for fabricated or dangling citations, tenant leakage,
    prompt/model payload exposure, non-atomic state, secrets, generated output,
    weakened tests, and scope drift.

## Likely files or components

- `backend/copilot-api/pom.xml`
- `backend/copilot-api/src/main/resources/application.yml`
- `backend/copilot-api/src/main/resources/db/migration/V6__*.sql`
- `backend/copilot-api/src/main/resources/reports/report-v1.schema.json`
- `backend/copilot-api/src/main/resources/prompts/report-v1.*`
- `backend/copilot-api/src/main/java/.../report/`
- Published snapshot ports under `incident`, `evidence`, and
  `knowledge.retrieval`
- `backend/copilot-api/src/test/java/.../report/`
- `backend/copilot-api/src/test/java/.../FeatureArchitectureTest.java`
- `frontend/operator-console/src/app/features/investigation-workspace/report-panel/`
- Queue, detail, investigation lifecycle, and shared incident-status models
- `.env.example`
- `README.md`
- `docs/agent/ARCHITECTURE.md`
- `docs/agent/DOMAIN.md`
- `docs/agent/STATUS.md`
- `docs/agent/tasks/current.md`

Do not modify the operations MCP server, immutable MCP v1 contract, existing
Flyway migrations, approved synthetic knowledge content, AWS infrastructure, or
unrelated incident features without an approved task-contract change.

## Validation commands

```powershell
./verify.ps1 -Scope Backend
./verify.ps1 -Scope Frontend
./verify.ps1 -Scope Repository
./verify.ps1
```

The unscoped command is the authoritative completion gate. The authorized
Bedrock smoke command must be documented during implementation and is additive,
not a replacement for deterministic local and CI coverage.

## Decisions needed

None. The owner selected the configurable Nova 2 Lite global default and
accepted `INSUFFICIENT_EVIDENCE` as a reviewable report that transitions to
`AWAITING_REVIEW`. Model access is verified before claiming provider
completion; it does not weaken deterministic CI coverage.

## Progress notes

- 2026-08-29: Repository review selected P2 report generation as the next
  vertical slice after evidence, retrieval, and B01-B06 boundaries.
- 2026-08-29: Prepared this proposal and ADR-0006 for owner review. No executable
  implementation has begun.
- 2026-08-29: Owner selected
  `${BEDROCK_CHAT_MODEL:global.amazon.nova-2-lite-v1:0}`, accepted global
  inference for the synthetic-only slice, and confirmed that a valid
  `INSUFFICIENT_EVIDENCE` report moves to `AWAITING_REVIEW`. ADR-0006 is accepted
  and this proposal became ready for explicit activation.
- 2026-08-29: Owner authorized implementation. Archived the completed B01-B06
  task and promoted this locked contract to `tasks/current.md` before executable
  work began.
- 2026-08-29: Implemented the report domain, exact input-snapshot ports,
  strict `report-v1` parsing and validation, one-call Nova adapter, append-only
  Flyway V6 persistence, create/history APIs, and atomic `AWAITING_REVIEW`
  lifecycle transition through red-green-refactor.
- 2026-08-29: Added the independent Angular report panel, report history and
  failure states, adjacent evidence/knowledge references, advisory boundary
  copy, and review-oriented queue/workspace behavior.
- 2026-08-29: Completed focused, PostgreSQL, HTTP, architecture, Angular,
  unscoped repository, desktop, and 390-CSS-pixel verification. No production
  Bedrock call was made during deterministic or browser verification.

## Completion evidence

- Red-phase evidence: report contract, validation, context, lifecycle,
  persistence, HTTP, provider-adapter, queue, and Angular component tests were
  introduced before their corresponding production behavior; the interruption
  retry scenario also failed before stale `STARTED` terminalization was added.
- Green-phase evidence: the focused report suite passed 27 tests with zero
  skips; PostgreSQL/Testcontainers applied Flyway V6 and verified append-only
  attempts, tenant-safe references, exact snapshots, retry history, atomic
  lifecycle changes, and tenant isolation.
- Acceptance-criteria coverage: backend report unit/HTTP/PostgreSQL tests,
  ten ArchUnit rules, and report/queue/workspace Angular tests cover the locked
  contract, including safe terminal failures and no client-supplied prompt or
  source parameters.
- Full verification: `./verify.ps1` passed on 2026-08-29 with 147/147 copilot
  API tests, 9/9 operations MCP server tests, and 53/53 Angular tests; all suites
  reported zero failures, errors, and skips. Spotless, Prettier, the 324.41 kB
  production build, zero-vulnerability npm audit, Compose validation, and
  `git diff --check` also passed.
- Manual verification: a bounded synthetic workspace rendered one evidence
  panel, one approved-knowledge panel, and one report panel at 1280x720 and
  390x844. At both sizes document width equaled scroll width, no interactive
  control was off viewport, keyboard focus had a visible 2.4 px outline, claim
  sources remained adjacent, and the browser reported no warnings or errors.
- Documentation updated: README, environment example, architecture, domain,
  project, roadmap, status, ADR-0006, and this task now describe the implemented
  model, schema, API, persistence, lifecycle, UI, and smoke path.
- Remaining limitations: the explicit real Bedrock report smoke against
  `global.amazon.nova-2-lite-v1:0` was not invoked because this task environment
  did not provide an explicitly authorized AWS credential/profile and isolated
  PostgreSQL smoke database. The one-shot smoke command is documented and its
  safe-log/safe-failure behavior is covered by deterministic tests. Human
  approval/rejection and the audit timeline remain P3.

