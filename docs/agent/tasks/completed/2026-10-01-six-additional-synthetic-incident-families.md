# Task: Six additional synthetic incident families

Status: Complete - deterministic workflows and full verification passed 2026-10-01
Created: 2026-09-30
Owner: Christopher Guzowski

## Goal

Extend the complete operator triage workflow to six additional incident families.

## User story

As an operator, I want authorization timeouts, capture failures, refund failures,
settlement delays, webhook delivery failures and reconciliation mismatches in
my queue with matching evidence and approved guidance, so I can triage them
like authorization declines.

## Chosen contract

The owner explicitly authorizes expansion beyond the completed single-family MVP.
Add AUTHORIZATION_TIMEOUT_SPIKE, CAPTURE_FAILURE_SPIKE, REFUND_FAILURE_SPIKE,
SETTLEMENT_DELAY, WEBHOOK_DELIVERY_FAILURE and RECONCILIATION_MISMATCH.
Explicit alert type; omission retains the legacy decline default; reject unknown
families. Add deterministic available and degraded scenarios with matching
read-only service-error evidence and family-scoped approved runbooks/policies.
Reuse persisted retrieval, report, human decisions and audit. Preserve original
catalog/oracle/corpus bytes and fixed evaluations through separate additive assets.

## In scope

Typed intake, generated scenarios/evidence and terminal answer keys, approved
knowledge and packaging/import, console labels, integration tests and documentation.

## Out of scope

Real payments, new tenants, autonomous actions, authentication, model tuning,
general semantic grading, changes to historical evaluations.

## Constraints

Synthetic only. Guidance is not observed proof. Preserve tenant boundaries,
provenance, missing evidence and human review. MCP v1 aggregate errors suffice
for simulated diagnostic signals; they do not establish transaction outcomes or
independently measure alert rates, balances or delays. No new dependencies.

## Acceptance criteria

- [x] Intake persists seven families, rejects unknown types, retains legacy
      default/idempotency and tenant boundaries.
- [x] Generator offers six additional families with matching deterministic
      available/degraded MCP evidence and post-decision answer keys.
- [x] Approved versioned runbook and policy per family import and retrieve with
      family/tenant filters and exact source provenance.
- [x] Console queue and detail label the actual family.
- [x] Each new family passes deterministic persisted intake, investigation,
      evidence, retrieval, report, human decision and audit verification; degraded
      evidence preserves LOW/null insufficient-evidence behavior.
- [x] Focused suites and ./verify.ps1 pass; documentation covers import/restart
      and limitations without claiming unperformed live-model checks.

## Test plan

Parameterized AlertApiPostgresIntegrationTest for typed intake, invalid/replay.
Generator catalog/generation/MCP/oracle tests for additional resources. Knowledge
source/chunk validation and PostgreSQL workflow tests for every new family,
source citations, degraded evidence, terminal decisions and audit with model
doubles. Angular queue/detail labels. Focused suites then ./verify.ps1.

## Progress notes

- Read required context and service instructions; initial Git working tree clean.
- Archived completed Q6 before activating the owner-requested expansion.
- Additive design preserves all historical evaluations and accepted safety rules.

## Completion evidence

- Red: six explicit-family intake cases returned HTTP 400 before implementation;
  generator catalog lookup lacked S301-S306/S311-S316, knowledge had no new-family
  sources, and all twelve console label cases displayed the decline label.
- Focused API suite passed 38 tests (intake, retrieval and twelve complete workflows)
  with no failures/errors/skips. Generator suite passed 50 tests; knowledge source
  tests passed. Degraded workflows preserve insufficient-evidence/LOW/null fields.
- Launcher preparation regression failed at expected 3 versus actual 2 steps;
  adding the explicit Markdown import step made the PowerShell tests pass.
- Initial full gate passed backend/generator verification but stopped at npm ci
  EPERM: a project esbuild process held its executable open. Resolution and final
  verification are in progress.

- Final ./verify.ps1 passed on 2026-10-01: 337 API, 9 operations MCP, 50
  generator and 103 Angular tests with zero failures/errors/skips. Java formatting,
  Angular formatting/production build, standalone generator packaging, Compose,
  verification-system tests and repository checks passed.
- Resolved npm ci EPERM by identifying and temporarily stopping only this
  project's Angular dev server (Node 27592, esbuild 10620); restored it after
  the gate at http://localhost:4200 and confirmed HTTP 200. Repository scope
  passed after completion documentation. Launcher plan tests include the explicit third Markdown import step.
- All fourteen added catalog/oracle/guidance hashes match the package manifest.
  Original observable/oracle resources, corpus/evaluations and legacy knowledge
  have no Git diff. The API jar packages twelve new guidance sources and no
  oracle; the independently built generator packages both additive authorities.
- Twelve rendered queue/detail label tests pass; templates and styles retain
  their existing structure. No additional live browser or model benchmark was
  performed. The complete persisted workflow tests use deterministic providers.
- npm ci reports eight dependency advisories (four moderate, four high) with
  unchanged lockfile. The gate has no failing npm-audit check; this is not a
  clean dependency security assessment.


## Remaining limitations

Live-model quality for new families has not been measured. Aggregate service
errors do not independently prove transaction outcomes, balances or alert
thresholds. Local adoption requires explicit -PrepareKnowledge and restarting
old API/generator processes. Eight existing dependency advisories remain; no
lockfile/dependency changes are included. No new browser visual QA was performed
for the unchanged layouts; rendered DOM label tests cover the changed text.

## Decisions needed

None; owner authorized expansion and triage support.
