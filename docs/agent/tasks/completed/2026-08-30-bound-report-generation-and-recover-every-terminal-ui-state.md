# Task: Bound report generation and recover every terminal UI state

Status: Completed
Created: 2026-08-30
Owner: Christopher Guzowski

## Goal

Prevent a report-generation request from remaining active indefinitely when an
AI provider is unavailable, retrying, or stalled, while preserving an auditable
terminal attempt and a retryable operator experience.

The owner authorized this defect fix on 2026-08-30 and requested that it be
applied to every active local branch. This is the active, locked behavioral
contract.

## User story

As a payment operations analyst, I want every report-generation request to
finish in a visible terminal state within a bounded time, so the Generate
button never remains busy indefinitely and I can understand and retry failures.

## Chosen contract

- A report model invocation has one configurable total wall-clock deadline,
  defaulting to two minutes through `REPORT_GENERATION_TIMEOUT`.
- When the deadline expires, the provider task is cancelled and the existing
  report attempt is completed as `TIMED_OUT`; no late model result is parsed or
  persisted.
- Spring AI does not perform hidden retries inside one auditable operator
  attempt. A refused or unavailable provider completes that attempt as
  `UNAVAILABLE`; a new provider attempt requires an explicit operator retry.
- The frontend clears its busy state for every returned terminal status and
  every HTTP error while preserving prior attempts and the existing error
  distinctions.
- The report HTTP schema, persistence schema, evidence citations, model
  metadata, and incident lifecycle contract do not change.
- The verified code fix is applied to `feature/local-ollama`, `main`, and
  `future-prod-env-AWS-Bedrock`, with provider-specific code otherwise
  unchanged.

## In scope

- A provider-neutral total deadline around report model invocation.
- Explicit Spring AI retry configuration consistent with one auditable model
  request per operator attempt.
- Backend tests for success, provider exceptions, interruption, and a stalled
  invocation that exceeds the deadline.
- Frontend tests for all terminal report responses and generation HTTP errors.
- Local configuration and operational documentation for the deadline.
- Sequential propagation and verification across all active local branches.

## Out of scope

- Installing Ollama, downloading models, or making a live model call in the
  automated suite.
- Changing the report schema, prompt, source-validation rules, persistence
  schema, or incident lifecycle.
- Adding background generation, polling, job queues, or a new dependency.
- Changing evidence collection, approved-knowledge retrieval, or MCP behavior.
- Pushing branches or opening pull requests.

## Constraints

- Follow red-green-refactor for the executable behavior.
- Keep automated tests deterministic, bounded, and network-free.
- Preserve the difference between provider unavailability, provider timeout,
  malformed output, and an HTTP request failure.
- Do not persist a late result after the operator attempt has timed out.
- Preserve all existing audit metadata and synthetic-data guardrails.

## Acceptance criteria

- [x] Local configuration sets `spring.ai.retry.max-attempts` to one and a
      two-minute, environment-overridable report-generation deadline.
- [x] A model call that does not complete before the deadline is cancelled and
      the attempt is persisted and returned as `TIMED_OUT`.
- [x] A provider exception before the deadline retains its existing
      `UNAVAILABLE` or `TIMED_OUT` mapping, and successful output is unchanged.
- [x] No response produced after the deadline is parsed, persisted, or allowed
      to transition the incident lifecycle.
- [x] The Generate button leaves its busy state for `AVAILABLE`, `UNAVAILABLE`,
      `TIMED_OUT`, `MALFORMED`, conflict, not-found, and other HTTP errors.
- [x] Previous report attempts remain visible throughout generation and after
      every terminal outcome.
- [x] Focused backend and frontend regressions and the authoritative repository
      verification pass with zero skipped tests.
- [x] The verified fix is present on all three active local branches.

## Test plan

- `configuresOneAuditableModelCallAndABoundedReportDeadline`
- `returnsSuccessfulOutputBeforeTheDeadline`
- `preservesProviderFailureClassificationBeforeTheDeadline`
- `cancelsAStalledModelCallAtTheTotalDeadline`
- `doesNotParseOrPersistALateResponseAfterTimeout`
- `clearsGeneratingForEveryTerminalGenerationResponse`
- `clearsGeneratingForEveryGenerationHttpErrorAndPreservesHistory`
- Existing report model, generation service, HTTP/PostgreSQL, Angular, and
  aggregate verification suites.

## Validation commands

```powershell
./mvnw.cmd -pl backend/copilot-api -Dtest=AiModelConfigurationTest,ReportModelCallExecutorTest,ReportGenerationServiceTest test
Push-Location frontend/operator-console; npm test -- --watch=false; Pop-Location
./verify.ps1
```

## Decisions needed

None. The owner requested the bounded failure fix across all active branches;
the two-minute default remains configurable for slower local hardware.

## Progress notes

- 2026-08-30: The supplied log proves Ollama refused the connection while
  Spring AI was on retry count two. The exact Spring AI 2.0 runtime defaults to
  ten attempts with exponential backoff from two seconds to three minutes.
- 2026-08-30: The frontend already clears its busy flag when the request emits
  or errors, so the indefinite button is caused by a request that never reaches
  a terminal event. The current backend has provider-exception mapping but no
  total invocation deadline.
- 2026-08-30: Added a provider-neutral virtual-thread call boundary with a
  configurable two-minute total deadline. Deadline expiry interrupts the model
  task and completes the already-persisted attempt as `TIMED_OUT`; Spring AI
  hidden retries are disabled so connection refusal maps promptly to one
  `UNAVAILABLE` attempt.
- 2026-08-30: Added backend regressions for successful and classified failures,
  task cancellation, and discarding a late response, plus frontend regressions
  for all terminal responses and 409/404/other HTTP failures.
- 2026-08-30: The authoritative `./verify.ps1` gate passed on
  `feature/local-ollama`: 154 copilot API, 9 operations MCP server, and 60
  Angular tests with zero failures or skips, plus Spotless, Prettier, the
  production build, Compose validation, and `git diff --check`.
- 2026-08-30: The verified implementation was committed on
  `feature/local-ollama` (`d32abe4`), propagated to `main` (`e54e893`) with an
  identical tree, and adapted without changing the Bedrock provider or
  credential guardrails on `future-prod-env-AWS-Bedrock` (`453ae3a` and
  `1970afc`).
- 2026-08-30: A clean focused Bedrock run passed 12/12 configuration, call
  boundary, service, and model tests. The branch's authoritative
  `./verify.ps1` gate then passed 152 copilot API, 9 operations MCP server, and
  60 Angular tests with zero failures or skips, including credential safety,
  formatting, build, audit, Compose, and diff checks.

## Completion evidence

- Red: the focused copilot API test run failed compilation because the new
  `ReportModelCallExecutor` boundary did not yet exist.
- Green: focused local tests passed 10/10, the clean Bedrock-focused tests
  passed 12/12, and the frontend regression suite passed 60/60.
- The full local-provider and Bedrock-provider repository gates passed with
  zero skipped tests. `main` has the same Git tree as the fully verified local
  provider branch.
- The new tests prove deadline cancellation, provider-failure classification,
  successful output, discarded late output, every terminal report status, and
  every existing generation HTTP-error path.

## Remaining limitations

- No live Ollama or Bedrock request was made; the automated checks use bounded,
  deterministic doubles. Ollama must be running with the configured models for
  a local request to succeed; otherwise the attempt now terminates as
  `UNAVAILABLE`.
- Interrupting the local provider task prevents any late response from being
  parsed or persisted, but provider-side work already accepted before
  cancellation may not itself be cancellable by the remote service.
