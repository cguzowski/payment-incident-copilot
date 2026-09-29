# Task: Collapse populated investigation sections (U2)

Status: Completed
Created: 2026-09-29
Owner: Christopher Guzowski

## Goal

Reduce investigation-workspace scrolling while keeping active controls, empty
states, failures, and source provenance clear and accessible.

## User story

As a payment operations analyst, I want populated investigation sections and
individual approved sources to collapse, so I can move between evidence,
knowledge, report, and audit history without losing their context.

## Chosen contract

- Service-error evidence, Runbooks and policy, Proposed incident report, and
  Audit timeline use native, keyboard-accessible disclosures once they contain
  history or timeline events.
- Populated disclosures start open. Their visible summaries retain the existing
  section eyebrow and title so collapsed content remains identifiable.
- Each selected runbook or policy result is independently collapsible and starts
  open, with its document type and title in the summary.
- Loading, empty, not-found, and error states remain expanded so status and retry
  controls are never hidden before content exists.
- Collection, retrieval, generation, refresh, lifecycle, history ordering,
  provenance, and responsive behavior remain unchanged.

## In scope

- Disclosure markup and presentation in the observed-evidence,
  approved-knowledge, report, and audit-timeline panels.
- Focused Angular regressions for populated versus unpopulated behavior,
  independent source disclosures, default-open state, and preserved actions.
- Frontend and full repository verification.

## Out of scope

- Backend, API, persistence, and data-model changes.
- Remembering disclosure state across navigation or browser sessions.
- Decision-panel and investigation-summary collapsing.
- Identifier links from U3 or unrelated workspace redesign.

## Constraints

- Follow red-green-refactor for executable behavior.
- Use native disclosure semantics without adding a dependency.
- Preserve every existing status, error, action, history, and provenance path.
- Keep controls keyboard accessible and the workspace usable at 390 CSS pixels.

## Acceptance criteria

- [x] Populated Service-error evidence, Runbooks and policy, Proposed incident
      report, and Audit timeline sections start open and can be independently
      collapsed and expanded through native disclosure controls.
- [x] Loading, empty, not-found, and error states remain visibly expanded with
      their existing action or retry controls.
- [x] Every selected runbook or policy result starts open and can be collapsed
      independently without changing sibling disclosure state.
- [x] Existing collection, retrieval, generation, refresh, history ordering,
      boundary copy, and provenance rendering remain covered and unchanged.
- [x] Focused frontend tests and the full repository verification gate pass with
      zero skipped tests.

## Test plan

- Add panel-level tests for disclosure presence only after populated history,
  default-open state, summary labels, and native toggle behavior.
- Add a two-result knowledge fixture proving independent document disclosure
  state for runbook and policy results.
- Preserve existing loading, empty, error, retry, pending-action, history, and
  provenance regressions.
- Run the four focused panel specs, `./verify.ps1 -Scope Frontend`, and
  `./verify.ps1`.

## Progress notes

- 2026-09-29: Owner activated U2 from the ordered roadmap after U1 completion.
- 2026-09-29: Locked the disclosure contract before implementation. Inspection
  confirmed all four panels currently render populated content permanently
  expanded and the knowledge panel renders every selected source as a plain
  article.
- 2026-09-29: Added focused regressions first. The four-panel run failed 4 of 32
  tests for the intended reason: the evidence, knowledge, report, and timeline
  disclosures did not yet exist.
- 2026-09-29: Wrapped populated histories in default-open native disclosures and
  made each selected runbook or policy its own default-open disclosure. Shared
  styling supplies consistent indicators and keyboard focus without changing
  loading, empty, error, action, history, or provenance paths.
- 2026-09-29: Live browser QA confirmed keyboard toggling, independent nested
  source state, visible focus, and no horizontal overflow at a 390 CSS-pixel
  viewport.

## Completion evidence

- Focused four-panel Angular run: 32 tests passed after the intentional red run
  failed 4 tests and passed 28.
- `./verify.ps1 -Scope Frontend`: 86 Angular tests passed with zero skips;
  Prettier and the production build passed.
- `./verify.ps1`: 290 copilot API, 9 operations MCP, 31 generator, and 86 Angular
  tests passed with zero failures, errors, or skips; formatting, production
  builds, Compose validation, and repository diff checks passed.
- Browser QA on a populated investigation found all four section disclosures
  and all seven approved-source disclosures open initially. Keyboard interaction
  collapsed the evidence section and one source without changing its sibling or
  parent disclosure; the 390-pixel viewport had no horizontal overflow.

## Remaining limitations

- Disclosure state intentionally resets to open after navigation or reload; U2
  did not include state persistence.
- The frontend install still reports the six existing dependency advisories
  (five moderate and one high); the documented gate has no failing audit step.

## Decisions needed

None.
