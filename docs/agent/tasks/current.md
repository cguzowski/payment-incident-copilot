# Task: Link resolvable evidence and resource identifiers (U3)

Status: Completed
Created: 2026-09-29
Owner: Christopher Guzowski

## Goal

Turn identifiers with exact reviewer targets into clear links while preserving
plain text for identifiers that cannot resolve to meaningful application
content.

## User story

As a payment operations analyst, I want evidence and approved-source references
to open the exact persisted record, excerpt, or immutable document, so I can
verify report conclusions without manually searching investigation history.

## Chosen contract

- Resolvable evidence-attempt, retrieval-attempt, report-attempt, and knowledge-
  chunk identifiers link to their exact rendered investigation record.
- Report citations resolve within the report attempt's persisted evidence and
  retrieval snapshots, including latest-evidence and applicable-evidence IDs.
- Applicable audit source and related-source identifiers link only when their
  event type identifies an exact rendered record.
- A PDF SHA-256 links to the exact immutable synthetic PDF and opens its cited
  physical page. The read-only resource URL is content-addressed and does not
  expose a tenant, filesystem path, or mutable filename as authority.
- Every resource link opens in a new tab and includes an accessible purpose;
  external-tab behavior is communicated without relying on an icon alone.
- Tool-call, correlation, actor, model, prompt, schema, document, version, and
  any unresolved identifiers remain non-links.

## In scope

- Content-addressed serving of manifest-listed SynTen PDF artifacts.
- Investigation-workspace anchors and links for resolvable evidence, retrieval,
  report, knowledge-chunk, audit, and decision references.
- Focused backend and Angular regressions for exact targets, PDF page fragments,
  new-tab safety, and intentionally unresolved identifiers.
- Backend, frontend, repository, and full verification plus responsive browser
  QA.

## Out of scope

- Authentication, authorization, expiring URLs, or general-purpose file
  serving.
- Linking identifiers that have no exact persisted or displayed target.
- Changing evidence collection, retrieval, generation, decision, or audit data.
- New source formats, corpus content, ranking behavior, or disclosure-state
  persistence.

## Constraints

- Follow red-green-refactor for every executable behavior.
- Serve only repository-owned synthetic PDFs listed by the validated corpus
  manifest and verify returned bytes against the requested SHA-256.
- Do not place tenant identity, local filesystem paths, or mutable source names
  in resource URLs.
- Preserve loading, empty, error, retry, provenance, history, disclosure, and
  responsive behavior.
- Use native links with visible keyboard focus and safe new-tab attributes.

## Acceptance criteria

- [x] Evidence references, including latest and applicable evidence IDs, link to
      the exact evidence attempt and open in a new tab.
- [x] Knowledge-chunk and retrieval references link to the exact persisted
      retrieval result or attempt and open in a new tab.
- [x] PDF SHA-256 values link to the exact hash-verified PDF at the cited page
      and open in a new tab.
- [x] Applicable audit and decision references link to exact displayed records;
      identifiers without a meaningful target remain non-links.
- [x] Existing workflow behavior, disclosure behavior, history ordering,
      provenance, and responsive presentation remain unchanged.
- [x] Focused backend/frontend tests and the full repository verification gate
      pass with zero skipped tests; browser QA confirms new-tab targets and a
      usable 390 CSS-pixel layout.

## Test plan

- Add backend tests first for valid hash lookup, exact PDF bytes/content type,
  malformed or unknown hashes, manifest allow-listing, and hash mismatch.
- Add panel tests first for exact anchor IDs, report and provenance links, PDF
  page fragments, audit event mapping, new-tab attributes, and retained plain
  text for unresolved identifiers.
- Run focused backend and investigation-panel tests after red and green phases.
- Run `./verify.ps1 -Scope Backend`, `./verify.ps1 -Scope Frontend`, and
  `./verify.ps1`, then inspect the populated workflow in a browser at desktop
  and 390 CSS pixels.

## Progress notes

- 2026-09-29: Owner activated U3 from the ordered roadmap after U2 completion.
- 2026-09-29: Archived the completed U2 task and locked this contract before
  implementation. Inspection found no existing source-serving endpoint and all
  identifiers currently render as plain code text.
- 2026-09-29: Added exact evidence, retrieval, knowledge-chunk, report, and
  decision anchors plus mapped links in investigation histories, report
  citations, and audit events. Unresolvable identifiers remain plain text.
- 2026-09-29: Added a hash-addressed, read-only PDF endpoint for active and
  archived synthetic corpus versions. Live QA exposed and then verified the
  fix for an archived-version hash that initially returned 404.
- 2026-09-29: Live QA also exposed asynchronous fragment targets that retained
  the URL without scrolling. A workspace observer now opens enclosing native
  disclosures and scrolls the target after it renders.

## Completion evidence

- Focused red-green evidence: the five panel specs initially failed 5 of 43
  tests before links were implemented; the workspace fragment regression then
  failed 1 of 6 tests before the async scroll behavior was added; the archived
  PDF regression failed before prior corpus versions were packaged. The final
  focused suites passed 43 panel tests, 6 workspace tests, and 4 PDF artifact
  tests.
- `./verify.ps1 -Scope Backend` passed: 294 copilot API, 9 operations MCP, and
  31 generator tests passed with zero failures, errors, or skips; Spotless and
  builds passed.
- `./verify.ps1 -Scope Frontend` passed: 91 Angular tests passed with zero
  failures or skips; Prettier and the production build passed.
- `./verify.ps1` passed on 2026-09-29, including verification-system, local
  knowledge/AI prerequisite, retrieval-evaluation runner, backend, frontend,
  Compose, and diff checks.
- Populated browser QA found 33 source links, all with `_blank`, `noopener
  noreferrer`, and accessible “opens in new tab” text. Direct fragment loads
  scrolled evidence and knowledge records into view. At 390×844 CSS pixels the
  document width was 390, with no horizontal overflow or clipped source links.
- Live PDF verification returned 12,898 bytes for persisted hash
  `4132a6f7a627247c40c5caf2d57b7ca8d200840ea5dbabb9676a8d272168d940`;
  the response hash matched exactly and headers reported `application/pdf`,
  inline disposition, and immutable caching. The rendered link included
  `#page=3`.

## Remaining limitations

None.

## Decisions needed

None.
