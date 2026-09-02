# ADR-0002: Hybrid tenant-scoped operational-knowledge retrieval

Status: Accepted; provider-specific portions superseded by ADR-0007 and K5
candidate pooling/context selection superseded by ADR-0011
Date: 2026-08-28
Decision owner: Christopher Guzowski

ADR-0007 changes the active local embedding provider, model, dimensions, and
test-provider boundary. ADR-0011 changes how the fixed candidate depth is
allocated and how repeated document chunks enter final context. This record
remains authoritative for the underlying hybrid-search approach, chunking,
RRF, provenance, and the historical Titan index contract.

## Context

The copilot must retrieve approved runbooks and policies for an investigation
before generating an AI-assisted report. Retrieval must work for exact payment
operations terminology as well as semantically similar incident descriptions,
remain explainable and tenant-safe, preserve enough metadata for audit, and fit
the existing PostgreSQL/pgvector and Amazon Bedrock constraints.

The initial corpus is intentionally small. The design should favor correctness,
determinism, and reviewability over approximate-search scale or additional
managed infrastructure.

## Decision drivers

- Tenant and approval filtering before any candidate can influence a result
- Good recall for both exact error codes and semantically related guidance
- Reproducible chunks, embeddings, ranking, and selected context
- Compatibility with PostgreSQL/pgvector and Amazon Bedrock
- Bounded context for later structured report generation
- Simple local and CI testing without committed AWS credentials
- No infrastructure optimized for hypothetical corpus size

## Considered options

### PostgreSQL full-text search only

- Advantages: Deterministic, inexpensive, locally testable, and strong for exact
  terms such as error codes and service names.
- Disadvantages: Weak semantic recall when incident language differs from the
  approved document wording.

### Vector search only

- Advantages: Strong semantic matching and a simple conceptual retrieval path.
- Disadvantages: Can underweight exact operational identifiers, requires an
  embedding call for every query, and is harder to explain alone.

### Hybrid full-text and vector retrieval with rank fusion

- Advantages: Preserves exact-match and semantic signals, avoids comparing
  incompatible raw scores, and remains implementable inside PostgreSQL.
- Disadvantages: Adds two candidate lists, fusion parameters, and more metadata
  that must be persisted and tested.

### External managed search or vector service

- Advantages: Mature indexing and ranking capabilities at larger scale.
- Disadvantages: Adds infrastructure, cost, security boundaries, and operational
  complexity that the initial corpus does not require.

## Decision

Use the following starting configuration:

| Decision | Selected starting point |
|---|---|
| Embedding model | Amazon Titan Text Embeddings V2 |
| Model ID | `amazon.titan-embed-text-v2:0` |
| Dimensions | 1,024 |
| Vector format | Float, normalized |
| Distance | Cosine |
| Chunking | Markdown-aware, section/block based |
| Chunk target | Approximately 400 tokens |
| Hard maximum | Approximately 600 tokens |
| Overlap | 40–60 tokens, only within the same section |
| Chunk representations | Exact `raw_content` plus enriched `embedding_input` |
| Search | Metadata filters + PostgreSQL full-text + vector |
| Fusion | Reciprocal Rank Fusion |
| Initial indexing | Exact pgvector search; no HNSW yet |
| Final context | Roughly 4 runbook chunks + 3 policy chunks |

Apply tenant, approval/effective-version, document-type, and incident-family
metadata filters before full-text or vector ranking. Independently rank the
eligible full-text and exact cosine-vector candidates, then combine their rank
positions with Reciprocal Rank Fusion rather than combining raw scores.

Retrieve at most 20 candidates from each modality and use Reciprocal Rank
Fusion with `k = 60`. Require PostgreSQL cover-density rank greater than zero
for lexical candidates and cosine similarity of at least `0.55` for vector
candidates. Resolve equal fused scores by best modality rank, document type,
document identifier, document version, and chunk ordinal.

If query embedding fails, run lexical retrieval and mark eligible results
`PARTIAL`. If lexical retrieval also yields no eligible chunk, retain the
embedding failure as `UNAVAILABLE`, `TIMED_OUT`, or `MALFORMED`; do not call it
a successful no-match.

The final context targets four runbook chunks and three policy chunks. It may
contain fewer chunks when the eligible corpus or relevance rules do not support
that target. Retrieval must never use an unapproved, cross-tenant, superseded,
or weak chunk merely to fill the context allocation.

Maintain two immutable forms for every chunk:

- `raw_content` contains the exact source text selected by the chunker. It is
  the only text displayed to the operator or reproduced in a citation.
- `embedding_input` enriches that exact content with approved metadata and is
  the only text sent to the embedding model. It is not itself citable source
  text.

Construct `embedding_input` in this exact versioned layout:

```text
Document: Authorization Decline Runbook
Section: Gateway Failures > Diagnosis
Type: RUNBOOK
Applies to: Card authorization

[exact chunk content]
```

The displayed values come from the approved document metadata and chunk. The
final line is the exact persisted `raw_content`. Persist both representations,
separate hashes, the embedding-input template version, and source line metadata.
Changing the raw text or any metadata included in `embedding_input` creates a
new chunk/index version and requires a new embedding.

Build PostgreSQL full-text search from separately weighted metadata and
`raw_content`, not from the rendered embedding wrapper. Never expose
`embedding_input` as source content through the operator API or UI.

Ingestion and retrieval metadata must include the model identifier, dimensions,
normalization setting, chunking strategy version, document and chunk versions,
raw and embedding-input hashes, embedding-input template version,
query/template version, metadata filters, independent ranks, fused rank, and
final selection position.

Automated tests may replace Bedrock with a deterministic adapter implementing
the same 1,024-dimension normalized-vector contract. Normal runtime uses Amazon
Titan Text Embeddings V2, and an explicitly invoked authorized smoke test
verifies the real provider boundary.

## Rationale

The chosen approach matches the strengths of the incident domain. Exact error
codes, service names, policy phrases, and identifiers are valuable lexical
signals, while incident summaries and runbook prose also require semantic
matching. Reciprocal Rank Fusion combines those rankings without pretending
that PostgreSQL full-text scores and cosine distance have the same scale.

Exact pgvector search is appropriate for the initial small approved corpus and
keeps ranking behavior easier to test. Approximate indexing should be justified
by measured corpus size and latency rather than added preemptively.

Markdown-aware chunking preserves operational document structure. The target,
hard maximum, and within-section overlap keep later report context bounded while
retaining local continuity and preventing unrelated sections from bleeding into
one another.

## Consequences

### Positive

- Exact operational terms and semantic similarity both influence retrieval.
- Every candidate and selected chunk can be explained through source metadata
  and independent/fused ranks.
- Operators and later report citations retain exact source fidelity while the
  embedding receives useful document and section context.
- Tenant and approval constraints are enforced before ranking.
- PostgreSQL remains the only required retrieval infrastructure.
- The report-generation slice receives a small, typed, persisted context rather
  than an unbounded or silently re-run search.

### Negative or accepted tradeoffs

- Ingestion and queries depend on access to a specific Bedrock embedding model
  outside deterministic tests.
- The vector column dimension and embedding metadata couple an index version to
  the selected model configuration.
- Hybrid retrieval requires explicit candidate-depth, fusion, relevance, and
  tie-breaking rules.
- Storing two text forms uses additional space, and a metadata change in the
  embedding wrapper requires re-embedding even when `raw_content` is unchanged.
- Exact vector search will eventually become slower than approximate indexing
  if the corpus grows materially.
- A fixed runbook/policy target may return fewer than seven chunks and must not
  be presented as evidence completeness.

## Validation or revisit trigger

Revisit this decision when measured PostgreSQL retrieval latency or corpus size
justifies HNSW, retrieval evaluation shows that Reciprocal Rank Fusion
underperforms a simpler or weighted method, the Bedrock model becomes
unavailable or materially uneconomic in the target region, a different
embedding dimension is required, or report-quality evaluation shows that the
chunking and four-plus-three context target omit necessary guidance.
