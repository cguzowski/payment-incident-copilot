# SynTen Inc PDF authoring and validation standard

Status: Approved for K2 generation
Standard version: `synten-pdf-authoring/v1`
Applies to: Every source and PDF in `corpus/inventory.md`

## Objective

Produce synthetic runbooks and policies that look, read, render, and extract
like controlled documents used by a mature payment-operations organization.
Realism comes from coherent operational detail, document control, usable
procedures, credible tables, ownership, and revision discipline—not from
copying a real company or adding filler.

Every PDF has a hard maximum of 15 pages. The count includes the cover,
document-control content, table of contents, appendices, and revision history.
There is no exception process for exceeding the limit.

## Source and artifact contract

Each inventory row produces exactly two maintained artifacts:

1. `corpus/sources/<pdf-basename>.md` is the editable authority.
2. `corpus/pdfs/<pdf-basename>.pdf` is the reproducible rendered artifact.

K2 may add a generator and validation tools under `SynTen Inc/corpus/validation/`.
Temporary renders, extracted text, caches, and working files remain uncommitted.
Do not hand-edit a PDF; update its Markdown authority and regenerate it.

The generated validation manifest records at least:

- corpus and authoring-standard versions;
- source and PDF relative paths;
- document ID, version, type, approval status, and incident family;
- source and PDF SHA-256 hashes;
- actual page count;
- extracted-text character count;
- generation timestamp and generator version; and
- pass/fail results for the checks in this standard.

## Visual system

Use one restrained SynTen Inc document family with visible differences between
runbooks and policies.

| Element | Requirement |
|---|---|
| Page size | A4 portrait by default; landscape is allowed only for a wide decision table and still counts toward 15 pages. |
| Margins | At least 16 mm on every side, with enough footer space to prevent collisions. |
| Typography | Professional embedded sans-serif body font, 9.5-11 pt body text, clear heading hierarchy, and monospace only for exact codes or identifiers. |
| Color | High-contrast neutral palette with one restrained accent; meaning must not depend on color alone. |
| Header | Short document title or key, classification, and version on interior pages. |
| Footer | `SynTen Inc — Internal — Synthetic Demo`, document ID, and `Page x of y`. |
| Tables | Repeating headers, readable cells, no clipped text, and no rows split so badly that meaning is lost. |
| Diagrams | Simple decision flows or architecture context only when they improve use; text must remain extractable or have an adjacent text description. |
| Branding | Modest fictional wordmark treatment; no imitation of a real company's logo, trade dress, or policy template. |

Every page must render without overlapping text, clipped controls, orphaned
headings, broken glyphs, blank accidental pages, or content outside the page
box.

## Common controlled-document structure

Every document includes:

1. Cover block with company, title, document key, stable document ID, version,
   type, classification, approval status, owner role, effective date, incident
   family, and synthetic-data notice.
2. Document-control block with purpose, audience, review cadence, approver role,
   related documents, and revision history.
3. Main content organized with semantic headings and concise paragraphs.
4. References to related inventory keys, never invented URLs or private
   endpoints.
5. Footer page numbering on every page.

A separate table of contents is optional. Include it only when it improves a
document of sufficient length; it still counts toward the page limit.

## Runbook content contract

An approved runbook should normally use 6-12 pages and contain the sections
below when relevant:

- purpose, scope, triggers, symptoms, and explicit non-goals;
- prerequisites and evidence needed before drawing a conclusion;
- safety and authority boundaries;
- a concise decision flow or ordered diagnostic procedure;
- evidence checks using exact scenario error codes and bounded time windows;
- interpretation tables that separate observation from hypothesis;
- negative, missing, partial, stale, and contradictory evidence handling;
- human-authorized containment or recovery options described without automatic
  execution by the copilot;
- validation and rollback considerations for separately authorized actions;
- escalation owner, escalation package, and communication checkpoint;
- audit artifacts to retain; and
- related policies and revision history.

Steps must be operationally specific enough to guide an analyst. Avoid vague
padding such as “investigate further.” State what source, window, comparison,
owner, limitation, or outcome is needed. Do not include executable destructive
commands, real endpoints, credentials, or claims that the current MCP tool can
retrieve evidence it does not expose.

## Policy content contract

An approved policy should normally use 7-12 pages and contain:

- purpose, scope, policy objectives, and definitions;
- governing principles and mandatory statements using `must`, `must not`,
  `should`, and `may` consistently;
- roles and responsibilities, with a RACI-style table where it improves use;
- control requirements and human approval checkpoints;
- evidence, record-retention, exception, review, and non-compliance handling;
- related runbooks and policies; and
- revision history.

Policies define durable requirements. They may point to runbooks for detailed
procedures but must not duplicate pages of diagnostic steps. Superseded policy
versions display a prominent `SUPERSEDED — NOT RETRIEVAL ELIGIBLE` banner and
name their approved replacement.

## Realism requirements

Each document must pass an editorial review answering yes to all of these:

- Does the document have a clear operational owner and intended audience?
- Does its structure match its type rather than a generic repeated template?
- Could an analyst identify when to use it and when not to use it?
- Are procedures, controls, tables, and escalation packages concrete and
  internally consistent with the SynTen Inc profile?
- Do exact error codes appear only where they have a meaningful diagnostic
  relationship?
- Are safety, authority, rollback, and validation considerations proportional
  to the topic?
- Does the document reference related corpus items instead of duplicating them?
- Is every example visibly synthetic and free of plausible personal, account,
  card, merchant, credential, endpoint, or confidential data?
- Is the prose varied and natural, without repetitive filler or obviously
  model-generated framing?
- Does the PDF remain useful when printed or read without the application?

Failure on any item sends the source back for revision before acceptance.

## Responsible-AI and evidence language

Every document preserves these distinctions:

- An alert is a signal, not a verified cause.
- An MCP evidence item is an observed synthetic fact only within its source and
  observation window.
- A runbook or policy is guidance, not proof that its described condition
  occurred.
- A hypothesis or probable cause is inference and must cite observed evidence.
- A recommendation is advisory and must cite approved knowledge.
- A model cannot approve its report or execute remediation.
- Missing, partial, empty, unavailable, malformed, stale, or contradictory
  evidence remains visible and lowers confidence.

Superseded documents may contain historically different guidance to create a
realistic retrieval challenge. They must remain safe to store, be visibly
superseded on every page, identify the current replacement, and never be used
as approved advice.

## Metadata and exact vocabulary

The cover metadata must exactly match the inventory. The maintained source may
use machine-readable front matter, but the visible document-control block must
also show the human-readable values.

Exact controlled values:

- types: `RUNBOOK`, `POLICY`;
- statuses: `APPROVED`, `SUPERSEDED`;
- incident family: `AUTHORIZATION_DECLINE_RATE_SPIKE`;
- tenant ID: `8b860d80-d17f-4e6b-8c48-af35f26a4d61`;
- classification: `Internal — Synthetic Demo`.

Error codes come only from the versioned scenario catalog. A document may use
plain-language synonyms to support semantic retrieval, but it must retain the
exact code beside the explanation.

## Page-budget rules

- Preferred runbook budget: 6-12 pages.
- Preferred policy budget: 7-12 pages.
- Inventory target ranges guide composition.
- Hard accepted range: 1-15 pages inclusive.
- Cover, table of contents, landscape pages, appendices, and revision history
  all count.
- Reducing font size below the visual-system minimum, shrinking margins below
  the minimum, or clipping content to force acceptance is prohibited.
- If a complete document exceeds 15 pages, split the topic into independently
  useful inventory items only through an owner-approved inventory change; do
  not silently add a file or weaken content.

## Automated validation contract

K2 must implement deterministic checks that fail on:

1. An inventory row without exactly one source and one PDF.
2. An extra source or PDF not present in the inventory.
3. Duplicate document-version identity or filename.
4. Metadata that differs between inventory, source, and rendered PDF.
5. A PDF page count outside 1-15 inclusive.
6. An encrypted, password-protected, malformed, or scanned-only PDF.
7. Missing extractable title, document ID, version, classification, synthetic
   notice, approval status, or `Page x of y` text.
8. Missing exact error codes required by that row's scenario coverage.
9. A missing superseded banner or replacement reference on a superseded PDF.
10. A source or PDF hash missing from the generated validation manifest.
11. Sensitive-pattern findings that are not approved synthetic identifiers.
12. Rendered pages with detected blank pages, overflow, or unreadable text.

Text extraction must preserve a sensible reading order for headings, paragraphs,
lists, and tables. Page-level extracted text must be available to the later PDF
locator/chunking ADR; K2 does not select that ADR's extraction library.

## Visual verification contract

Automated checks do not replace visual QA. K2 must render every PDF page to an
image and inspect every document at least at thumbnail/contact-sheet scale.
It must inspect the cover, densest table, one middle procedure page, and final
revision page at full resolution for every PDF. Any anomaly triggers correction
and complete regeneration of that document.

The K2 completion record reports:

- total PDFs and pages;
- minimum, maximum, and median pages per document;
- all automated validation results;
- visual review coverage and corrected defects; and
- any deliberate limitations that remain.

## Generation completion boundary

The PDF corpus is complete only when all 30 inventory rows have reproducible
sources and PDFs, every file passes the automated and editorial checks, every
page has been rendered and reviewed, and no PDF exceeds 15 pages. Passing file
generation alone is not completion.
