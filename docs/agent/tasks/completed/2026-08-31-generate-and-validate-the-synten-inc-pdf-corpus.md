# Task: Generate and validate the SynTen Inc PDF corpus

Status: Complete
Created: 2026-08-31
Owner: Christopher Guzowski

## Goal

Generate all 30 inventoried SynTen Inc runbooks and policies as realistic,
reproducible, text-based PDFs that pass the corpus contract and never exceed 15
pages each.

## User story

As a payment operations analyst, I want a substantial library of credible
controlled runbooks and policies, so later PDF chunking and retrieval work uses
realistic operational documents rather than small fixtures.

## Context

K1 approved `synten-auth-knowledge/v1`: 22 runbooks and 8 policies covering all
36 synthetic authorization-decline scenarios. The authoring standard defines
real-life document structure, deterministic source/PDF pairing, synthetic-data
guardrails, automated validation, and full render-based visual review.

## Chosen contract

- Generate exactly the 30 document versions in
  `SynTen Inc/corpus/inventory.md`: 27 approved and 3 superseded.
- Maintain one editable Markdown authority and one reproducible PDF for every
  inventory row.
- Follow `synten-pdf-authoring/v1`; document realism comes from useful,
  topic-specific operating content and controlled-document discipline, not
  copied material or filler.
- Every PDF must contain 1-15 pages inclusive. The cover, document control,
  appendices, and revision history all count and there are no exceptions.
- PDFs must be human-readable, text-extractable, unencrypted, visually sound,
  and traceable to exact inventory metadata and source hashes.
- Generation and validation are deterministic and network-free. Do not use a
  live chat model to author or approve corpus content.
- Superseded PDFs remain realistic historical artifacts but display their
  ineligible status and approved replacement prominently.

## In scope

- Topic-specific Markdown sources for all inventoried runbooks and policies.
- A repository-owned deterministic PDF generator and corpus validator under
  `SynTen Inc/`.
- Generated PDFs under `SynTen Inc/corpus/pdfs/`.
- A generated validation manifest with document metadata, hashes, page counts,
  extraction results, generator version, and check outcomes.
- Render every PDF page and perform the visual checks required by the authoring
  standard.
- Correct source, layout, extraction, or rendering defects and regenerate the
  affected documents.

## Out of scope

- Selecting or implementing the production PDF extraction/chunk locator used
  by the copilot API.
- Changing the backend Markdown parser, knowledge schema, retrieval ranking,
  embedding client, or report generation.
- Writing embeddings or vectors to PostgreSQL/pgvector.
- Running Ollama or evaluating live retrieval/report quality.
- Adding another tenant or incident family.

## Constraints

- Follow red-green-refactor for generator and validator behavior.
- Use only fictional SynTen Inc content and opaque identifiers.
- Keep every tenant-specific source, PDF, generator, manifest, render aid, and
  validation asset under `SynTen Inc/`.
- Preserve exact inventory IDs, versions, types, statuses, filenames, incident
  family, applicability, and supersession relationships.
- Do not reduce font size, margins, readability, or content quality to satisfy
  the 15-page maximum.
- Do not hand-edit generated PDFs or commit temporary render output.
- Do not introduce a PDF ingestion dependency into an application service.

## Acceptance criteria

- [x] Exactly 30 maintained Markdown sources and 30 PDFs match the inventory,
      with no missing or extra document.
- [x] Every source and PDF exposes the exact document metadata and a generated
      manifest records source/PDF SHA-256 hashes and generator provenance.
- [x] Every PDF contains 1-15 pages inclusive and automated validation fails
      when a document exceeds the maximum.
- [x] Every runbook and policy passes the real-life editorial checklist with
      topic-specific procedures or controls, ownership, escalation, safety,
      related documents, and revision history appropriate to its type.
- [x] Every PDF is readable, unencrypted, text-extractable in sensible order,
      and contains the exact error-code vocabulary required by its scenario
      coverage.
- [x] Approved PDFs are visibly current; superseded PDFs show a prominent
      ineligible banner and approved replacement on every page.
- [x] All rendered pages pass visual review with no clipping, overlap, broken
      glyphs, accidental blank pages, unreadable tables, or footer collisions.
- [x] Sources and artifacts contain no real payment, person, merchant,
      credential, endpoint, or proprietary company data.
- [x] Focused generator/validator tests, corpus validation, repository static
      verification, and diff checks pass.

## Test plan

- Inventory membership -> validator fails before sources/PDFs exist, then passes
  only with exactly one matching pair per row.
- Page limit -> focused regression rejects a synthetic 16-page PDF and accepts
  the 1-page and 15-page boundaries.
- Metadata and hashes -> focused tests detect mismatched IDs, versions, status,
  filenames, and changed artifacts.
- Extraction -> validator rejects encrypted, malformed, scanned-only, and
  missing-required-text fixtures.
- Supersession -> validator rejects a superseded document without its banner or
  replacement reference.
- Rendering -> render all pages and review the cover, densest table, middle
  procedure/control page, and revision page at full resolution for every PDF.
- Corpus completion -> validate 30 sources, 30 PDFs, all inventory metadata,
  all required error codes, page counts, hashes, and sensitive-pattern results.

## Expected approach

1. Read and follow the repository PDF workflow and load its bundled tooling.
2. Add focused failing tests for membership, metadata, page limits, extraction,
   and superseded-document rules.
3. Implement the smallest reusable generator/validator and one representative
   runbook and policy until the focused checks pass.
4. Author the remaining topic-specific sources from the approved inventory and
   generate all PDFs deterministically.
5. Run corpus validation, render every page, inspect the required visual states,
   correct defects, and rerun the full static gate.

## Likely files or components

- `SynTen Inc/corpus/sources/`
- `SynTen Inc/corpus/pdfs/`
- `SynTen Inc/corpus/validation/`
- `SynTen Inc/corpus/validation-manifest.json`
- `SynTen Inc/corpus/inventory.md`
- `SynTen Inc/corpus/authoring-standard.md`

## Validation commands

The exact commands will use the bundled PDF runtime selected by the repository
PDF workflow and will be recorded after the generator/validator exists.

## Decisions needed

None. K1 fixed the corpus membership, metadata, scenario coverage, authoring
standard, retrieval roles, realism requirements, and 15-page hard limit.

## Progress notes

- 2026-08-31: K1 completed with 30 inventoried PDF versions, full 36-scenario
  and 49-error-code coverage, 23 retrieval cases, and a passing repository
  static gate. K2 was activated from the owner's instruction to begin
  implementation.
- 2026-08-31: Red tests first failed because the corpus validator did not exist;
  the focused suite then covered exact membership, 1/15/16-page boundaries,
  encryption/scanned-only rejection, required metadata/error codes, and
  superseded-document markings.
- 2026-08-31: Implemented a deterministic, network-free ReportLab generator,
  validator, render/contact-sheet helper, and 30 topic-specific Markdown
  authorities. Visual review exposed sparse final runbook pages, so a useful
  investigation record and reviewer handoff table replaced empty space.
- 2026-08-31: A reproducibility comparison failed because the document template
  overrode ReportLab's invariant canvas timestamp. The template was fixed,
  a regression test was added, and two subsequent generations produced
  identical SHA-256 hashes for all 30 PDFs.

## Completion evidence

- Focused tests: bundled Python `-m unittest discover -s
  "SynTen Inc/corpus/validation" -p "test_*.py" -v` passed 6/6.
- Corpus validation: `validate_corpus.py` passed 30/30 PDFs and emitted
  `validation-manifest.json`; the corpus has 112 pages, minimum 3, maximum 4,
  and median 4.0 pages.
- Determinism: SHA-256 comparison before and after a complete regeneration
  passed for all 30 PDFs.
- Rendering: `render_corpus.py` rendered all 112 pages and 30 contact sheets.
  Every final page was visually reviewed; no clipping, overlap, blank/orphan
  page, unreadable table, broken glyph, or footer collision remained.
- Supersession: RB-022, PL-007, and PL-008 display the ineligible banner and
  approved replacement on every page.
- Static verification: `.\verify.ps1 -Scope Repository` passed repository-tool,
  verification-system, Java, Compose, and diff checks.

## Remaining limitations

The completed PDFs are not yet ingested by `knowledge.catalog`. PDF extraction,
stable page/source locators, deterministic chunking, embedding, vectorization,
retrieval measurement, and live-model evaluation remain K3-K5.

