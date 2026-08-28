# Task: Polish the operator workflow presentation

Status: Complete

Archived: 2026-08-28

Created: 2026-08-28

Owner: Christopher Guzowski

## Goal

Make the implemented queue, incident detail, and investigation workspace read
as one intentional operator console: queue ordering must be visually
explainable, actions must use consistent accessible styling, status and action
layouts must remain compact, and the desktop presentation must use space more
efficiently.

## User story

As a payment operations analyst, I want the active-work workflow to communicate
its ordering and available actions clearly so that I can scan incidents and
move between the queue, detail, and investigation screens without mistaking
polish defects for workflow defects.

## Chosen contract

- The queue shows both `Received` and `Detected` timestamps for every incident.
  The default `Newest received` sort therefore has a visible field that
  explains row order. Existing sort membership and behavior do not change.
- Queue refresh, retry, start, resume, incident-detail, and back-navigation
  controls use a consistent visual language with intentional hover, focus,
  disabled, and visited states. Native semantics and router navigation remain
  unchanged.
- Status pills keep intrinsic content width. Queue status and resume action,
  incident-detail workflow actions, and the workspace incident-detail link use
  deliberate spacing without oversized empty containers.
- The queue header and surrounding spacing become more compact on desktop while
  preserving the existing responsive behavior at 390 CSS pixels.
- Repository-owned synthetic fixtures use plausible payment-operations names,
  not implementation-verification names. Existing local database records are
  not silently rewritten by application code.
- The investigation workspace remains limited to its current exact API
  contract. It does not gain incident title, severity, type, external alert ID,
  breadcrumbs, or any other incident-context field in this task.

## In scope

- Angular templates, component styles, and shared presentation styles for the
  queue, incident detail, and investigation workspace.
- Focused component tests covering the visible timestamp and control/layout
  contracts.
- Plausible repository-owned test/demo fixture wording where necessary.
- Responsive and keyboard-accessibility verification.
- Factual task and project-status updates after verification.

## Out of scope

- Backend, persistence, API, DTO, route, or domain changes.
- Adding incident context to the investigation workspace.
- Rewriting existing synthetic records in a developer's local PostgreSQL
  database from application code.
- Evidence collection, reports, human decisions, new workflow actions, or new
  incident lifecycle states.
- Changes to the global `Human review required` guardrail.
- New dependencies or unrelated redesign.

## Constraints

- Preserve the completed one-queue and start/resume behavior.
- Preserve loading, empty, not-found, error, retry, and pending states.
- Do not rely on color alone; retain semantic labels and native control
  behavior.
- Keep all screens usable without horizontal page overflow at 390 CSS pixels.
- Follow red-green-refactor for visible production behavior.

## Acceptance criteria

- [x] Each queue row visibly labels and displays both received and detected UTC
      timestamps.
- [x] The default newest-received ordering is still covered by a behavioral
      test and is understandable from the visible Received values.
- [x] Queue refresh and resume, detail start/resume, workspace incident-detail,
      retry, and back-navigation controls opt into the shared intentional
      control/link styling rather than browser-default or visited-purple
      presentation.
- [x] Status pills have intrinsic width, and status/action groups expose
      explicit layout hooks with deliberate spacing.
- [x] The incident-detail workflow action is compact and is not rendered as a
      generic oversized state card.
- [x] Desktop queue heading and content spacing are tightened while desktop and
      390-pixel layouts remain usable and free of horizontal page overflow.
- [x] Repository-owned fixture labels shown to users are plausible synthetic
      payment-operations data rather than implementation-verification wording.
- [x] No incident-context field or API-contract change is introduced in the
      investigation workspace.
- [x] Focused Angular tests, the full Angular suite, formatting, production
      build, and `git diff --check` pass.

## Test plan

- `showsReceivedAndDetectedTimestampsThatExplainDefaultOrdering`
- `usesIntentionalQueueControlAndActionStyles`
- `usesCompactDetailWorkflowActionStyles`
- `usesCompactWorkspaceStatusAndIncidentActionStyles`
- Re-run the existing sorting, refresh, start/resume, routing, loading, empty,
  not-found, and failure-state tests.
- Manually inspect queue, detail, and workspace at desktop and 390 CSS pixels.

## Progress notes

- 2026-08-28: Owner approved timestamp clarity, consistent controls, compact
  status/action layouts, and tighter/demo-ready presentation. Owner explicitly
  deferred adding incident context to the investigation workspace.
- 2026-08-28: Red phase added four named component regressions before template
  or style changes. The Angular run failed those four tests for the expected
  missing Received column and control/layout hooks while the other 26 passed.
- 2026-08-28: Green phase added visible Received and Detected fields, shared
  control/link styles, intrinsic-width status pills, compact action regions,
  and denser queue/detail spacing. All 30 Angular tests passed.
- 2026-08-28: The production build initially exposed a component-style budget
  warning. Shared controls were moved to the global stylesheet; the repeated
  component CSS was removed, and the warning-free build passed.
- 2026-08-28: Desktop and 390-pixel browser checks covered queue, detail, and
  workspace screens. All widths stayed within the viewport, both queue
  timestamps remained visible, status pills stayed intrinsic-width, and no
  browser warnings or errors were reported.
- 2026-08-28: The two exact local synthetic verification records visible in the
  review screenshots were relabeled with plausible alert identifiers, titles,
  and descriptions. No migration or runtime data-rewrite behavior was added.

## Completion evidence

- Red evidence: `npm test -- --watch=false` failed 4/30 tests for the intended
  missing timestamp and presentation hooks before production changes.
- Green evidence: `npm test -- --watch=false` passed 30/30 tests across eight
  files; `npx prettier --check .`, `npm run build`, and `git diff --check`
  passed with no warnings.
- Manual evidence: The live PostgreSQL/API/Angular workflow passed at 1280 and
  390 CSS pixels with no horizontal overflow or browser-console warnings. The
  queue displayed both timestamp columns, primary/secondary controls retained
  intentional colors, and status pills measured narrower than their parent
  cells.
- Scope evidence: No backend/API file was changed for this follow-up, and the
  investigation workspace still renders only its existing exact contract.

## Remaining limitations

- Investigation workspace context will be designed as a separate future task.

## Decisions needed

None.
