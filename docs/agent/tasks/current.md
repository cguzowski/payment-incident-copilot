# Task: Establish explicit codebase boundaries B01-B06

Status: Complete
Created: 2026-08-29
Owner: Christopher Guzowski

## Goal

Make the existing three-application codebase safer to extend with report
generation by establishing complete repository verification, explicit internal
feature ownership, stable tenant-scoped snapshot ports, a composed
investigation workspace, one synthetic request-identity convention, and a
versioned MCP wire contract.

## User story

As the repository maintainer, I want the current MVP behavior protected by
automated boundaries so that report generation can consume stable incident,
evidence, and knowledge contracts without reaching through feature storage,
UI state machines, or duplicated integration schemas.

## Context

The approved-knowledge slice is implementation-complete. Live authorized
Bedrock smoke verification remains an explicitly documented external
limitation and is not weakened or reclassified by this task.

`docs/agent/CODEBASE_BOUNDARY_FINDINGS.md` identified B01-B06 as the changes
that should precede report generation. The owner reviewed the phased plan and
authorized implementation on 2026-08-29.

## Chosen contract

### Delivery order

Implement behavior-preserving slices in this order:

1. B05 authoritative local/CI verification.
2. B02 tenant-scoped investigation and normalized evidence snapshot ports.
3. B01 feature package ownership and dependency enforcement.
4. B03 Angular workspace composition.
5. B04 one synthetic request-identity convention.
6. B06 one versioned MCP wire contract.

Every slice must pass focused tests and the authoritative broader verification
available at that point before the next slice begins.

### B05 verification boundary

- Add one repository-root PowerShell verification entry point that runs on
  Windows PowerShell/PowerShell Core and Ubuntu PowerShell Core.
- Pin Node.js `24.14.1`, which is supported by Angular 21, while preserving the
  repository's npm `10.8.3` package-manager pin.
- Add and use the pinned Maven Wrapper.
- Add Spotless Maven plugin `3.9.0` with Palantir Java Format `2.96.0` and bind
  formatting verification to `verify`.
- The root entry point runs prerequisite checks, Maven clean verification,
  backend no-skips enforcement, locked frontend installation, non-watch
  frontend tests with no-skips enforcement, Prettier, the production build,
  Docker Compose validation, and `git diff --check`.
- CI uses the same script implementation. Backend and frontend jobs may use
  explicit script scopes for useful isolation, followed by one aggregate
  required result; CI must not duplicate the command list.

### B02 snapshot ports

- The incident feature publishes a tenant-scoped investigation snapshot for
  knowledge retrieval containing only investigation/correlation identifiers,
  incident family, title, and description.
- The evidence feature publishes a tenant-scoped latest-applicable evidence
  snapshot containing identifiers, a stable status value, service name, and
  normalized error-code counts. It does not publish persistence records, the
  MCP payload type, or the evidence status enum.
- Knowledge retrieval composes those ports before starting an attempt.
- The knowledge retrieval persistence adapter owns only knowledge retrieval
  attempt/result tables and no longer queries or decodes incident/evidence
  storage.
- Preserve the current distinction between the newest evidence attempt and the
  newest earlier AVAILABLE/PARTIAL attempt that can contribute observations.

### B01 feature ownership

- Keep the existing three deployables and Maven/npm project boundaries.
- Inside the copilot API, establish `incident`, `evidence`,
  `knowledge.catalog`, and `knowledge.retrieval` feature ownership.
- Evidence collection obtains investigation/scenario context through a narrow
  incident read port; its persistence adapter owns only evidence tables.
- Catalog owns approved source loading, parsing, chunking, hashing, embedding,
  ingestion, and index writes. Retrieval owns investigation-time query
  derivation, search, selection, attempts, history, and HTTP behavior.
- Add package dependency tests after the target map exists. Enforce allowed
  feature directions rather than naming conventions alone.
- Do not add Maven modules, deployables, a global `common` package, or a shared
  compiled DTO jar.

### B03 Angular composition

- Keep `InvestigationWorkspaceComponent` as the route-level investigation
  loader and composition shell.
- Extract independently loading/retrying observed-evidence and
  approved-knowledge panels. Each owns its models, API calls, state, template,
  styles, and focused tests.
- Move investigation lifecycle API calls and models used by incident detail and
  the workspace under `core/api/investigations`.
- Remove the workspace's import of the incident-detail component stylesheet.
  Reuse only genuinely shared presentation mixins.
- Preserve copy, provenance, history ordering, retry behavior, accessibility,
  and responsive behavior.

### B04 synthetic request identity

- Use `X-Synthetic-Tenant-Id` as the required tenant context for application
  HTTP requests.
- Use `X-Synthetic-Operator-Id` for operator-attributed mutations, initially
  investigation start.
- Add one validated backend request-context resolver and one frontend context
  interceptor.
- Resource identifiers remain in paths. Tenant/operator identity is removed
  from resource paths, query parameters, and request bodies.
- Queue reads use `GET /api/incidents`; alert intake receives tenant identity
  from the request context; investigation start has no operator body.
- Persistence and application ports continue to receive tenant identity
  explicitly and enforce tenant-scoped lookups.
- These caller-supplied headers are synthetic demonstration context, not
  authentication or a production authorization claim.
- Migrate atomically; do not maintain two active identity conventions.

### B06 MCP contract

- Add one repository-owned immutable `v1` contract artifact for
  `getRecentServiceErrors`, including metadata, input/output JSON schemas, and
  synthetic canonical fixtures.
- Both Java service test suites consume the same artifact as test resources.
- Provider tests compare live MCP discovery and responses semantically with the
  contract. Consumer tests decode canonical fixtures and reject incompatible
  results.
- Keep provider and consumer implementation records separate.
- Keep transport concerns separate from a typed evidence-owned payload decoder.
- A backward-incompatible future contract creates `v2`; it does not rewrite
  `v1`.

## In scope

- B01-B06 exactly as described above.
- Test-only architecture and contract-verification dependencies where required.
- Mechanical package and formatter moves required by the chosen boundaries.
- Public HTTP contract documentation for the synthetic identity migration.
- ADRs for internal module/snapshot boundaries, synthetic request identity, and
  the MCP contract artifact.
- Factual updates to architecture, quality, status, README, and this task.

## Out of scope

- B07-B12 except where a minimal supporting move is unavoidable for B01-B06.
- Report generation, report review, human decisions, audit timeline, or
  incident lifecycle expansion.
- Authentication, authorization, AWS infrastructure, or deployment choices.
- New MCP tools or incident families.
- Flyway schema changes or edits to V1-V5.
- JPA/forms/validation dependency cleanup, Testcontainers consolidation,
  retrieval-history batching, generic web-error consolidation, or correlation
  logging.
- Live Bedrock smoke verification; its existing external limitation remains.

## Constraints

- Follow red-green-refactor for every production behavior change.
- Preserve every current successful and failure outcome except the explicitly
  chosen B04 HTTP identity transport change.
- Carry tenant identity explicitly through every application and persistence
  port even after HTTP extraction is centralized.
- Preserve STARTED-before-network transaction boundaries for MCP and Bedrock.
- Preserve exact evidence and retrieval history, audit metadata, raw knowledge
  excerpts, and missing/degraded outcomes.
- Use synthetic data only and never log or commit credentials or real payment
  data.
- Keep every deployable independently buildable.
- Keep the untracked boundary-findings source user-owned unless the owner
  separately chooses to add it.

## Acceptance criteria

- [x] One authoritative root command and CI implementation verify all three
      deployables, formatting, builds, Compose, diff integrity, and zero skipped
      tests.
- [x] Knowledge retrieval composes tenant-scoped incident and normalized
      evidence snapshots and does not query or decode incident/evidence
      persistence.
- [x] Evidence collection uses an incident-owned context port and its
      persistence adapter owns only evidence tables.
- [x] Copilot API code is organized into enforced incident, evidence,
      knowledge-catalog, and knowledge-retrieval feature boundaries.
- [x] No new deployable, Maven feature module, global common package, or shared
      Java DTO artifact is introduced.
- [x] The Angular investigation route composes independently tested evidence
      and knowledge panels and imports no sibling feature stylesheet.
- [x] Shared investigation lifecycle API code has a deliberate core location.
- [x] Every application HTTP path uses the documented synthetic request context
      and no longer transports tenant/operator identity inconsistently.
- [x] Tenant isolation, indistinguishable cross-tenant not-found behavior, and
      explicit tenant persistence parameters remain tested.
- [x] Both Java services verify the same immutable versioned MCP contract
      artifact while retaining separate implementation types.
- [x] Existing queue, detail, investigation, evidence, knowledge, transaction,
      provenance, retry, responsive, and failure behavior passes unchanged.
- [x] V1-V5 remain unchanged and no unrelated B07-B12 or report-generation work
      appears in the final diff.

## Test plan

### Verification system

- Focused PowerShell tests for version rejection, command ordering, child
  failure propagation, backend/frontend skipped-result rejection, and scoped
  execution.
- A CI contract check that prevents a second duplicated verification list.

### Backend snapshots and packages

- PostgreSQL tests for tenant-owned incident snapshots and latest/applicable
  evidence semantics.
- Workflow tests proving snapshot composition occurs before STARTED persistence
  and cross-tenant requests perform no evidence, embedding, or persistence work.
- Existing PostgreSQL and HTTP retrieval tests with unchanged response and
  transaction assertions.
- Architecture tests for allowed package directions and adapter isolation.

### Angular

- Focused shell, evidence-panel, knowledge-panel, lifecycle API, and request
  context interceptor tests.
- Preserve the existing named loading, empty, partial, unavailable, retry,
  provenance, ordering, direct-route, and failure scenarios.
- Desktop and 390-CSS-pixel browser verification with no overflow or warnings.

### HTTP identity

- Missing, blank, malformed, conflicting/legacy, tenant-scoped not-found, and
  operator-required request-context tests across each endpoint family.
- Frontend tests proving identity is attached once and resource services no
  longer build tenant/operator parameters themselves.

### MCP contract

- Provider discovery/schema/annotation and canonical-response tests.
- Consumer canonical-fixture decoding plus unknown-field, identifier,
  status/content, bounds, timeout, and unavailable tests.
- Full live local MCP/API workflow regression.

## Expected approach

1. Add red verification-system tests, then implement B05 and make it the gate.
2. Add red snapshot contract and PostgreSQL tests, then remove knowledge
   persistence reach-through.
3. Move feature types mechanically, introduce the incident context port for
   evidence, then add architecture rules.
4. Move Angular evidence and knowledge behavior into focused panels one at a
   time with their existing tests.
5. Add request-context tests, then migrate backend and frontend identity in one
   coherent slice.
6. Add failing cross-service contract tests, then the canonical artifact and
   typed decoder.
7. Run focused tests after every behavior and the authoritative full gate after
   every phase.
8. Review the final diff for scope, secrets, generated output, weakened tests,
   architecture leakage, and contract drift.

## Decisions needed

None. The owner approved the phased plan and its recommended identity,
toolchain, formatter, snapshot-port, UI-composition, and MCP-contract choices
on 2026-08-29.

## Progress notes

- 2026-08-29: Reviewed B01-B06 against the current implementation and tests.
- 2026-08-29: Owner authorized implementation of the phased plan.
- 2026-08-29: Archived the implementation-complete approved-knowledge task with
  its external Bedrock verification limitation preserved.
- 2026-08-29: Implemented B05 with one scoped root PowerShell gate, exact Node
  and npm pins, Maven Wrapper, Spotless/Palantir formatting, skipped-test
  enforcement, CI delegation, and a portable Ubuntu wrapper invocation.
- 2026-08-29: Implemented B02 and B01 with incident/evidence snapshots, an
  evidence collection context port, feature-owned persistence, four feature
  packages, and eight passing architecture rules.
- 2026-08-29: Implemented B03 and B04 with independently loading evidence and
  knowledge panels, a core investigation lifecycle client, shared SCSS mixins,
  one frontend context interceptor, and one validated backend header resolver.
- 2026-08-29: Implemented B06 with an immutable repository-owned MCP v1
  artifact, shared provider/consumer test resources, live provider contract
  checks, and an evidence-owned typed payload decoder.
- 2026-08-29: Repository and frontend verification scopes passed. The aggregate
  gate compiled both Java services and passed every runnable Java test, then
  correctly failed its no-skips check because Docker unavailability skipped 31
  PostgreSQL tests. Responsive browser regression remains pending with the
  Docker-backed local stack.
- 2026-08-29: A real Windows PowerShell 5.1 repository-scope regression first
  failed on the PowerShell-Core-only `$IsWindows` variable. Platform detection
  was made runtime-neutral, and the same scope then passed under both Windows
  PowerShell 5.1 and PowerShell Core.
- 2026-08-29: After the owner restarted Windows, Docker Desktop and
  Testcontainers recovered. The unscoped root gate passed with zero skipped
  tests, and live desktop/390-CSS-pixel workspace verification completed the
  remaining acceptance criteria.

## Completion evidence

- Red-phase evidence: verification ordering/version/no-skip tests, snapshot
  composition and ownership tests, architecture rules, Angular panel and
  interceptor tests, request-context controller tests, and shared MCP contract
  tests failed for their intended missing behavior before their production
  slices. The final Maven-wrapper portability test first failed because
  `Get-MavenWrapperInvocation` did not exist.
- Green-phase evidence: focused snapshot/PostgreSQL tests passed during B02;
  eight ArchUnit rules passed; 27 request-context/controller tests passed; ten
  consumer MCP tests and nine provider tests passed; and the final frontend gate
  passed 45/45 tests with zero skips.
- Full verification: `./verify.ps1` passed end to end after the Windows restart:
  116/116 copilot API tests and 9/9 operations MCP tests passed with zero skips
  against Docker/Testcontainers PostgreSQL 17.11 and Flyway V1-V5. The same gate
  passed 45/45 Angular tests with zero skips, npm audit with zero vulnerabilities,
  Spotless, Prettier, the 306.66 kB production build, Compose validation, and
  `git diff --check`.
- Manual verification: the live investigation route composed the investigation
  shell, observed-evidence panel, and approved-knowledge panel at 1280x720 and
  390x844. Both states had no document overflow, off-viewport interactive
  controls, browser warnings, or browser errors; direct-route history,
  provenance, degraded states, and actions remained visible.
- Documentation updated: root and operator-console READMEs, architecture,
  quality contract, ADR-0003 through ADR-0005, project status, and this task.
- Remaining limitations: none within B01-B06. The previously documented
  authorized live Titan V2 smoke check remains external and out of scope.
