# Task: Retrieve approved operational knowledge

Status: Implementation Complete; External Verification Pending
Created: 2026-08-28
Owner: Christopher Guzowski

## Goal

Give a payment operations analyst one complete approved-knowledge workflow:
ingest versioned synthetic Markdown guidance, retrieve the most relevant
tenant-approved runbook and policy chunks for an existing investigation, retain
the exact retrieval context and provenance in PostgreSQL, and present the
results or explicit retrieval limitation in the investigation workspace.

## User story

As a payment operations analyst, I want to retrieve approved guidance relevant
to an authorization decline-rate investigation so that I can compare observed
operational evidence with sanctioned runbook and policy information before an
AI-assisted report is generated.

## Context

The application can ingest a synthetic alert, retain the incident in one work
queue, start or resume an investigation, invoke `getRecentServiceErrors`
through MCP, preserve every evidence attempt, and display observed evidence
with its provenance.

The next report-generation slice needs a stable, tenant-scoped retrieval
context. PostgreSQL already enables pgvector, but the repository has no
knowledge document model, Markdown ingestion, Bedrock embedding adapter,
hybrid retrieval, retrieval persistence, API, or operator presentation.

This task follows the ordering in `docs/agent/ROADMAP.md` and the retrieval
design in `docs/agent/decisions/ADR-0002-hybrid-knowledge-retrieval.md`. It is
the active implementation contract. The owner approved it on 2026-08-28, so
the behavioral contract, scope, constraints, test plan, acceptance criteria,
and decisions below are locked under the repository task-update policy.

## Strict scope boundary

- This task adds approved-knowledge ingestion, retrieval, provenance, and
  presentation only.
- Observed MCP evidence remains unchanged and visibly separate.
- The retrieved chunks are source material, not AI inference, a probable cause,
  or a recommendation.
- The incident remains `INVESTIGATING`; this task does not add
  `AWAITING_REVIEW`, `APPROVED`, or `REJECTED`.
- No chat-model call, report schema, report generation, approval, rejection,
  remediation, or general audit-timeline UI is authorized.
- Existing queue, incident-detail, investigation, and evidence behavior are
  regression contracts only and must not be redesigned.

## Chosen contract

### Approved knowledge corpus

- Add one synthetic Markdown runbook for authorization decline-rate spikes and
  one synthetic Markdown operations policy relevant to investigation and
  response boundaries.
- Each document has a stable opaque document identifier, tenant ID, document
  type (`RUNBOOK` or `POLICY`), title, version, incident-family metadata,
  approval status, approver identifier, approval timestamp, and effective
  timestamp.
- All content is synthetic and repository-owned. It contains no real payment,
  customer, merchant, credential, endpoint, or organization data.
- Only approved, effective document versions are eligible for retrieval.
  Superseded or unapproved versions remain auditable but are excluded before
  ranking.
- Ingestion is an explicit application command. Normal API startup and ordinary
  `GET` routes never call Bedrock or mutate the knowledge index.
- Re-importing identical document content and metadata is idempotent. Changed
  content requires a new version and never overwrites the earlier version.

### Embedding and chunking

| Decision | Starting point |
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

- Headings establish section boundaries and remain attached to their chunks.
  Paragraphs, lists, tables, and fenced code blocks are not split unless the
  hard maximum requires it.
- No overlap crosses a heading/section boundary.
- The importer and its tests use the same deterministic token estimator.
- Maintain two immutable text representations for every chunk:
  - `raw_content` is the exact source text selected by the chunker, including
    any within-section overlap. It is the only chunk text shown to the operator
    and the only text quoted or excerpted by later citations.
  - `embedding_input` is a deterministic metadata-enriched representation used
    to create the vector. It is not source text and is never presented as a
    citation.
- Construct `embedding_input` with this exact field order and layout:

  ```text
  Document: Authorization Decline Runbook
  Section: Gateway Failures > Diagnosis
  Type: RUNBOOK
  Applies to: Card authorization

  [exact chunk content]
  ```

  The example values are replaced by the chunk's approved document title,
  section breadcrumb, document type, canonical applicability label, and exact
  `raw_content`. Labels, separators, and field order belong to a versioned
  embedding-input template.
- Generate the embedding from `embedding_input`, not `raw_content`. Build the
  PostgreSQL full-text document from separately weighted metadata fields and
  `raw_content`; do not index the rendered embedding wrapper as if it were
  source prose.
- Persist both text forms, their separate content hashes, the embedding-input
  template version, and source start/end line metadata. A change to either
  `raw_content` or an embedding-input metadata value requires a new chunk/index
  version and a new embedding.
- Persist the embedding model ID, dimensions, normalization setting, chunking
  strategy version, source document version, section path, chunk ordinal,
  content hashes, and embedded timestamp with each chunk.
- PostgreSQL stores embeddings as `vector(1024)`. Initial retrieval uses exact
  pgvector search; this task adds no HNSW or IVFFlat index.

### Retrieval trigger and HTTP API

- The investigation workspace exposes an explicit `Retrieve approved
  knowledge` action. Loading or refreshing a route never starts retrieval.
- `POST /api/investigations/{investigationId}/knowledge-retrievals` with the
  required `tenantId` query parameter creates one retrieval attempt. The body
  is absent; the application owns the query, filters, model, and ranking
  parameters.
- `GET /api/investigations/{investigationId}/knowledge-retrievals` with the
  required `tenantId` query parameter returns all attempts newest first.
- A deliberate retry creates a new attempt and never overwrites or hides an
  earlier attempt.
- Cross-tenant and nonexistent investigations return indistinguishable
  structured `404` responses without an embedding call or persisted attempt.
- Invalid identifiers return a structured `400` without an embedding call or
  persisted attempt.
- A successfully recorded terminal retrieval outcome returns `201 Created`,
  including no-match or explicitly degraded outcomes.

### Query construction and hybrid search

- The application derives a bounded query from the incident type, title, and
  description plus normalized observations from the most recent applicable
  `AVAILABLE` or `PARTIAL` service-error attempt.
- The query records which incident fields and evidence identifiers contributed.
  Missing or unavailable evidence remains explicit and is never fabricated.
- Apply tenant, approval/effective-version, document-type, and incident-family
  metadata filters before ranking.
- Run PostgreSQL full-text search and exact cosine vector search against the
  filtered corpus.
- Fuse the independently ranked lexical and vector result lists with Reciprocal
  Rank Fusion. Do not combine raw full-text and cosine scores directly.
- Retrieve at most 20 candidates from each modality after metadata filtering.
  Use Reciprocal Rank Fusion with `k = 60`.
- A lexical candidate is eligible when its PostgreSQL cover-density rank is
  greater than zero. A vector candidate is eligible when cosine similarity is
  at least `0.55` (cosine distance at most `0.45`). These are versioned initial
  relevance rules and must be preserved with the retrieval attempt.
- Break equal fused scores deterministically by best modality rank, document
  type, document identifier, document version, and chunk ordinal.
- Build a bounded final context targeting approximately four runbook chunks and
  three policy chunks. Return fewer when the approved corpus or relevance rules
  do not support the target; never add a weak or cross-tenant chunk merely to
  fill the quota.
- Preserve each result's full-text rank, vector distance/rank, fused rank, and
  final selection position for later report citation and audit.

### Retrieval status and persistence

- Retrieval attempts use exactly `STARTED`, `AVAILABLE`, `PARTIAL`, `NO_MATCH`,
  `UNAVAILABLE`, `TIMED_OUT`, and `MALFORMED`.
- When the query embedding is unavailable, times out, or is malformed, complete
  a lexical-only search. Record `PARTIAL` when it yields eligible chunks and
  preserve the embedding failure in a safe status detail. When it yields no
  eligible chunks, record the corresponding `UNAVAILABLE`, `TIMED_OUT`, or
  `MALFORMED` status rather than presenting a degraded empty result as a
  trustworthy no-match.
- Insert `STARTED` before the Bedrock call, perform network I/O outside a
  database transaction, and terminally update only the matching attempt.
- An interrupted `STARTED` attempt remains visible and does not prevent retry.
- Persist tenant, investigation, correlation and retrieval identifiers;
  request/completion timestamps; query/template version; bounded derived query;
  contributing evidence identifiers; embedding model and dimensions; metadata
  filters; ranking/fusion versions; outcome; safe status detail; and the exact
  selected document-version and chunk identifiers.
- The report-generation slice must be able to consume the persisted retrieval
  snapshot without silently re-running retrieval.
- Do not persist AWS credentials, arbitrary provider payloads, raw stack traces,
  unbounded content, or sensitive prompt/log data.

### Investigation workspace

- Add an `Approved knowledge` section after observed evidence and before any
  future AI inference or recommendation.
- Provide independent loading, not-retrieved, retrieving, matched, no-match,
  degraded, unavailable/timed-out, malformed, and API-error states.
- Each selected result shows document type, title, version, section path,
  bounded excerpt, approval/effective metadata, retrieval timestamp, and stable
  document/chunk provenance.
- Excerpts and later citations are derived only from persisted `raw_content`.
  The API does not expose `embedding_input` as approved source content.
- The UI states that retrieved content is approved synthetic source material,
  not an AI conclusion or executable instruction.
- While a `POST` is pending, disable the trigger. Retrying appends a new attempt
  without hiding earlier attempts.
- Preserve keyboard access and usability at 390 CSS pixels without horizontal
  page overflow.

### Local and CI behavior

- Use the Amazon Bedrock embedding adapter in normal runtime configuration and
  IAM/environment-based credentials without committed secrets.
- Automated tests use a deterministic embedding test double with the same
  1,024-dimension contract; CI must not require AWS credentials.
- Add an explicitly invoked Bedrock smoke-test path for an authorized local or
  AWS environment. It must verify model access, 1,024 dimensions, normalized
  output, and safe failure reporting without printing credentials or full
  document content.
- Docker Compose remains limited to PostgreSQL/pgvector.

## In scope

- One approved synthetic runbook and one approved synthetic policy.
- Versioned Markdown parsing, section-aware chunking, and idempotent explicit
  ingestion.
- Amazon Titan Text Embeddings V2 adapter and deterministic test double.
- Tenant-safe knowledge document/chunk and retrieval-attempt persistence through
  a new Flyway migration.
- PostgreSQL full-text search, exact pgvector cosine search, metadata filtering,
  Reciprocal Rank Fusion, and bounded runbook/policy context selection.
- Explicit tenant-scoped retrieval and history APIs.
- Approved-knowledge workspace states and provenance presentation.
- Focused, PostgreSQL, HTTP, Angular, regression, responsive, and authorized
  Bedrock smoke verification.
- Factual documentation updates after implementation and verification.

## Out of scope

- Chat model selection or structured report generation.
- AI inference, probable cause, confidence, recommendation, or report citations.
- Incident lifecycle changes or human decisions.
- Additional incident families or MCP tools.
- Knowledge-authoring, approval, or document-management UI.
- General-purpose file upload, crawler, object storage, or remote content source.
- HNSW, IVFFlat, managed search, reranking model, or learned fusion weights.
- Authentication, public ingestion endpoints, AWS infrastructure, or deployment.
- General audit-event table or audit-timeline UI.
- Refactoring the existing evidence model into a generic framework.

## Constraints

- Carry `tenant_id` through document, chunk, embedding, retrieval, result, and
  lookup boundaries.
- Apply authorization metadata filters before ranking, not after selecting
  candidates.
- Preserve missing, partial, unavailable, contradictory, and no-match outcomes.
- Retrieved knowledge is approved source material but is not automatically true
  for the current incident.
- Every displayed excerpt must reference a persisted document version and chunk.
- Every displayed excerpt and later citation must reproduce text from the
  persisted `raw_content`; metadata added only to `embedding_input` must never
  be represented as source text.
- Embedding and chunking metadata must be sufficient to reproduce or explain the
  index version used by a retrieval attempt.
- Use Flyway and preserve V1–V4 unchanged.
- Follow red-green-refactor one acceptance criterion at a time.
- Do not weaken, skip, or conditionally disable existing completion checks.

## Acceptance criteria

- [x] The explicit importer ingests the approved runbook and policy into
      tenant-scoped versioned documents and Markdown-aware chunks.
- [x] Chunking targets approximately 400 tokens, never exceeds approximately
      600 tokens under the chosen estimator, and overlaps 40–60 tokens only
      within one section.
- [x] Every chunk preserves exact `raw_content` for display and citation and a
      deterministic, versioned `embedding_input` using the required document,
      section, type, applicability, blank-line, and content layout.
- [x] Embeddings are generated from `embedding_input`, while operator excerpts
      and citations reproduce only `raw_content` and its source location.
- [x] Every stored embedding is a normalized 1,024-dimension vector associated
      with `amazon.titan-embed-text-v2:0` and reproducible chunk metadata.
- [x] Re-importing an unchanged source is idempotent; a new source version does
      not overwrite prior documents, chunks, or retrieval snapshots.
- [x] An owned investigation can explicitly create a retrieval attempt without
      client-supplied query or ranking parameters.
- [x] The API records `STARTED` before embedding and holds no database
      transaction across the Bedrock call.
- [x] Search applies tenant, approval/effective-version, document-type, and
      incident-family filters before full-text and exact vector ranking.
- [x] Reciprocal Rank Fusion produces a deterministic combined order without
      mixing raw lexical and vector scores.
- [x] The final bounded context targets four runbook and three policy chunks and
      returns fewer rather than fabricating or selecting ineligible content.
- [x] Every retrieval snapshot retains query, evidence-context, model,
      filtering, ranking, document-version, chunk, and timestamp provenance.
- [x] Cross-tenant, unapproved, superseded, and unrelated-incident-family chunks
      cannot be returned or exposed through the API.
- [x] No-match, degraded, timeout/unavailable, malformed, and interrupted
      attempts remain explicit and retry never hides prior history.
- [x] The workspace renders every retrieval state and source excerpt separately
      from observed evidence and absent AI inference/recommendation.
- [x] Existing alert, queue, detail, investigation, MCP evidence, and responsive
      behavior remain unchanged.
- [x] Focused and broader backend, PostgreSQL, frontend, formatting, build,
      Compose, diff, and responsive checks pass with no skipped tests.
- [x] The final diff contains no credentials, real payment data, unapproved
      content, generated output, unrelated refactoring, or edits to V1–V4.

## Test plan

### Markdown ingestion and embeddings

- `parsesApprovedRunbookAndPolicyMetadata`
- `chunksMarkdownBySectionWithinTargetAndHardMaximum`
- `overlapsOnlyWithinTheSameSection`
- `preservesRawContentExactlyForDisplayAndCitation`
- `buildsVersionedEmbeddingInputFromMetadataAndRawContent`
- `reembedsWhenEmbeddingInputMetadataChanges`
- `importsUnchangedDocumentIdempotently`
- `preservesPriorDocumentVersionWhenContentChanges`
- `rejectsInvalidUnapprovedOrOversizedSourceMetadata`
- `requestsNormalizedTitanV2EmbeddingsWith1024Dimensions`
- `rejectsMalformedOrWrongDimensionEmbeddingOutput`

### PostgreSQL and retrieval

- `persistsTenantScopedDocumentVersionsAndChunks`
- `preventsCrossTenantDocumentChunkAssociation`
- `filtersUnapprovedSupersededAndUnrelatedChunksBeforeRanking`
- `ranksFullTextAndExactCosineCandidatesIndependently`
- `fusesCandidateRanksDeterministicallyWithReciprocalRankFusion`
- `selectsBoundedRunbookAndPolicyContext`
- `persistsCompleteRetrievalSnapshotAndEveryRetry`
- `doesNotHoldTransactionAcrossEmbeddingCall`

Use PostgreSQL/pgvector through Testcontainers for isolation, vector distance,
full-text ranking, filtering, and persistence behavior. Mock-only tests are not
sufficient for those criteria.

### HTTP and workflow

- `retrievesKnowledgeForTenantOwnedInvestigation`
- `derivesQueryFromIncidentAndApplicableEvidence`
- `retrievesFromIncidentContextWhenEvidenceIsUnavailable`
- `returnsCreatedForRecordedNoMatchOrDegradedAttempt`
- `returnsRetrievalAttemptsNewestFirst`
- `rejectsMalformedRetrievalIdentifiers`
- `doesNotEmbedOrPersistForCrossTenantInvestigation`
- `doesNotChangeInvestigationOrIncidentState`

### Angular workspace

- `requestsKnowledgeHistoryForConfiguredTenant`
- `startsKnowledgeRetrievalWithoutClientQueryParameters`
- `rendersMatchedRunbookAndPolicyChunksWithProvenance`
- `distinguishesNoMatchDegradedUnavailableMalformedAndInterruptedRetrieval`
- `disablesRetrievalWhilePending`
- `retriesWithoutHidingPreviousAttempts`
- `keepsApprovedKnowledgeSeparateFromObservedEvidenceAndAiInference`

### External and manual verification

- Invoke the explicit importer against authorized Bedrock access and confirm
  Titan V2 returns normalized 1,024-dimension embeddings.
- Run the real PostgreSQL hybrid query for the primary synthetic incident and
  inspect the fused runbook/policy ordering and persisted provenance.
- Exercise retry, no-match, unavailable, timeout, malformed, unapproved,
  superseded, unrelated-family, and cross-tenant scenarios.
- Inspect desktop and 390-pixel workspace states for keyboard access, overflow,
  warnings, and errors.

## Expected approach

1. Approve this contract and accept or revise ADR-0002 before executable work.
2. Promote this proposal to `tasks/current.md` only after the existing active
   task is complete or explicitly superseded.
3. Lock the exact retrieval status enum, RRF parameters, candidate depths, and
   minimum relevance rules listed under Decisions needed.
4. Add parser/chunker red tests before the synthetic Markdown sources and
   ingestion implementation.
5. Add PostgreSQL red tests before the Flyway knowledge schema.
6. Add the narrow embedding adapter and deterministic test double before
   application orchestration.
7. Add hybrid-ranking and snapshot tests before retrieval services.
8. Add tenant-scoped HTTP tests before controllers and response records.
9. Add Angular API and workspace tests before the approved-knowledge panel.
10. Run focused suites, full repository verification, authorized Bedrock smoke
    verification, and live responsive checks.
11. Review the final diff for tenant leakage, unapproved content, missing
    provenance, fabricated results, secrets, generated output, and scope drift.

## Likely files or components

- `backend/copilot-api/pom.xml`
- `backend/copilot-api/src/main/resources/application.yml`
- `backend/copilot-api/src/main/resources/db/migration/V5__*.sql`
- `backend/copilot-api/src/main/resources/knowledge/`
- `backend/copilot-api/src/main/java/.../knowledge/`
- `backend/copilot-api/src/test/java/.../knowledge/`
- `frontend/operator-console/src/app/features/investigation-workspace/`
- `.env.example`
- `README.md`
- `docs/agent/ARCHITECTURE.md`
- `docs/agent/STATUS.md`
- `docs/agent/tasks/current.md`

Do not modify the operations MCP server, existing Flyway migrations, queue,
incident-detail presentation, application shell, AWS infrastructure, or
unrelated incident features without an approved task-contract change.

## Validation commands

Use the authoritative repository verification entry point once the current
development-system task establishes it. Until then, run the documented full
backend, frontend, formatting, build, Compose, and diff checks in
`docs/agent/QUALITY.md`, with zero skipped tests.

## Decisions needed

None. The initial implementation uses the locked statuses, lexical fallback,
RRF `k = 60`, 20 candidates per modality, positive lexical rank, `0.55` minimum
cosine similarity, deterministic tie-breaking, and the owner-supplied settings
captured in ADR-0002. Bedrock region and live model access are environment
configuration verified when an authorized environment is available; they do
not alter the application contract.

## Progress notes

- 2026-08-28: Repository review selected approved operational-knowledge
  retrieval as the next product slice after the verification-entry-point task.
- 2026-08-28: The owner supplied the starting embedding, chunking, hybrid-search,
  fusion, exact-indexing, and final-context decisions. This proposal and
  ADR-0002 were prepared for review; no executable work has begun.
- 2026-08-28: The owner approved the proposal and authorized implementation.
  The task moved to `In Progress`; the remaining ranking, status, and fallback
  starting values were resolved and the contract is now locked.
- 2026-08-28: Implemented explicit Markdown ingestion, Titan V2 embedding
  validation, Flyway V5 knowledge and retrieval persistence, filtered exact
  hybrid search with RRF, bounded context selection, retrieval history APIs,
  and the operator approved-knowledge panel.
- 2026-08-28: Hardened auditability so source fingerprints include citation
  line offsets; a line shift cannot silently retain stale stored citations.
- 2026-08-28: Completed automated, PostgreSQL, build, diff, and responsive UI
  verification. Live authorized Bedrock invocation remains pending because no
  authorized AWS credential or profile was available in this environment.

## Completion evidence

- Red-phase evidence: parser/chunker, importer, schema, hybrid ranking,
  selection, query derivation, persistence, API, Angular state rendering, and
  citation-fingerprint tests failed for their intended missing behavior before
  the corresponding production change. The final fingerprint regression first
  failed with identical hashes for body lines 14 and 15.
- Green-phase evidence: focused suites passed after each behavior; the final
  importer/fingerprint run passed 4/4 tests with zero skips.
- Acceptance-criteria coverage: named unit, HTTP, Testcontainers PostgreSQL,
  and Angular tests cover all checked criteria, including tenant/approval/
  effective/family exclusion, transaction boundaries, every terminal status,
  retry history, exact raw excerpts, and four-runbook/three-policy bounds.
- Full verification: `mvn clean verify` passed 97 copilot API tests and 9
  operations MCP tests with zero failures, errors, or skips against PostgreSQL
  17.11 and Flyway V1-V5. After `npm ci` reported zero vulnerabilities,
  Angular passed 46/46 tests; Prettier, the production build, Docker Compose
  validation, sensitive-pattern review, and `git diff --check` passed.
- Manual verification: a local synthetic API fixture rendered observed
  evidence followed by approved runbook and policy knowledge. Desktop and
  390-CSS-pixel inspection confirmed one knowledge panel, native link/button
  actions, readable raw excerpts and provenance, no page-level horizontal
  overflow, no off-viewport actions, and no browser warnings or errors.
- Documentation updated: root and console READMEs, `.env.example`, architecture,
  ADR-0002, roadmap, project status, documentation index, and this task.
- Remaining limitations: the explicit importer and embedding smoke command were
  not invoked against live Amazon Bedrock because authorized AWS access was not
  available. The deterministic embedding double, Spring AI adapter contract
  tests, and real PostgreSQL retrieval path passed; live Titan V2 authorization,
  region availability, and returned-vector behavior still require the
  documented one-shot smoke command in an authorized environment.
