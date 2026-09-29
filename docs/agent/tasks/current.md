# Task: Recover report-generation button state (U1)

Status: Completed
Created: 2026-09-29
Owner: Christopher Guzowski

## Goal

Ensure report generation leaves its loading presentation after every terminal
outcome and does not retain obsolete generation work after the one-response API
contract has been satisfied.

## User story

As a payment operations analyst, I want the Generate proposed report control to
stop appearing busy when generation finishes, so its state accurately reflects
whether work is still running.

## Chosen contract

- The report-generation request consumes at most one response, matching the
  existing HTTP endpoint contract, and unsubscribes immediately after that
  response.
- Every terminal report status and every HTTP error clears the generating state.
- A button disabled because generation is active presents a busy state and wait
  cursor; a button disabled only because generation is no longer allowed does
  not present a busy state or wait cursor, including on hover.
- Existing report history, terminal-status rendering, error distinctions,
  report-available notification, API schemas, and incident lifecycle behavior
  remain unchanged.

## In scope

- Report-panel request lifecycle and button-state presentation.
- Angular regressions for terminal response, unsubscription, errors, and disabled
  versus busy presentation.
- Focused frontend and full repository verification.

## Out of scope

- Backend report-generation timing, provider cancellation, or API changes.
- Background jobs, polling, retries, or new dependencies.
- Other investigation-section presentation changes from U2 or identifier links
  from U3.

## Constraints

- Follow red-green-refactor for executable behavior.
- Keep automated tests deterministic and provider-free.
- Preserve prior attempts throughout generation and after terminal outcomes.
- Do not make an unavailable Generate button appear actionable.

## Acceptance criteria

- [x] `AVAILABLE`, `UNAVAILABLE`, `TIMED_OUT`, and `MALFORMED` responses clear
      the loading label, status message, busy semantics, and wait-cursor styling
      as soon as the terminal response is received, without requiring source
      completion.
- [x] The generation subscription releases obsolete upstream work immediately
      after the single response and when the panel is destroyed.
- [x] Conflict, not-found, and other HTTP errors continue to clear the loading
      state and preserve their existing messages and report history.
- [x] A workflow-disabled button remains visibly disabled but does not show the
      generation wait cursor, including on hover.
- [x] Focused frontend tests and the full repository verification gate pass with
      zero skipped tests.

## Test plan

- Extend `clearsGeneratingForThe...TerminalGenerationResponse` to emit without
  completing and assert loading/busy state clears plus upstream observers are
  released.
- Assert pending generation retains disabled, busy, and wait-cursor state.
- Assert a post-`AVAILABLE` workflow-disabled button is not busy and has no
  wait-cursor class.
- Preserve the existing HTTP-error and previous-history regressions.
- Run `./verify.ps1 -Scope Frontend`, then `./verify.ps1`.

## Progress notes

- 2026-09-29: Owner activated U1 from the roadmap.
- 2026-09-29: Inspection found that the component clears `generating` in
  `finalize`, which waits for completion/error/unsubscription after a response,
  and that the shared disabled-button cursor always communicates waiting even
  when generation is terminal and the workflow alone disables the control.
- 2026-09-29: Added the one-response subscription boundary, explicit busy
  semantics, and separate busy/workflow-disabled cursor styles. The red focused
  run failed all four non-completing terminal-response cases and the missing
  busy-state assertion; the implementation then passed focused and aggregate
  verification.

## Completion evidence

- Red: the focused report-panel run had 5 failures and 8 passes. Each terminal
  status remained on `Generating…` when the source stayed open, and the active
  button lacked its explicit busy class.
- Green: the focused report-panel spec passed 13/13 tests, including immediate
  upstream release, panel-destroy cancellation, busy semantics, and computed
  `wait` versus `not-allowed` cursor behavior.
- `./verify.ps1 -Scope Frontend` passed 79 Angular tests with zero failures or
  skips, Prettier, and the production build.
- `./verify.ps1` passed on 2026-09-29: 290 copilot API, 9 operations MCP, 31
  generator, and 79 Angular tests passed with zero failures, errors, or skips;
  formatting, builds, Compose validation, and repository diff checks also
  passed.

## Remaining limitations

- Frontend installation continues to report six dependency advisories (five
  moderate, one high); the repository verification gate does not fail on npm
  audit findings.

## Decisions needed

None.
