# Task: Select the PDF extraction, locator, and chunking contract

Status: Ready
Created: 2026-08-31
Owner: Christopher Guzowski

## Goal

Accept the architectural contract that will let `knowledge.catalog` ingest the
SynTen Inc PDFs into deterministic, versioned chunks with stable source
locations before executable ingestion changes begin.

## User story

As a payment operations analyst, I want every retrieved excerpt to resolve to
the exact PDF version and location that produced it, so I can review the source
and distinguish approved guidance from superseded or malformed material.

## Context

K1 fixed `synten-auth-knowledge/v1`; K2 generated and validated 30 maintained
Markdown/PDF pairs totaling 112 pages. The current application ingests two
Markdown documents through `knowledge.catalog` under ADR-0002. K3 must revise
that Markdown-only decision deliberately rather than inserting a PDF parser or
page assumptions into the existing path without an accepted contract.

## Chosen contract

- This task selects and records the contract; it does not implement ingestion.
- The accepted ADR must precede any executable PDF catalog change.
- Existing Markdown ingestion behavior and public retrieval/report behavior
  remain unchanged during this task.
- K3 v1 may support the maintained text-based, unencrypted SynTen Inc PDFs and
  fail closed on encrypted, malformed, scanned-only, or unsupported PDFs; OCR
  is not required.
- Every extracted representation and chunk must retain tenant identity, exact
  document ID/version/status, source and PDF hashes, parser version, chunker
  version, and a stable human-reviewable PDF locator.
- Chunking must be deterministic, bounded, section-aware where feasible, and
  independently testable without a live embedding or chat model.
- Superseded documents remain auditable inputs but must not become retrieval-
  eligible content.

## In scope

- Inspect the current `knowledge.catalog` source, parser, chunking, persistence,
  tests, ADR-0002, and Flyway schema relevant to knowledge versions and chunks.
- Compare the smallest credible Java PDF extraction choices against the actual
  corpus and repository constraints.
- Define text normalization, page/section locator semantics, chunk identity and
  ordering, metadata/version propagation, failure behavior, and migration
  compatibility.
- Record the accepted decision as a new ADR and update architecture/quality
  documentation and the implementation test map.
- Prove the selected extractor/locator assumptions against representative
  approved and superseded corpus PDFs without writing embeddings or database
  rows.

## Out of scope

- Production PDF ingestion code, schema migrations, or API changes.
- Embedding generation, pgvector writes, hybrid retrieval measurement, or live
  Ollama calls.
- OCR, scanned-document support, password handling, external document stores,
  or continuous content synchronization.
- Changing the K1 inventory, K2 PDFs, existing retrieval ranking, report schema,
  tenant boundary, or incident family.

## Constraints

- Follow the repository documentation and ADR process.
- Prefer the smallest dependency and representation that preserve correct text
  order and stable locators for the real K2 corpus.
- Do not infer page numbers from text alone or silently discard extraction
  failures, empty pages, duplicate versions, or superseded status.
- Do not claim byte offsets are stable unless the selected library and contract
  can actually guarantee them.
- Keep automated checks deterministic and network-free.
- Keep every exploratory extraction artifact under `SynTen Inc/validation` or
  a temporary ignored path and do not commit render output.

## Acceptance criteria

- [ ] A new accepted ADR selects the PDF library and immutable parsed-document
      representation, with rejected alternatives and tradeoffs recorded.
- [ ] The ADR defines a stable locator that can take a reviewer from a chunk to
      an exact PDF version and page, plus section/block context where reliable.
- [ ] Text normalization, repeated headers/footers, page boundaries, table
      handling, empty/invalid input, and deterministic ordering are explicit.
- [ ] Chunk identity, maximum/minimum sizing, overlap or non-overlap behavior,
      source-hash/parser-version/chunker-version propagation, and regeneration
      semantics are explicit.
- [ ] Approval/effective/superseded behavior and tenant propagation remain
      compatible with current persistence and retrieval guardrails.
- [ ] Representative extraction probes pass against an approved runbook, an
      approved policy, the densest table-oriented runbook, and a superseded
      document, with exact page locators reviewed.
- [ ] The follow-on implementation task maps every contract rule to a named
      red-green test and identifies any required migration without starting it.
- [ ] Repository static verification and diff checks pass.

## Test plan

- Parser feasibility -> extract representative PDFs twice and compare ordered
  page/block output and hashes.
- Locator feasibility -> resolve selected excerpts back to exact document ID,
  version, PDF hash, page, and stable section/block context.
- Layout cases -> inspect headings, lists, tables, repeated headers/footers,
  long machine codes, and superseded banners.
- Failure contract -> define tests for encrypted, malformed, empty/scanned-only,
  duplicate, wrong-tenant, and unsupported-version inputs.
- Compatibility -> map the new parsed/chunk metadata to current catalog records,
  Flyway constraints, retrieval filters, and immutable snapshot references.

## Expected approach

1. Read ADR-0002 and the current catalog parser/chunker, schema, and tests.
2. Probe representative K2 PDFs with the candidate extraction library or
   libraries using repository-local, non-production code.
3. Choose the smallest contract that preserves ordered text and reviewable
   page provenance for the actual corpus.
4. Write and review the ADR, update architecture/quality references, and create
   the locked implementation task with named red-green tests.
5. Run repository static verification and archive this task only after the ADR
   and implementation contract are complete.

## Likely files or components

- `docs/agent/decisions/`
- `docs/agent/ARCHITECTURE.md`
- `docs/agent/QUALITY.md`
- `docs/agent/tasks/current.md`
- `backend/copilot-api/src/main/java/.../knowledge/catalog/`
- `backend/copilot-api/src/test/java/.../knowledge/catalog/`
- `backend/copilot-api/src/main/resources/db/migration/`
- `SynTen Inc/corpus/pdfs/`
- `SynTen Inc/corpus/validation-manifest.json`

## Validation commands

The exact representative extraction probes and focused test commands will be
recorded after the current catalog and candidate library APIs are inspected.
Repository completion uses `.\verify.ps1 -Scope Repository`.

## Decisions needed

- Select the Java PDF extraction library and its pinned version.
- Select the canonical parsed-document representation and stable locator.
- Select the deterministic chunk boundaries and version identifiers that will
  revise ADR-0002 for PDF sources.

## Progress notes

- 2026-08-31: K2 completed with 30 deterministic PDFs, 112 visually reviewed
  pages, exact manifest hashes, and a hard observed maximum of 4 pages against
  the 15-page contract. K3 contract selection is ready.

## Completion evidence

Not complete.

## Remaining limitations

No PDF extraction/locator ADR has been accepted and the application still
ingests only its two baseline Markdown sources.
