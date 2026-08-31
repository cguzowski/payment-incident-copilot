# ADR-0009: Page-aware PDF knowledge catalog

Status: Accepted
Date: 2026-08-31
Decision owner: Christopher Guzowski

## Context

ADR-0002 selected Markdown-aware chunks for the first two approved knowledge
documents. The SynTen Inc expansion now contains 30 maintained, text-based PDFs
whose future retrieval results must resolve to an exact PDF version and page.
The catalog needs a deterministic extraction and chunking contract before it
can persist those documents without weakening tenant, approval, or audit
boundaries.

K3 must remain independent of a live embedding or chat model. It may make
approved PDF chunks available to the existing lexical retrieval path, but K4
owns embedding those chunks and measuring hybrid retrieval with the configured
local model.

## Decision drivers

- Exact, human-reviewable PDF page provenance
- Deterministic, network-free extraction and chunking tests
- Fail-closed handling of malformed, encrypted, scanned-only, or changed input
- Compatibility with the existing catalog, retrieval snapshots, and reports
- A small Java dependency surface inside `knowledge.catalog`
- Preservation of superseded versions for audit without retrieval eligibility

## Considered options

### Apache PDFBox 3.0.8

- Advantages: focused Java PDF dependency, Unicode text extraction, explicit
  page ranges, byte-array loading, active security fixes, and no runtime
  service boundary.
- Disadvantages: tables are emitted in reading order rather than as a semantic
  table model, and section recognition remains an application concern.

### Apache Tika

- Advantages: broad document-format detection and a higher-level parsing API.
- Disadvantages: a materially larger dependency and parser surface than this
  PDF-only corpus requires, while still requiring custom page locators.

### Python extraction sidecar

- Advantages: the corpus tooling already uses Python PDF libraries.
- Disadvantages: adds a second runtime, process boundary, deployment contract,
  and failure surface to the Java catalog for no K3 capability gain.

### Continue indexing maintained Markdown only

- Advantages: reuses the current parser and line locators.
- Disadvantages: cannot prove that a retrieved excerpt came from the exact PDF
  that an operator reviews and therefore does not meet the expansion goal.

## Decision

### Parser and supported input

Pin `org.apache.pdfbox:pdfbox:3.0.8` in the copilot API. Load bytes with the
PDFBox 3 `Loader` API and extract each physical page independently with
`PDFTextStripper`. The extraction strategy identifier is
`pdfbox-text-pages/v1`.

K3 supports only manifest-listed SynTen Inc PDFs that are unencrypted,
text-based, and between 1 and 15 pages inclusive. Before parsing, ingestion
must verify the maintained-source SHA-256 and PDF SHA-256 against
`SynTen Inc/corpus/validation-manifest.json`, validate the matching source front
matter, and reject duplicate tenant/document/version entries. It must reject
an encrypted PDF even when an empty password could open it. Malformed PDFs,
page-count mismatches, empty pages, scanned-only pages, unsupported metadata,
hash mismatches, and any tenant other than the manifest tenant fail the whole
catalog import before persistence. OCR and password handling are not part of
this contract.

### Immutable parsed representation and normalization

The parser returns an immutable document containing the validated tenant and
document metadata, maintained-source hash, exact PDF hash, parser version, and
an ordered list of physical pages. A page contains its 1-based page number and
an ordered list of 1-based normalized text blocks.

Normalization is versioned as part of `pdfbox-text-pages/v1`:

1. normalize CRLF and CR to LF and Unicode text to NFC;
2. replace non-breaking spaces with ASCII spaces;
3. collapse horizontal whitespace within an extracted line, trim line edges,
   and remove empty outer lines while preserving non-empty line order;
4. remove only the exact generated header and page-footer lines derived from
   the validated document key, title, version, classification, document ID,
   page number, and page count;
5. retain superseded banners, headings, lists, wrapped prose, machine codes,
   and table text in the order PDFBox emitted them; and
6. never repair hyphenation, infer table cells, invent missing text, or join
   content across a physical page boundary.

A normalized non-empty line is one block. This deliberately modest block model
does not claim that PDF byte offsets, glyph offsets, or semantic table cells
are stable.

### Locator and chunks

The stable PDF locator consists of:

- source name and format `PDF`;
- exact PDF SHA-256;
- 1-based start and end page; and
- 1-based inclusive start and end block within those pages.

K3 chunks never cross a physical page, so start and end page are equal. The
page is the authoritative reviewer locator; section path and block range are
additional context that is stable only for the pinned PDF hash and extraction
strategy.

Use chunking strategy `pdf-page-sections/v1`. Recognize numbered operational
headings and the corpus's document-control/revision headings, carry the current
section path onto the next page, and split only at block boundaries. Retain
ADR-0002's approximate 400-token target, 600-token hard maximum, and 50-token
overlap, but apply overlap only within the same page and section. Prefer a
minimum of 80 tokens by merging a short trailing chunk backward when the hard
maximum permits; a naturally short section or page may remain below 80 tokens.
An oversized single block is split on word boundaries. Table lines retain
their extracted order and receive no invented column delimiters.

Chunk ordinals are deterministic across the complete document. A PDF chunk ID
is a name-based UUID over tenant ID, document ID, document version, PDF hash,
extraction strategy, chunking strategy, chunk ordinal, page/block locator, and
raw-content hash. Repeating the import with identical inputs produces the same
representation, chunks, hashes, and identifiers. Changed bytes under an
existing document version are rejected. A future parser or chunker change must
use a new strategy identifier and an explicit catalog migration or a new source
document version; existing referenced chunks are never silently rewritten.

The existing versioned embedding-input wrapper remains unchanged. The exact
normalized PDF chunk is `raw_content`; the enriched wrapper is
`embedding_input` and is not citable source text.

### Persistence and retrieval compatibility

Flyway extends `knowledge_document_version` with source format, maintained-
source hash, optional PDF hash, and extraction strategy version. It extends
`knowledge_chunk` and immutable retrieval-result snapshots with nullable line
locators plus PDF page/block locators. A constraint requires exactly the
locator family appropriate to `MARKDOWN` or `PDF`.

K3 may persist PDF chunks without embedding metadata or a vector. Those fields
must be either all present or all absent. PostgreSQL full-text search includes
approved unembedded chunks; vector ranking includes only chunks with a complete
embedding matching the query model and dimensions. K4 fills the embedding
boundary. Existing Markdown rows and historical Titan/Ollama rows retain their
current values and behavior.

All 30 manifest versions are catalogued. The three `SUPERSEDED` versions and
their chunks remain queryable for audit and validation inside the catalog but
are excluded by the existing approval/effective filter before lexical or
vector ranking. Retrieval snapshots and the operator response copy source
format, source name, PDF hash, and page/block locator so a later corpus change
cannot alter prior citations.

Catalog import remains an explicit operator/development command and is
all-or-nothing for the manifest. It does not become a continuous file watcher.

### Representative probes

PDFBox 3.0.8 extracted the following PDFs twice with identical ordered output:

| Probe | Result and reviewed locator |
|---|---|
| RB-002 approved runbook | Four pages; document control on page 2, diagnostic procedure on page 3, escalation and closure on page 4 |
| PL-001 approved policy | Three pages; document control on page 2 and roles/evidence records on page 3 |
| RB-011 densest approved runbook | Four pages; signal table and diagnostic procedure on page 3, escalation and closure on page 4 |
| RB-022 superseded runbook | Four pages; superseded/replacement banner extracted on every page and retained as content |

The probes produced no committed extraction or render artifact. The K2 visual
review remains authoritative for PDF appearance; these probes validate the
Java extraction and page-locator assumptions.

## Rationale

PDFBox is the smallest credible in-process Java choice for the corpus and gives
the catalog direct control over page-by-page extraction and failure handling.
An exact page plus PDF hash is understandable to an operator and does not make
an unsupported promise about PDF byte offsets. Corpus-derived header/footer
validation avoids a heuristic that might silently remove legitimate guidance.

Persisting unembedded chunks separates K3's source-of-truth problem from K4's
model installation and vector-quality problem. The existing approval filter
continues to protect retrieval even though historical versions remain
auditable.

## Consequences

### Positive

- Every PDF-derived result resolves to exact immutable bytes and a physical
  page, with deterministic block context.
- The entire corpus can be catalogued and lexically exercised without Ollama.
- Superseded near-matches create realistic exclusion pressure without entering
  retrieval candidates.
- Existing Markdown ingestion and historical embeddings remain compatible.

### Negative or accepted tradeoffs

- PDF text is a normalized extraction, not the original visual layout.
- Tables remain ordered text; semantic row/column reconstruction is deferred.
- The v1 header/footer contract is intentionally specific to the generated
  SynTen corpus and fails closed when that layout changes.
- A parser or chunker upgrade requires an explicit migration/reindex decision.
- K3 results are lexical-only until K4 writes compatible embeddings.

## Validation or revisit trigger

Revisit this decision when the corpus adds scanned, encrypted, rotated,
multi-column, or non-SynTen PDFs; when extraction evaluation finds incorrect
reading order or table loss; when documents exceed the bounded in-memory
assumptions; or when a measured reindex requirement justifies multiple active
catalog generations per document version.
