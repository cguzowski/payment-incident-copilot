# Task: Automatically refresh the incident work queue

Status: Completed
Created: 2026-08-31
Owner: Christopher Guzowski

## Goal

Show newly received synthetic incidents in the operator work queue without
requiring the operator to press Refresh.

## User story

As a payment operations analyst, I want the incident work queue to refresh
automatically, so I can see new alerts shortly after they arrive.

## Chosen contract

- Refresh the currently selected queue view every five seconds while the queue
  component is open.
- Apply the refreshed response without replacing the populated queue with a
  loading screen.
- Preserve the selected queue view and sort order.
- Stop automatic refresh work when the operator leaves the queue.
- Preserve the existing manual Refresh and Retry controls.

## In scope

- The Angular incident work queue component and its focused regression test.

## Out of scope

- Backend API, persistence, ingestion, push notifications, WebSockets, and
  unrelated UI changes.

## Constraints

- Follow red-green-refactor.
- Keep the change deterministic and network-free in automated tests.
- Keep existing loading, empty, success, error, sorting, and view behavior.

## Acceptance criteria

- [x] While the queue remains open, it requests the current queue view again
      after five seconds and displays newly returned incidents automatically.
- [x] A background refresh preserves the selected view, sort, and populated
      success state.
- [x] Automatic refresh stops after the queue component is destroyed.
- [x] Existing focused frontend tests, formatting, and production build pass.

## Test plan

- `automaticallyRefreshesTheCurrentViewAndStopsWhenDestroyed`
- Existing alert queue component regression suite.

## Progress notes

- 2026-08-31: Archived the completed P3 task and activated this focused queue
  refresh contract from the owner's request.
- 2026-08-31: Added a lifecycle-bound five-second queue refresh. Background
  requests preserve visible results while pending or temporarily unavailable.
- 2026-08-31: The new timer and background-failure regressions failed before
  production changes, then passed after the minimal implementation.

## Completion evidence

- Focused queue suite: 11/11 tests passed.
- Frontend gate: 77/77 tests passed with zero skips, followed by Prettier and
  the production build.
- Authoritative `./verify.ps1`: 182/182 copilot API, 9/9 operations MCP server,
  and 77/77 Angular tests passed with zero skips; formatting, builds, npm audit,
  Compose validation, verification-contract tests, and diff checks passed.

## Remaining limitations

None for the selected polling behavior. New incidents can take up to five
seconds to appear.
