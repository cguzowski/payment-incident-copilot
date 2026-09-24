# Task: Connect generated SynTen incidents to service-error evidence

Status: Complete
Created: 2026-09-24
Owner: Christopher Guzowski

## Goal

Make the one-click local SynTen workflow collect the deterministic service-error
evidence owned by the standalone synthetic incident generator instead of
returning `NOT_FOUND` for every generated `sig-v1` alert reference.

## User story

As a payment operations analyst, I want **Collect service-error evidence** to
return the observations belonging to the generated SynTen incident, so the
rest of the investigation uses the scenario that actually produced the alert.

## Chosen contract

- The standalone synthetic incident generator remains the authoritative local
  evidence provider for alerts it creates. It continues to expose the immutable
  `getRecentServiceErrors` v1 MCP contract on port 8082.
- The root one-click Windows launcher starts or reuses the generator and waits
  for its health endpoint before starting the copilot API.
- The launcher supplies `OPERATIONS_MCP_BASE_URL=http://localhost:8082` to the
  copilot API process. It must not start the API against the legacy port-8081
  fixture server and switch providers afterward.
- The independently buildable operations MCP server and its legacy fixtures
  remain unchanged. Direct `scripts/start-local.ps1` configuration remains
  environment-overridable; this task fixes the root SynTen demonstration path.
- Generated S001 evidence remains `AVAILABLE` with `GATEWAY_TIMEOUT` and
  `UPSTREAM_CONNECTION_RESET`. Unknown scenarios and the wrong tenant remain
  `NOT_FOUND`.
- Existing recorded attempts are immutable. The fix applies to new collection
  attempts after the correctly configured API starts.

## In scope

- Root launcher startup order, health wait, environment wiring, and browser
  opening behavior.
- Launcher and live generator MCP regression tests.
- Authoritative verification coverage for the standalone generator so the
  launcher/MCP regressions run in local and CI completion gates.
- A live S001 operator/API smoke through generation, investigation start, and
  evidence collection when local prerequisites are available.
- Factual README, architecture, roadmap, status, and task updates.

## Out of scope

- Changing the MCP v1 wire contract, scenario catalog, evidence payload, or
  error-code mappings.
- Copying generator scenarios into the legacy operations MCP fixture server.
- Database migrations, frontend presentation changes, knowledge ranking,
  report generation, authentication, AWS, or another tenant/incident family.
- Rewriting historical `NOT_FOUND` attempts.

## Constraints

- Follow ADR-0001, ADR-0004, and ADR-0005 and preserve independent builds.
- Follow red-green-refactor for executable changes.
- Automated verification must be deterministic and network-free.
- Do not weaken tenant matching or unknown-scenario behavior.
- Do not expose generator truth through the alert or evidence response.

## Acceptance criteria

- [x] A launcher regression proves the generator is started/reused and healthy
      before `scripts/start-local.ps1` starts the API.
- [x] The root launcher supplies the generator MCP URL to the API process and
      does not silently retain port 8081 for the one-click SynTen workflow.
- [x] Live generator MCP coverage proves S001 returns `AVAILABLE` with exactly
      `GATEWAY_TIMEOUT` and `UPSTREAM_CONNECTION_RESET`, while wrong-tenant and
      unknown-scenario calls remain `NOT_FOUND`.
- [x] The authoritative backend and full verification plans build and test the
      standalone generator and reject skipped generator tests.
- [x] A live generated S001 investigation records and displays a new AVAILABLE
      collection attempt containing both expected error codes.
- [x] Relevant focused suites, generator verification, repository checks, and
      `./verify.ps1` pass with zero skips.

## Test plan

1. Change `BatchLauncherContractTest` first and confirm it fails against the
   current generator-last launcher.
2. Strengthen `GeneratorMcpContractTest` for exact S001 observations plus
   wrong-tenant and unknown-scenario `NOT_FOUND` results.
3. Extend verification-plan tests first, confirm red, then add standalone
   generator verify/no-skip steps to the authoritative gate.
4. Implement the minimal launcher reorder and environment wiring.
5. Run focused generator and verification-system tests, then backend,
   repository, and unscoped authoritative gates.
6. Run the local S001 workflow and record the resulting evidence attempt.

## Progress notes

- 2026-09-24: Diagnosis confirmed that generated alerts use opaque references
  such as `sig-v1-S001-...`, while the one-click launcher starts the API against
  the legacy port-8081 fixture provider, which only recognizes references such
  as `alert-auth-decline-001`. The generator's port-8082 MCP implementation
  already decodes the generated reference and returns the catalogued evidence.
- 2026-09-24: The owner authorized this task. The completed K5 task was archived
  unchanged before this contract became active.
- 2026-09-24: Red-green launcher coverage first failed against the legacy
  port-8081/start-generator-last path, then passed after the root launcher began
  starting and health-checking port 8082 before starting the API and explicitly
  selected that MCP endpoint after `.env` loading.
- 2026-09-24: The authoritative verification plan now builds the standalone
  generator and rejects skipped generator tests in both backend and aggregate
  scopes.
- 2026-09-24: A live S001 API and operator-console smoke recorded and displayed
  an `AVAILABLE` attempt with both catalogued error codes.

## Completion evidence

- Focused generator launcher/MCP tests passed 4/4 with zero skips; the full
  generator build passed 17/17 with zero skips and Spotless.
- Verification-plan tests and `start-local.bat --CheckOnly` passed.
- Live alert reference `sig-v1-S001-1790246971-c0de1234abcd` produced incident
  `07f91da2-b031-420a-b22d-3f78c09686c7`, investigation
  `e14a942d-c0a4-445c-bd7a-6e4eec8e0b32`, and evidence attempt
  `f84af625-4279-4c71-9d98-a5a5a4463a59`. The API and rendered workspace both
  showed `AVAILABLE`, `payment-authorization`, `GATEWAY_TIMEOUT`, and
  `UPSTREAM_CONNECTION_RESET`.
- `./verify.ps1` passed 289/289 copilot API, 9/9 operations MCP server, 17/17
  generator, and 78/78 Angular tests with zero failures, errors, or skips, plus
  Spotless, Prettier, production builds, Compose validation, and diff checks.

## Remaining limitations

- Existing `NOT_FOUND` evidence attempts remain immutable; operators must make a
  new collection attempt after launching the API with the corrected endpoint.
- The legacy port-8081 operations MCP fixture remains independently usable and
  intentionally does not recognize generated `sig-v1` references.

## Decisions needed

None.
