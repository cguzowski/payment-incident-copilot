# Task: Catalog the SynTen Inc PDFs with exact page provenance

Status: Implementation in progress — Docker-backed PostgreSQL acceptance pending
Created: 2026-08-31
Owner: Christopher Guzowski

## Goal

Implement ADR-0009 end to end: validate and parse all 30 SynTen Inc PDFs into
deterministic page-aware chunks, persist the complete catalog without requiring
a model, make approved chunks available to existing lexical retrieval, and
show exact PDF/page citations while preserving the completed Markdown path.

## User story

As a payment operations analyst, I want a retrieved SynTen Inc excerpt to name
the exact PDF version, SHA-256, page, and block range that produced it, so I can
review the source and trust that superseded or changed guidance did not enter
the result.

## Context

K1 defined `synten-auth-knowledge/v1`; K2 generated and validated 30 maintained
Markdown/PDF pairs totaling 112 pages; ADR-0009 selected PDFBox 3.0.8,
`pdfbox-text-pages/v1`, and `pdf-page-sections/v1`. The application currently
persists embedded chunks from two classpath Markdown documents. K3 adds a
parallel explicit catalog import for the repository-owned SynTen corpus. K4,
not this task, will populate PDF embeddings and measure hybrid retrieval with
Ollama.

## Chosen contract

- Pin `org.apache.pdfbox:pdfbox:3.0.8` in `copilot-api`.
- Read SynTen membership and artifact hashes from
  `SynTen Inc/corpus/validation-manifest.json`; read catalog metadata from each
  paired maintained source front matter; verify both artifacts before parsing.
- Require the exact SynTen tenant, 30 unique manifest entries, 1-15 pages,
  unencrypted text on every page, supported `RUNBOOK`/`POLICY` type, and
  `APPROVED`/`SUPERSEDED` status. Reject the whole import before writes when any
  input is invalid.
- Normalize and parse physical pages exactly as ADR-0009 specifies. Remove only
  the exact validated generated header/footer; retain superseded banners,
  ordered table text, headings, lists, machine codes, and page boundaries.
- Use 1-based inclusive PDF page/block locators. A chunk never crosses a page.
  Use `pdf-page-sections/v1` with target 400, maximum 600, overlap 50, and
  preferred minimum 80 estimated tokens.
- Persist all 30 versions and their chunks. Superseded chunks remain auditable
  but the existing approval filter excludes them before ranking.
- PDF chunks have a complete embedding-input wrapper and hashes but an absent
  embedding tuple. Embedding model, dimensions, normalized flag, timestamp,
  and vector are either all present or all absent.
- Existing lexical search includes approved unembedded PDF chunks. Existing
  vector search ignores chunks without a compatible complete embedding.
- Retrieval-result snapshots and API responses copy source name, source format,
  PDF hash, and the PDF page/block range. Historical Markdown results continue
  to use line ranges.
- The operator console labels either `PDF page N, blocks A-B` with filename and
  hash or the historical Markdown line range. No raw repository path is sent
  to the browser.
- Import remains explicit and disabled by default. A configured corpus root is
  required when PDF catalog import is enabled; normal startup and automated
  tests do not depend on a working-directory-relative production default.
- Existing Markdown ingestion, retrieval ranking constants, report schema,
  report source-reference validation, tenant boundary, and incident family do
  not change.

## In scope

- PDFBox dependency and immutable PDF parsed-document/page/block records.
- Manifest and paired-source metadata loading under `knowledge.catalog`.
- PDF validation, normalization, header/footer removal, extraction, hashing,
  deterministic page-aware chunking, and stable IDs.
- An explicit all-or-nothing SynTen PDF catalog import path.
- Flyway V9 catalog, nullable-embedding, chunk-locator, and retrieval-snapshot
  evolution with existing-row backfill and constraints.
- Lexical retrieval of approved unembedded chunks and vector null-safety.
- Additive retrieval API and operator-console provenance rendering.
- Documentation, task evidence, and deterministic verification.

## Out of scope

- Generating or editing the K2 sources or PDFs.
- Calling Ollama, downloading models, creating PDF embeddings, or measuring
  semantic/hybrid retrieval quality; those are K4.
- OCR, passwords, scanned documents, rotated/multi-column generalization,
  semantic table reconstruction, or arbitrary external PDF ingestion.
- Continuous file watching, uploads, object storage, or a content-management
  API.
- Serving PDF bytes from the API or adding a browser PDF viewer.
- Re-ranking changes, a second incident family, authentication, or deployment.

## Constraints

- Follow ADR-0009 and do not weaken a fail-closed rule to admit a fixture.
- All SynTen-specific data and fixtures stay under `SynTen Inc/`; shared Java,
  schema, UI, and project documentation stay in their existing boundaries.
- Use TDD for every behavior change and record the intended red failure before
  production code.
- Keep automated verification network-free and model-free after Maven resolves
  the pinned build dependency.
- Preserve immutable historical retrieval/report references and all existing
  V1-V8 Flyway checksums.
- Do not expose filesystem paths, source Markdown bodies, or whole PDFs through
  the retrieval API.

## Acceptance criteria

- [x] `SynTenCorpusSourceRepositoryTest.loadsTheExactThirtyManifestVersionsInOrder`
      proves exact membership, tenant, metadata, maintained-source/PDF hashes,
      and 27 approved plus 3 superseded versions.
- [x] Repository failure tests reject source/PDF hash mismatch, duplicate
      tenant/document/version, missing/extra artifact, unsupported metadata,
      path escape, wrong tenant, and manifest/source disagreement before parse.
- [x] `PdfBoxKnowledgeDocumentParserTest.extractsDeterministicPageBlocksAndRemovesOnlyGeneratedMargins`
      covers RB-002 and exact page counts/block order across two parses.
- [x] Parser tests cover PL-001 policy structure, RB-011 table reading order,
      retained RB-022 superseded banners, CR/LF/NBSP/NFC normalization, and
      exact page locators.
- [x] Parser failure tests reject encrypted, malformed, empty/scanned-only,
      zero-page or over-15-page, missing expected margin, and page-count-
      mismatch PDFs with bounded non-sensitive errors.
- [x] `PdfKnowledgeChunkerTest.neverCrossesPagesAndCarriesSectionContext`
      proves deterministic order/IDs, page/block ranges, section carry-forward,
      table-line order, target/hard maximum, same-page overlap, short-tail
      merging, and oversized-block splitting.
- [ ] `SynTenPdfCatalogImportServiceTest.validatesTheWholeCorpusBeforeOneAtomicWrite`
      proves all-or-nothing validation, stable repeat imports, changed-content
      rejection, 30 catalogued versions, and no embedding calls.
- [ ] Flyway V9 preserves V1-V8 checksums and enforces source-format/hash,
      exactly-one-locator-family, PDF range, all-or-none embedding tuple,
      tenant foreign-key, and 600-token constraints for both legacy and PDF
      rows.
- [ ] `SynTenPdfCatalogPostgresIntegrationTest.catalogsThirtyVersionsWithStablePdfLocators`
      stores the real corpus with 27 approved and 3 superseded versions and
      obtains the same document/chunk IDs and hashes on a repeat plan.
- [ ] Hybrid-search integration proves an approved unembedded PDF chunk is
      lexically eligible, absent from vector ranking, tenant-filtered, and that
      an exact superseded near-match is excluded before ranking.
- [ ] Retrieval persistence and HTTP tests prove source format, safe filename,
      PDF hash, and page/block range are copied into immutable attempt history;
      Markdown line locators remain backward compatible.
- [x] Operator-console tests render PDF citation fields for PDF results and
      line ranges for Markdown results with no regression to loading, history,
      retry, or status states.
- [ ] Focused backend/frontend suites, PostgreSQL integration tests, Spotless,
      Prettier, builds, Compose validation, `git diff --check`, and the
      authoritative `./verify.ps1` gate pass with zero skipped tests.

## Test plan and red-green order

1. Red: add `SynTenCorpusSourceRepositoryTest` membership/hashing and rejection
   cases before implementing manifest/front-matter loading.
2. Green: add only the immutable source descriptors and safe filesystem loader.
3. Red: add `PdfBoxKnowledgeDocumentParserTest` representative and failure
   cases before adding PDFBox parser production code.
4. Green: implement `pdfbox-text-pages/v1` and immutable page/block records.
5. Red: add `PdfKnowledgeChunkerTest` before implementing
   `pdf-page-sections/v1` and stable chunk IDs.
6. Red: add V9 schema assertions and catalog persistence tests before the
   migration and repository changes.
7. Red: add import-service atomicity/idempotency tests before the explicit
   import service/command.
8. Red: add lexical/vector eligibility and locator snapshot tests before
   changing search and retrieval persistence.
9. Red: add Angular provenance rendering tests before model/template changes.
10. Green/refactor: run focused suites after each behavior, then backend,
    frontend, repository, and authoritative gates.

## Likely files or components

- `backend/copilot-api/pom.xml`
- `backend/copilot-api/src/main/java/.../knowledge/catalog/`
- `backend/copilot-api/src/main/java/.../knowledge/retrieval/`
- `backend/copilot-api/src/main/resources/db/migration/V9__*.sql`
- `backend/copilot-api/src/test/java/.../knowledge/`
- `frontend/operator-console/src/app/features/investigation-workspace/approved-knowledge-panel/`
- `SynTen Inc/corpus/validation-manifest.json`
- `docs/agent/STATUS.md`

## Validation commands

```powershell
./mvnw.cmd -pl backend/copilot-api -Dtest=SynTenCorpusSourceRepositoryTest,PdfBoxKnowledgeDocumentParserTest,PdfKnowledgeChunkerTest,SynTenPdfCatalogImportServiceTest test
./mvnw.cmd -pl backend/copilot-api -Dtest=KnowledgeSchemaPostgresIntegrationTest,SynTenPdfCatalogPostgresIntegrationTest,KnowledgeHybridSearchPostgresIntegrationTest,KnowledgeRetrievalPersistencePostgresIntegrationTest,KnowledgeRetrievalApiPostgresIntegrationTest test
./verify.ps1 -Scope Backend
./verify.ps1 -Scope Frontend
./verify.ps1 -Scope Repository
./verify.ps1
```

## Decisions needed

None. ADR-0009 and this contract authorize implementation.

## Progress notes

- 2026-08-31: ADR-0009 accepted after repeatable PDFBox 3.0.8 probes over
  RB-002, PL-001, RB-011, and RB-022. The design-task repository gate passed.
- 2026-08-31: Implemented fail-closed manifest/source validation, PDFBox page
  extraction, deterministic page-confined chunking, stable IDs/hashes, and an
  explicit all-or-nothing catalog command. A real-corpus model-free test parsed
  all 30 PDFs into 180 chunks with 27 approved and 3 superseded versions.
- 2026-08-31: Added Flyway V9, nullable all-or-none embedding support,
  PostgreSQL catalog/search/snapshot contracts, additive API provenance, and
  operator citations for PDF page/block or historical Markdown line locators.
- 2026-08-31: Red-green evidence includes repository, PDF parser/chunker,
  import orchestration, API, and Angular citation tests. A late fail-closed
  audit added and proved rejection of `DRAFT` corpus metadata after its focused
  test first failed for accepting it.
- 2026-08-31: Docker Desktop reported an error. Work continued through every
  model-free path; Testcontainers was not treated as passing or replaced with
  skipped-test evidence.
- 2026-08-31: Reproduced the reported Maven startup exit. With `.env` loaded,
  the actionable cause was Spring constructor ambiguity in
  `SynTenCorpusSourceRepository`. Added a context regression that failed with
  `No default constructor found`, marked the configuration constructor for
  injection, and proved the regression green.

## Completion evidence

- `./mvnw.cmd "-Dtest=!*PostgresIntegrationTest" verify` passed 159 copilot API
  and 9 operations MCP tests with zero failures, errors, or skips, including
  Spotless and both deployable JAR builds.
- With the ignored root `.env` loaded, `./mvnw.cmd -pl backend/copilot-api
  spring-boot:run` started the API against native PostgreSQL 18.3, validated
  Flyway V1-V9 at schema version 9, and returned `UP` from
  `/actuator/health` before a graceful shutdown.
- The focused PDF/API suite passed 28/28 tests with zero skips; final parser and
  corpus validation reruns passed 12/12 after the fail-closed metadata audit.
- `./verify.ps1 -Scope Frontend` passed locked installation with zero audit
  vulnerabilities, 78/78 tests with zero skips, Prettier, and the 389.32 kB
  raw production build.
- `./verify.ps1 -Scope Repository` passed verification-system tests, Compose
  configuration, and `git diff --check`.
- All 56 copilot API test sources, including the new PostgreSQL integration
  contracts, compile successfully.
- Completion is not claimed until the Docker-backed tests and authoritative
  zero-skip gate execute successfully.

## Remaining limitations

- Docker Desktop is stopped after previously reporting an error, so the new atomic catalog,
  idempotency/conflict, lexical eligibility, vector null-safety, and immutable
  snapshot integration tests have not executed. Flyway V9 itself is verified
  on native PostgreSQL 18.3, but the backend and aggregate verification gates
  remain pending.
- The explicit PDF catalog command has not been run against a real PostgreSQL
  database in this environment. PDF embeddings and live-model evaluation
  remain intentionally deferred to K4.
