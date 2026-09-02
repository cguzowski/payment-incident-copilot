# Task: Embed and evaluate the SynTen Inc PDF knowledge catalog

Status: Complete — live backfill verified; fixed-threshold evaluation recorded a factual FAIL
Created: 2026-08-31
Owner: Christopher Guzowski

## Goal

Populate the stable K3 PDF chunks with live normalized `nomic-embed-text`
embeddings and evaluate the existing hybrid-retrieval path against all 23
human-labeled `synten-retrieval-eval/v1` cases without changing corpus content,
eligibility rules, or accepted ranking constants to manufacture a pass.

## User story

As a payment operations analyst, I want approved SynTen operational guidance to
be selected by both exact terminology and semantic similarity, so the later
report path receives relevant, source-verifiable PDF excerpts while
superseded, cross-tenant, or weak guidance remains excluded.

## Context

K3 catalogued all 30 manifest PDFs into 705 deterministic page-aware chunks.
Approved unembedded chunks are already lexically eligible, incomplete vectors
are ignored, and retrieval snapshots preserve exact PDF provenance. ADR-0007
selects normalized 768-dimensional Ollama `nomic-embed-text` embeddings;
ADR-0002 fixes the existing PostgreSQL hybrid search, RRF, relevance, and
selection rules. `SynTen Inc/evaluation/retrieval-cases.md` defines the fixed
K4 labels and thresholds. K5, not this task, evaluates live chat report output.

## Proposed contract

- Keep `synten-auth-knowledge/v1`, `pdfbox-text-pages/v1`,
  `pdf-page-sections/v1`, and all K3 document/chunk identities immutable.
- Use the existing Spring AI Ollama boundary with model ID
  `nomic-embed-text`, exactly 768 dimensions, and normalized vectors.
- Keep normal startup and automated verification model-free. Live embedding
  and evaluation remain explicit developer operations and never pull models.
- Validate the complete catalog and every model response before writing any
  embedding tuple. Write only the all-or-none embedding fields already defined
  by Flyway V9; never rewrite source text, hashes, locators, or chunk IDs.
- Repeating the same embedding operation against the same catalog/model is
  idempotent. Partial tuples or a conflicting model/dimension contract fail
  closed and remain auditable rather than being silently repaired.
- Preserve historical Markdown and Titan rows. Do not delete, re-embed, or
  vector-compare incompatible model/dimension pairs.
- Derive each evaluation query through the application query builder from the
  referenced synthetic scenario and persisted evidence. Do not hand-tune
  queries after seeing ranks.
- Persist and report the exact corpus, extraction, chunking, embedding,
  query-template, candidate-rank, selection, and source-location metadata
  required by `synten-retrieval-eval/v1`.
- Apply the fixed K4 thresholds exactly. A threshold miss produces a factual
  diagnostic record; it does not authorize relabeling sources or changing
  ranking constants inside this task.

## In scope

- Explicit, disabled-by-default PDF embedding backfill orchestration.
- Catalog/model validation, bounded embedding calls, stable idempotency, and
  conflict handling.
- Atomic persistence of complete embedding tuples for the 705 stable chunks.
- Network-free deterministic tests for success, malformed vectors,
  unavailability, timeout, partial-catalog, repeat, and conflict behavior.
- A live K4 evaluation runner over all 23 approved retrieval cases.
- Auditable evaluation output containing queries, candidates, ranks, selected
  chunks, provenance, thresholds, and observed failures.
- Documentation for external Ollama prerequisites and explicit execution.

## Out of scope

- Editing the SynTen corpus, PDFs, labels, expected sources, or K3 chunks.
- Changing lexical/vector candidate limits, cosine threshold, RRF constant,
  tie-breaks, or final runbook/policy allocation.
- Automatically pulling Ollama or model weights.
- Live report generation or report-quality grading; that later scope was
  subsequently deferred outside K5 by ADR-0010.
- Approximate vector indexes, a second tenant or incident family,
  authentication, Bedrock, or deployment.

## Constraints

- Follow ADR-0002, ADR-0007, ADR-0009, and
  `synten-retrieval-eval/v1` without weakening a fail-closed rule or threshold.
- Keep automated verification deterministic, network-free, and model-free.
- Never commit model weights, embedding caches, database files, credentials,
  prompts containing sensitive data, or whole PDFs as evaluation output.
- Carry tenant identity through every catalog, embedding, persistence, and
  retrieval boundary.
- Preserve all V1-V9 Flyway checksums and immutable historical retrieval/report
  references.
- Follow red-green-refactor for every production behavior change.

## Acceptance criteria

- [x] A focused test proves all 705 stable PDF chunks are validated before the
      first embedding persistence write and no chat-model call occurs.
- [x] Embedding response tests accept only model ID `nomic-embed-text`, 768
      finite dimensions, and a normalized vector; malformed, unavailable, and
      timed-out responses write no partial tuple.
- [x] PostgreSQL integration proves every PDF chunk receives one complete
      embedding tuple without changing its IDs, hashes, text, or PDF locator.
- [x] Repeat embedding is idempotent, while partial tuples, changed catalog
      content, or conflicting model metadata fail closed.
- [x] Historical Markdown/Titan rows remain unchanged and incompatible vectors
      never enter the live Ollama vector ranking.
- [x] The evaluation runner executes all 23 fixed cases through application-
      derived queries and records lexical, vector, fused, and selected ranks
      with exact document/chunk/page provenance.
- [x] Evaluation reports a 0% ineligible rate, 100% primary-runbook selection,
      at least 90% required-policy selection, at least 90% weak-match outrank,
      preserved KQ-020/KQ-022 semantics, and complete KQ-023 superseded
      exclusion—or records the exact unmet threshold without relabeling.
- [x] Normal startup and the authoritative automated gate remain independent of
      Ollama and pass with zero skipped tests.
- [x] Focused backend and PostgreSQL suites, Spotless, repository checks,
      `git diff --check`, and the authoritative `./verify.ps1` gate pass.

## Execution invariants

The implementation must preserve these invariants at every intermediate commit:

- No database transaction is held open while Ollama is called. The complete
  catalog is validated first, all required embeddings are prepared and
  validated in memory, and only then may a short persistence transaction begin.
- The expected catalog is exactly the K3 plan: 30 PDF document versions and 705
  stable chunks for the SynTen tenant. A missing, extra, duplicate, changed, or
  non-PDF row aborts before a model call or write.
- Embedding coverage is a closed state: either all 705 tuples are absent or all
  705 are complete for the accepted model contract. Mixed coverage, an
  incomplete tuple, or conflicting metadata is an error, not a repair request.
- The prepared write set carries the tenant, chunk ID, document-version ID,
  embedding-input hash, and catalog fingerprint it was built from. The
  transaction rechecks them under lock before its first update.
- One operation uses one UTC `embedded_at` value for all 705 rows. It never
  changes a source field, locator, document/chunk identity, approval state, or
  historical Markdown/Titan row.
- Model calls are sequential, bounded, retry-free inside one auditable
  operation, and ordered by document key/version/chunk ordinal. Progress logs
  contain identifiers and counts, never embedding input, vectors, or PDF text.
- Normal startup, tests, and the authoritative gate keep all import, backfill,
  evaluation, and smoke-test commands disabled and never require Ollama.
- Evaluation executes the production query builder, metadata filters, search
  SQL, RRF constants, tie-breaks, and context selector. It cannot accept a
  caller-supplied replacement query or ranking parameter.
- Live evaluation input comes from the reviewed generator scenario catalog and
  evidence persisted through the normal product/MCP workflow. Test-only SQL
  inserts are allowed in integration tests, not in the live runner.
- A live threshold miss creates a valid failing result with exact diagnostics
  and a nonzero runner outcome. It never mutates the corpus, labels, expected
  sources, model contract, or ranking constants.

## Fault-resistant implementation procedure

Implementation may begin only after the owner approves every item under
`Decisions needed`. Execute these stages in order; do not start a later stage
while the current stage's exit gate is red.

### Stage 0 — Lock the contract and preserve the baseline

1. Record the owner approval and change only the task status from planning to
   implementation.
2. Inspect `git status` and the complete diff. Preserve the existing K3
   repository-contract fix and all unrelated user-owned changes.
3. Run the current focused K3 catalog/search tests and `./verify.ps1`. Record
   the exact passing counts before changing production behavior.
4. Record the current 30-document/705-chunk corpus fingerprint, the V1-V9
   Flyway checksums, and the accepted constants from ADR-0002, ADR-0007, and
   ADR-0009. Treat any later drift as a stop condition.

Exit gate: owner approval is recorded, the baseline is green, and no material
decision remains implicit.

### Stage 1 — Create one reusable, pure K3 catalog plan

1. Red: add a planner test that asserts the real corpus produces exactly 30
   versions and 705 uniquely identified chunks in deterministic order, with the
   accepted tenant, hashes, parser/chunker versions, embedding-input hashes, and
   PDF locators.
2. Green/refactor: extract the existing in-memory planning portion of
   `SynTenPdfCatalogImportService` into one catalog-owned planner used by both
   K3 import and K4 backfill. Do not reimplement parsing or chunk identity in a
   second service.
3. Prove the existing import tests and real-corpus plan are unchanged across
   two executions.

Exit gate: import behavior is unchanged and K3 and K4 consume the same immutable
plan representation.

### Stage 2 — Define the backfill state machine before model calls

1. Red: define tests for `ABSENT`, `COMPLETE_SAME_MODEL`, `MIXED`,
   `INCOMPLETE`, and `CONFLICTING` catalog embedding states.
2. Add minimal immutable records for a catalog snapshot, target chunk, prepared
   embedding, and operation summary. Clone vector arrays at every record
   boundary.
3. Add a catalog repository read port that compares the persisted 705 PDF rows
   with the pure K3 plan. Validate tenant, document/chunk identity, ordinals,
   raw and embedding-input hashes, strategy versions, source format, PDF
   locators, and current embedding fields.
4. Green: allow only all-absent work or an all-complete exact no-op. Reject
   every other state before invoking the embedding client.

Exit gate: unit and PostgreSQL tests prove that invalid catalog/coverage states
produce zero model calls and zero writes.

### Stage 3 — Prepare and validate all embeddings without persistence

1. Red: add orchestration tests using 705 deterministic targets. Assert stable
   call order and that persistence is not invoked until response 705 passes.
2. Red: fail each representative position (first, middle, last) with unavailable,
   timed-out, wrong-model, wrong-dimension, non-normalized, non-finite, null, or
   incorrectly sized output. Assert zero persistence calls in every case.
3. Green: call only `KnowledgeEmbeddingClient` for each exact persisted
   `embedding_input`; validate model ID, 768 finite values, normalization, and
   defensive copies again at the orchestration boundary.
4. Enforce a configurable finite per-call timeout and whole-operation deadline,
   with no hidden retry. A timeout cancels the operation and discards the entire
   in-memory write set.
5. Capture one UTC embedding timestamp only after all 705 responses are valid.

Exit gate: the full prepared set exists in memory or nothing reaches the
persistence boundary. No chat-model type is a dependency of this path.

### Stage 4 — Apply the complete set in one short transaction

1. Red: add PostgreSQL tests that snapshot every non-embedding column before
   backfill and assert byte-for-byte equality afterward.
2. Red: force a failure on the final update and prove the transaction leaves all
   705 tuples absent. Also test a catalog change between preparation and commit.
3. Green: inside one transaction, lock and reread the exact 705 target rows,
   compare the plan fingerprint and every optimistic identity/hash field, then
   conditionally update the all-or-none embedding columns.
4. Require an update count of one per target. Any zero/multiple update count or
   revalidation mismatch rolls back the transaction.
5. Verify database constraints independently: 705 complete normalized
   `nomic-embed-text`/768 tuples, one shared timestamp, unchanged identities,
   hashes, text, and locators, and no change to Markdown/Titan rows.

Exit gate: the PostgreSQL test proves both atomic success and rollback after a
late failure.

### Stage 5 — Prove repeat, concurrency, and startup safety

1. Red/green: an exact same-model repeat returns a no-op summary with no model
   calls, writes, or timestamp changes.
2. Run two backfills from the same unembedded snapshot. Duplicate model work is
   acceptable; after locking, one transaction writes and the other must observe
   the exact completed state and make no changes. Neither may partially write.
3. Reject a concurrent content/hash change and any mixed or conflicting
   embedding coverage; never overwrite or auto-repair it.
4. Add one disabled-by-default command/property namespace for PDF backfill.
   Reject incompatible simultaneous command modes such as import plus backfill
   or evaluation plus backfill.
5. Add a normal-startup regression with all live commands disabled and both
   Spring AI providers set to `none`.

Exit gate: repeat/concurrency tests are green and ordinary startup makes no
model or backfill call.

### Stage 6 — Load and validate the fixed evaluation contract

1. Red: add a strict repository test for the approved
   `synten-retrieval-eval/v1` Markdown table, the generator's 36-scenario JSON
   catalog, and the PDF validation manifest.
2. Require exactly KQ-001 through KQ-023, exactly the referenced 36 `Sxxx`
   variants for KQ-001 through KQ-022, unique scenario coverage, resolvable
   primary/supporting/weak document keys, and the exact three superseded keys.
3. Validate that expected primary/supporting/weak current versions are eligible
   at the evaluation instant and that RB-022, PL-007, and PL-008 are
   `SUPERSEDED`. Fail before running a query if the sources disagree.
4. Keep `retrieval-cases.md` authoritative; do not create a second editable
   label source. Any machine representation is derived and validated at run
   time.

Exit gate: malformed, missing, duplicated, unknown, or drifted cases/scenarios
fail deterministically before live work.

### Stage 7 — Reuse one retrieval execution path

1. Add characterization tests around current query derivation, embedding
   fallback, metadata filters, candidate depth, RRF ordering/tie-breaks,
   runbook/policy allocation, status mapping, and selected-result persistence.
2. Red/green refactor the existing retrieval service just enough to expose an
   internal execution result containing the derived query, embedding outcome,
   all candidates, and selected chunks. The product retrieval service and K4
   evaluator must call this same executor.
3. Keep all ADR-0002 constants private to that shared path. The evaluation
   command receives no override for depth, threshold, RRF `k`, tie-breaks, or
   allocation.
4. Re-run every existing retrieval, report-snapshot, HTTP, and architecture
   test before adding evaluation behavior.

Exit gate: existing public behavior is unchanged and the evaluator cannot drift
from production ranking.

### Stage 8 — Seed live evaluation through real boundaries

1. Add a versioned PowerShell runner that preflights a dedicated synthetic
   evaluation database, the copilot API, the standalone generator MCP endpoint,
   and the configured tenant. It must not delete or rewrite an existing
   non-evaluation database.
2. For every referenced `Sxxx` variant, read the reviewed generator catalog,
   construct a valid opaque `sig-v1` alert reference, submit the sparse alert to
   the copilot API, start its investigation, and collect evidence through the
   generator's existing read-only MCP tool.
3. For KQ-023, submit one explicit synthetic legacy-terminology probe through
   alert intake and persist the honest no-matching-scenario evidence outcome;
   still derive its query through the application query builder.
4. After each seed, read back the persisted context and compare title,
   description, scenario reference, evidence availability, service name, and
   error counts with the generator catalog. Abort the evaluation on any
   mismatch.
5. Store the case/variant/investigation mapping only in a temporary run manifest
   outside version control; never seed evaluation by direct SQL.

Exit gate: all variants have tenant-scoped persisted incident/evidence context
that exactly matches their reviewed synthetic source.

### Stage 9 — Execute, grade, and atomically record evaluation

1. Red: with deterministic embeddings/candidates, test every grading rule,
   boundary count, missing rank, duplicate candidate, ineligible source, and
   KQ-020/KQ-022/KQ-023 special case.
2. Execute each scenario variant through the shared production query/search/
   selection path. A multi-scenario case passes an assertion only when every
   referenced variant passes it; this prevents an easy variant from hiding a
   failed one.
3. Resolve document IDs/versions to inventory keys from the validation manifest,
   never from model output or title similarity.
4. Build a compact versioned JSON result containing run/corpus/extraction/
   chunking/model/query/ranking versions, effective time, derived query,
   evidence references/status, filters, lexical/vector/fused ranks and scores,
   tie-break fields, selection position, exact PDF provenance, case assertions,
   aggregate counts, thresholds, and diagnostics.
5. Exclude vectors, embedding input, raw PDF/chunk text, credentials, database
   URLs, and stack traces. Sort cases, variants, candidates, and fields
   deterministically.
6. Validate the complete result in memory, write it to a temporary file, then
   atomically move it under `SynTen Inc/evaluation/results/`. Never leave a
   partial result or overwrite a prior run.
7. A pass exits zero. A threshold miss first preserves the valid `FAIL` result,
   then exits nonzero and points to the exact failed cases/ranks.

Exit gate: deterministic evaluation tests pass and both passing and failing
artifacts are complete, bounded, schema-valid, and reproducible from recorded
inputs.

### Stage 10 — Run the explicit live operation

1. Use a dedicated synthetic evaluation database. Apply and validate Flyway
   V1-V9, run the explicit K3 import, and verify 30 documents, 705 chunks, and
   all embedding tuples absent before K4.
2. Outside the repository, install/start Ollama and the pinned
   `nomic-embed-text` model. Verify the exact configured model through the
   existing embedding smoke test; never pull it automatically.
3. Run the PDF backfill once. Independently query the database for all Stage 4
   invariants, then run it again and prove the exact no-op behavior.
4. Start the copilot API against the generator MCP endpoint, run the live
   evaluation, validate the result artifact, and review every threshold miss
   without changing labels or ranking inside K4.
5. If the catalog or database changes between preflight and commit, discard the
   prepared vectors and restart from Stage 10. Do not cache or reuse them.

Exit gate: either the thresholds pass or a complete factual failure artifact
exists; database and corpus invariants remain intact in both outcomes.

### Stage 11 — Complete deterministic verification and documentation

1. Run the focused unit and PostgreSQL suites after each behavior, then the
   standalone generator gate, backend scope, repository scope, and unscoped
   authoritative gate with zero skips.
2. Run `git diff --check` and review the final diff for ranking/corpus/label
   drift, V1-V9 checksum changes, secrets, vectors, caches, database files,
   generated build output, and unrelated refactors.
3. Update README execution/preflight/failure instructions, then update only the
   unlocked status, progress, evidence, limitation, and acceptance-checkbox
   fields in this task and `STATUS.md`.
4. Mark an acceptance checkbox only after its named verification has actually
   run and passed. Record any unavailable live prerequisite and its remaining
   risk verbatim; do not substitute a mocked check.

Exit gate: deterministic gates are green, live evidence is recorded honestly,
and documentation matches the actual commands and outcomes.

## Acceptance-criteria traceability

| Acceptance criterion | Required automated evidence | Live/independent evidence |
|---|---|---|
| Validate all 705 before persistence; no chat call | Planner/orchestration order and failure-position tests | Backfill summary and zero chat invocation |
| Strict embedding contract and no partial tuple | Embedding client plus orchestration malformed/unavailable/timeout tests | Pinned-model smoke result |
| Atomic PostgreSQL tuples with stable source identity | Snapshot-before/after and forced-late-rollback integration tests | Pre/post invariant queries |
| Idempotency and fail-closed conflict handling | Repeat, mixed-state, changed-hash, and concurrent-run tests | Second live run is an exact no-op |
| Preserve Markdown/Titan behavior | Schema and hybrid-search model/dimension filtering tests | Historical-row checksum/count comparison |
| Execute all fixed cases with complete provenance | Contract-loader, shared-executor, artifact-schema, and PostgreSQL evaluation tests | 36 scenario variants plus KQ-023 result |
| Apply thresholds and special semantics exactly | Pure grader boundary/failure tests | Versioned PASS or factual FAIL artifact |
| Normal startup/gate independent of Ollama | Disabled-command context and model-none startup tests | Normal startup with Ollama stopped |
| Focused and authoritative verification | Named focused suites and zero-skip gate | Recorded commands, counts, and environment |

## Failure and resume matrix

| Failure point | Required state after failure | Resume rule |
|---|---|---|
| Contract/corpus/manifest drift | No model call, DB write, or result | Resolve the owner decision or restore the accepted version; rerun Stage 0 |
| Missing/extra/changed persisted catalog | No model call or write | Correct the explicit K3 import/new-version issue; rerun Stage 2 |
| Mixed/incomplete/conflicting tuple state | No model call or auto-repair | Audit the external change; restore a valid all-absent or all-complete state explicitly |
| Ollama unavailable, malformed, or timed out | All 705 DB tuples remain absent | Fix the external provider and restart the entire preparation stage |
| Catalog changes after embedding preparation | Transaction rolls back; prepared vectors are discarded | Re-plan and re-embed from the new accepted snapshot |
| Any update/constraint/connection failure | Transaction rolls back all 705 updates | Diagnose PostgreSQL, prove all-absent state, then rerun |
| Concurrent identical backfill wins first | One complete set; losing transaction makes no change | Report the second operation as already complete |
| Scenario/evidence mismatch | No evaluation result is published | Correct service/configuration drift and reseed a fresh run |
| Evaluation artifact write/validation failure | No partial final artifact | Fix output/schema issue and rerun evaluation; do not hand-edit output |
| Retrieval threshold miss | DB/corpus/ranking unchanged; complete `FAIL` artifact retained | Review diagnostics and open a separately approved follow-up decision |

## Likely files or components

- `backend/copilot-api/src/main/java/.../knowledge/catalog/`
- `backend/copilot-api/src/main/java/.../knowledge/retrieval/`
- `backend/copilot-api/src/test/java/.../knowledge/`
- `backend/copilot-api/src/main/resources/application.yml`
- `syntheticIncidentGenerator/src/main/resources/scenarios/catalog.json`
  (read as the existing input authority; no content change is expected)
- `scripts/run-synten-retrieval-evaluation.ps1`
- `SynTen Inc/evaluation/retrieval-cases.md`
- `SynTen Inc/evaluation/results/`
- `README.md`
- `docs/agent/STATUS.md`

Flyway V9 already contains the nullable all-or-none embedding columns and
constraints. Do not add a V10 migration unless a failing acceptance test proves
that V9 cannot support the atomic backfill contract; document and review that
new persistence decision before proceeding.

## Validation commands

```powershell
./mvnw.cmd -pl backend/copilot-api -Dtest=SynTenPdfEmbeddingPlanServiceTest,SynTenPdfEmbeddingServiceTest,SpringAiOllamaKnowledgeEmbeddingClientTest test
./mvnw.cmd -pl backend/copilot-api -Dtest=SynTenPdfEmbeddingPostgresIntegrationTest,KnowledgeHybridSearchPostgresIntegrationTest test
./mvnw.cmd -pl backend/copilot-api -Dtest=SynTenRetrievalEvaluationContractTest,SynTenRetrievalEvaluationTest,SynTenRetrievalEvaluationPostgresIntegrationTest test
./mvnw.cmd -f syntheticIncidentGenerator/pom.xml clean verify
./verify.ps1 -Scope Backend
./verify.ps1 -Scope Repository
./verify.ps1
git diff --check
```

The exact focused class names may be adjusted to existing naming conventions
when each red test is created; the acceptance behavior and recorded command may
not be weakened. Live Ollama/backfill/evaluation commands must be documented in
`README.md` when their final property and script interfaces exist, not guessed
in advance here.

## Decisions needed

None. On 2026-08-31 the owner authorized implementation of stages 0-2, thereby
accepting the six proposed K4 choices as the fixed implementation contract.
This session remains scoped to stages 0-2; later stages require a separate
implementation instruction but no contract reinterpretation.

## Progress notes

- 2026-08-31: K3 passed focused PostgreSQL 17.11 acceptance and the
  authoritative zero-skip gate. The completed K3 task was preserved before K4
  became the planning-stage current task.
- 2026-08-31: Expanded the proposed K4 red-green outline into a gated,
  fault-resistant implementation procedure. The plan now separates model work
  from the atomic database transaction, reuses the K3 planner and production
  retrieval path, traces every acceptance criterion, defines failure/resume
  behavior, and uses all reviewed generator scenarios through persisted
  evidence. No implementation began and owner contract approval remains
  required.
- 2026-08-31: The owner authorized implementation of stages 0-2 and accepted
  the proposed K4 choices as the fixed contract. Work is limited to baseline
  preservation, shared catalog planning, and pre-model persisted-state
  validation in this session.
- 2026-08-31: The first exact shared-planner test exposed that the unchanged K3
  parser/chunker produces 705 chunks, while earlier records reported 180 from a
  test that asserted only a lower bound. The owner authorized preserving K3
  behavior and correcting the K4 contract to the verified 705-chunk baseline.
- 2026-09-01: Stage 1 extracted the K3 import planning path into one reusable,
  immutable catalog plan. The real corpus produces the same 30 document
  versions and 705 stable chunks on repeat execution, pinned by catalog
  fingerprint `734461e767e08a59b83169fdf75d208d20c0366bebecd8825e2458c5f1b3d427`.
- 2026-09-01: Stage 2 added the closed `ABSENT`,
  `COMPLETE_SAME_MODEL`, `MIXED`, `INCOMPLETE`, and `CONFLICTING` state
  machine plus immutable snapshot, target, prepared-vector, and summary
  records. The PostgreSQL read boundary compares the persisted PDF catalog
  with the accepted K3 plan before later work and allows only all-absent work
  or an exact all-complete no-op.
- 2026-09-01: PostgreSQL tests prove exact absent and complete states, reject
  mixed coverage, conflicting model metadata, and changed catalog content,
  and leave embedding counts unchanged. No embedding client or persistence
  writer is reachable from the Stage 2 planning service.
- 2026-09-01: Stages 3-5 added a sequential, deadline-bounded preparation loop
  for all 705 normalized embeddings, followed by one catalog-revalidating
  PostgreSQL transaction. Invalid, unavailable, timed-out, mixed, drifted, and
  concurrent states make no partial change; exact complete reruns are no-ops.
- 2026-09-01: Stages 6-9 added the strict 23-case/37-variant evaluation
  contract, a shared production retrieval executor, the HTTP-only synthetic
  seeding runner, fail-closed grading, and atomic non-overwriting PASS/FAIL
  artifacts with exact ranks, filters, provenance, thresholds, and diagnostics.
- 2026-09-01: Stage 10 created a dedicated PostgreSQL 17.11 evaluation database
  on local container port 15432, applied Flyway V1-V9, and explicitly imported
  the immutable 30-document/705-chunk catalog. All 705 embedding tuples remain
  absent because Ollama is neither installed nor listening at port 11434.
- 2026-09-01: The pinned live `nomic-embed-text` smoke test passed with 768
  normalized dimensions. Backfill prepared all 705 responses before one atomic
  write; independent SQL found 705 exact complete tuples, zero incomplete or
  malformed vectors, and one shared UTC timestamp. The repeat command reported
  `COMPLETE_SAME_MODEL` with `noOp=true` and preserved that timestamp.
- 2026-09-01: Live HTTP seeding exposed PowerShell's non-enumerated
  `Invoke-RestMethod` JSON-array behavior. A red-green regression now flattens
  the history response before selecting exactly one persisted evidence ID; a
  fresh run then seeded and read-back-verified all 37 variants.
- 2026-09-01: The first evaluation publication exposed that two disjoint
  20-deep lexical/vector lists can validly fuse to 40 candidates. A red-green
  artifact-writer regression accepts that bounded union and rejects 41.
- 2026-09-01: The immutable live run published factual result
  `14588db4735841ffb5711a962e2c5119-FAIL.json`. Eligibility, structure,
  KQ-020 partial semantics, KQ-022 unavailable semantics, and KQ-023
  superseded exclusion passed. Fixed quality thresholds missed at 9/22
  primary-runbook cases, 1/20 required supporting-policy cases, and 16/21
  primary-over-weak cases against 19 required. No corpus, label, eligibility,
  query, or ranking constant changed.

## Completion evidence

- Stage 0 baseline before production changes:
  - focused K3 model-free catalog tests passed 20/20 with zero skips;
  - focused PostgreSQL 17.11 K3 tests passed 11/11 with zero skips;
  - `./verify.ps1` passed 210/210 copilot API, 9/9 MCP, and 78/78 Angular
    tests with zero skips, plus formatting, builds, npm audit, Compose,
    verification-system, and diff checks.
- Stage 0 immutable SHA-256 baseline, rechecked after Stage 2:
  - validation manifest:
    `2a5698a4ec6fbeeb7cb3f0b3211818dc5458aadaea55e0a413156c9c0af5beb6`;
  - ADR-0002:
    `820cc074e575e4d3bfcc14bbd0b07440f2b4c1266b310890953ec73d516078ca`;
  - ADR-0007:
    `58261ed39a9c2b52117ab2fae7f66ffd68b015f84412ec1f407943e99f89e809`;
  - ADR-0009:
    `7e332a8af84fb0ad6c684bba4b19ab55b225f2d1faa7618715bb95c073253d25`;
  - Flyway V1-V9, in order:
    `999c63e4d2b6f3777e014be54bb19cdfcf6bac6fc7feaac7dfbf014875934183`,
    `d0c4ca48d00f93154d512014765073fd7886bd082cb9ea508fc869ab6e56715b`,
    `e7594bad69cdecc2a9ba37f7916835e7a308f7f788016fa3d233ae6d5c0b0f5e`,
    `b877553d0a0cf62e102baa989e7a11a3ad21bfb7bf071ae43059c580eea0bc1d`,
    `f7e793ff971b8471d5a67bad2f8f7315a619b53df01b49398d9de684841dd1a6`,
    `50df545020ef20dc35c1c7ad106e3f9d950899e85a92bfb8cf74e6dac5e7be0a`,
    `498100bce06dbd99dc3d60b38c09f6e0b4aa96fcfe0d03efcf56c24e93f1062e`,
    `022b3470de73e355945f49c89f2fd20dc89f09e6c11e765189507a86945e1df9`,
    and
    `22d4dfe5f6f493254d4d4bb82d99bf63eb7ddcb1315d408d6192a2b74ea09bd7`.
- The combined Stage 1-2 focused suite passed 19/19 tests with zero skips,
  including the real-corpus planner, import regression, all state-machine
  states, five PostgreSQL catalog/state scenarios, and hybrid-search
  compatibility.
- `./verify.ps1 -Scope Backend` passed 221/221 copilot API and 9/9 MCP tests
  with zero skips, Flyway V1-V9 on PostgreSQL 17.11, architecture checks,
  packaging, and Spotless.
- `./verify.ps1 -Scope Repository` passed all verification-system tests,
  Compose validation, and `git diff --check`.
- The final authoritative `./verify.ps1` passed 221/221 copilot API, 9/9 MCP,
  and 78/78 Angular tests with zero skips, plus Spotless, Prettier, production
  builds, zero-vulnerability npm audit, Compose, repository, and diff checks.
- The complete named K4 focused suite passed 73/73 tests with zero skips,
  covering the real catalog plan, response validation, all-or-none persistence,
  idempotency/concurrency, hybrid retrieval, all 37 evaluation variants,
  grading, safe artifact publication, and PostgreSQL 17.11 integration.
- The standalone synthetic generator gate passed 16/16 tests with zero skips,
  and the PowerShell evaluation-runner suite passed 6/6 tests.
- `./verify.ps1 -Scope Backend` passed 282/282 copilot API and 9/9 MCP tests
  with zero skips, Flyway V1-V9 on PostgreSQL 17.11, architecture checks,
  packaging, and Spotless.
- `./verify.ps1 -Scope Repository` passed all verification-system and six
  evaluation-runner tests, Compose validation, and `git diff --check`.
- The final authoritative `./verify.ps1` passed 282/282 copilot API, 9/9 MCP,
  and 78/78 Angular tests with zero skips. It also passed the network-free
  evaluation-runner tests, Spotless, Prettier, both production builds, a
  zero-vulnerability npm audit, Compose validation, and the diff check.
- The dedicated live-database preflight validated 9 successful Flyway
  migrations at V9, 30 PDF documents, 705 PDF chunks, 705 wholly absent
  embedding tuples, and zero non-absent tuples before any model operation.
- The live model-contract smoke test passed for `nomic-embed-text`, 768
  dimensions, and normalized output. The first backfill completed 705 targets
  from `ABSENT`; the second completed from `COMPLETE_SAME_MODEL` as an exact
  no-op. Independent SQL verified 705 exact complete tuples, zero incomplete,
  wrong-dimension, or non-normalized vectors, and one unchanged timestamp.
- The evaluation-runner regression suite passed 7/7 after reproducing the live
  evidence-history array defect. The artifact-writer suite passed 5/5 after
  reproducing the valid 40-candidate fused-union defect and covering the
  rejected 41-candidate boundary.
- The live seed runner persisted and read-back-verified all 37 fixed variants
  through the isolated API and generator MCP endpoint. The evaluator published
  the 1,183,607-byte factual FAIL artifact with SHA-256
  `b9acc9bd4493e7c91405dc104b4c6629f59fd05a2baf9aebba502bbc779753bc`;
  it contains 37 variant records, 23 case grades, no forbidden fields, and no
  leftover temporary file.
- After the two live-only regression fixes, `./verify.ps1 -Scope Backend`
  passed 284/284 copilot API and 9/9 MCP tests; `./verify.ps1 -Scope Repository`
  passed all repository and seven evaluation-runner tests; the authoritative
  `./verify.ps1` passed those backend suites plus 78/78 Angular tests, Spotless,
  Prettier, both production builds, zero-vulnerability npm audit, Compose,
  repository, and diff checks.

## Remaining limitations

- The fixed live retrieval thresholds failed materially: only 9/22 primary
  runbook cases and 1/20 required supporting-policy cases passed, while 16/21
  primary-over-weak cases passed against 19 required. The factual artifact is
  the authority for exact queries, ranks, selections, and diagnostics.
- K4 does not authorize tuning the corpus, labels, eligibility, or ranking to
  address those misses. Any remediation requires a separately approved task
  that preserves this FAIL artifact and its database state.
- The owner subsequently scoped K5 to live `nomic-embed-text` retrieval and an
  operator-visible cited result. Live chat/report-model selection is deferred;
  K4 did not exercise report generation.
