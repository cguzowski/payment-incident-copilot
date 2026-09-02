# Task: Prove live approved-knowledge retrieval in the operator workflow

Status: Complete with factual evaluation FAIL
Created: 2026-09-01
Owner: Christopher Guzowski

## Goal

Improve the approved-knowledge retrieval path from the retained K4 baseline and
prove that a repeatable synthetic investigation using live
`nomic-embed-text` returns and displays eligible cited SynTen PDF guidance when
the operator clicks **Retrieve approved knowledge**.

## User story

As a payment operations analyst, I want the approved-knowledge action to show
relevant runbook and policy excerpts with exact PDF provenance, so I can review
useful operational guidance instead of receiving an unexplained no-match for a
supported synthetic incident.

## Chosen contract

- Preserve the immutable K4 FAIL artifact, `synten-retrieval-eval/v1` labels,
  corpus content, 30 document versions, 705 chunk identities, embedding tuples,
  tenant filters, approval/effective-version rules, and superseded exclusions.
- Keep live K5 model use embedding-only: Ollama `nomic-embed-text`, normalized
  768 dimensions. Keep `spring.ai.model.chat=none`; do not select, pull, or call
  a chat model.
- Introduce `knowledge-query/v2`. When applicable evidence contains a service
  or error observations, build the query from the scenario-specific description,
  evidence status, service, and ordered error codes/counts; omit the repeated
  incident-family and generic alert-title boilerplate. When evidence is absent
  or unavailable, retain the normalized title/description and explicit status
  without inventing observations.
- Introduce `postgres-hybrid-rrf/v2`. Keep candidate depth 20, RRF `k=60`,
  cosine threshold `0.55`, exact pgvector search, and existing tie-break fields,
  but apply candidate depth independently to RUNBOOK and POLICY within each
  lexical and vector modality. Rank positions are type-local, matching the
  existing four-runbook/three-policy allocation. The bounded fused union is at
  most 80 chunks.
- Select context in two stable passes per document type: first take the highest-
  ranked chunk from distinct document versions, then fill any remaining type
  slots from the still-ranked candidate stream. Never include an ineligible or
  weaker type merely to fill a quota.
- Use S001 as the primary live operator proof because its reviewed labels are
  RB-002 and PL-006 and its evidence contains both `GATEWAY_TIMEOUT` and
  `UPSTREAM_CONNECTION_RESET`. Completion requires an AVAILABLE retrieval with
  at least RB-002 displayed as a cited PDF result; PL-006 remains part of the
  fixed evaluation rather than being hard-coded into product behavior.
- Re-run all 23 cases/37 variants through the unchanged evaluator. K5 must keep
  zero ineligible candidates and all KQ-020/KQ-022/KQ-023 semantics, must not
  reduce any K4 aggregate, and must retain a complete factual result even if the
  fixed quality thresholds still fail.

## In scope

- Query-template v2 and its persisted/audited version metadata.
- Type-balanced lexical/vector candidate generation and bounded artifact rules.
- Document-diverse context selection.
- Deterministic unit and PostgreSQL regression tests, including S001-shaped
  retrieval data and unavailable/partial evidence behavior.
- A fresh live evaluation artifact comparable with the retained K4 baseline.
- Live API and operator-console verification against the dedicated K4/K5
  synthetic database, using the real button and visible PDF citation.
- README, ADR, roadmap, status, and task evidence updates.

## Out of scope

- Editing corpus sources, PDFs, validation manifest, labels, expected document
  keys, or the retained K4 artifact.
- Hard-coding scenario IDs, error-code-to-document mappings, expected document
  IDs, or evaluation labels into product retrieval.
- Lowering eligibility, approval, effective-time, tenant, or superseded-source
  protections.
- Approximate indexes, a new database migration, re-embedding, another tenant
  or incident family, authentication, deployment, or AWS work.
- Live report generation, report grading, or any chat-model selection.

## Constraints

- Follow ADR-0002, ADR-0009, ADR-0010, and the repository product guardrails.
- Every executable behavior change follows red-green-refactor.
- Automated verification remains deterministic, network-free, and model-free.
- Persist exact query/ranking versions and candidate/selection provenance.
- Never log or publish vectors, embedding inputs, source text outside the
  existing operator result contract, credentials, database URLs, or stack traces.
- Preserve all V1-V9 Flyway checksums and independently deployable services.

## Acceptance criteria

- [x] Query-builder tests prove evidence-focused `knowledge-query/v2`, bounded
      normalized text, stable evidence IDs, and honest unavailable/no-evidence
      behavior without generic-boilerplate dominance.
- [x] PostgreSQL tests prove each modality contributes at most 20 RUNBOOK and
      20 POLICY candidates, preserves exact eligibility/model filtering, emits
      deterministic type-local positions, and bounds the fused union at 80.
- [x] Selector tests prove distinct document versions receive the first slots
      per type before repeat chunks, while four-runbook/three-policy limits and
      ranked fill behavior remain stable.
- [x] Retrieval/API persistence tests record `knowledge-query/v2` and
      `postgres-hybrid-rrf/v2`, return AVAILABLE for an S001-shaped deterministic
      case, and preserve partial/unavailable/no-match semantics.
- [x] The fixed live evaluation preserves zero ineligible candidates and all
      KQ-020/KQ-022/KQ-023 semantics, does not reduce the K4 aggregates
      9/22, 1/20, and 16/21, and retains exact comparative diagnostics.
- [x] In the live operator console, clicking **Retrieve approved knowledge** for
      the accepted S001 investigation displays an AVAILABLE attempt containing
      RB-002 source text, filename, PDF SHA-256, page, block range, and IDs; the
      no-match copy is not shown for that attempt.
- [x] Focused backend/PostgreSQL/frontend tests, Spotless, Prettier, production
      builds, repository checks, `git diff --check`, and `./verify.ps1` pass
      with zero skips and no Ollama dependency in automation.

## Test plan

1. Red/green `KnowledgeRetrievalQueryBuilderTest` for available evidence,
   unavailable evidence, no evidence, normalization, truncation, and version.
2. Red/green `KnowledgeContextSelectorTest` for repeated leading chunks,
   distinct-document first pass, stable fill, and type quotas.
3. Red/green PostgreSQL hybrid-search tests for per-type modality depth,
   type-local positions, 80-candidate bound, filters, fallback, and tie-breaks.
4. Update artifact-bound tests from a 40-candidate to an 80-candidate valid
   maximum and reject 81.
5. Run retrieval service/controller/persistence/evaluation focused suites, then
   backend and repository gates.
6. Run a fresh live evaluation against the retained database and compare every
   aggregate and special semantic with the K4 artifact.
7. Start the isolated live API and operator console, use the browser to click
   the real S001 action, and inspect the visible citation and absence of the
   no-match message.
8. Run the unscoped authoritative gate and final secret/generated-output audit.

## Progress notes

- 2026-09-01: The owner authorized K5 after K4 completed and selected
  `nomic-embed-text` as the only live K5 model. ADR-0010 fixes the operator-
  visible cited-retrieval outcome and defers live report generation.
- 2026-09-01: K4 diagnostics show generic RB-001 and PL-001 chunks monopolizing
  global candidate pools and repeated chunks consuming context slots. The K5
  contract addresses those measured causes without changing labels or corpus.
- 2026-09-01: Red-green coverage introduced the v2 evidence query, type-local
  candidate depth and ranks, an 80-candidate union bound, and distinct-document
  first-pass selection. The focused K5 suite passed 17/17 tests; the bounded
  full-evaluation artifact suite passed 6/6 tests.
- 2026-09-01: Fresh live run `375ebc04ba894e84b2d18aeb6bc4d3cb`
  evaluated all 23 cases/37 variants against the retained 30-document,
  705-chunk `nomic-embed-text` catalog. It preserved zero ineligible
  candidates and all KQ-020/KQ-022/KQ-023 semantics and improved the three
  aggregates from 9/22, 1/20, 16/21 to 19/22, 12/20, 16/21.
- 2026-09-01: In the real operator console, the existing **Retrieve approved
  knowledge** button produced AVAILABLE attempt
  `bcf58984-c8c1-464e-9fed-5f2515705f4a` for S001 and visibly rendered RB-002
  with its exact PDF filename, SHA-256, page 3, block range 34-50, and document,
  version, and chunk IDs. The no-match copy was absent.

## Completion evidence

- Live artifact:
  `SynTen Inc/evaluation/results/375ebc04ba894e84b2d18aeb6bc4d3cb-FAIL.json`
  (SHA-256
  `5a58f03654d9bdaba267ce01a62537536f8fdf7c0b8686967f3b926a2021503e`).
  The immutable K4 comparison artifact remains
  `14588db4735841ffb5711a962e2c5119-FAIL.json`.
- S001 selected RB-002 plus RB-001, RB-011, RB-003, PL-005, PL-001, and PL-002;
  RB-002 was the first rendered result. Its cited source was
  `rb-002-gateway-connectivity-and-timeouts-v2.0.0.pdf`, SHA-256
  `7ea219d9ed06f921feeaea683d8ee4003047755eee0c1eefd240d82dcd484149`,
  page 3, blocks 34-50.
- The focused PostgreSQL API regression passed 2/2 with zero skips after the
  v1 query assertion was updated to the locked v2 contract.
- Authoritative `./verify.ps1` passed 289/289 copilot API, 9/9 operations MCP,
  and 78/78 Angular tests with zero failures, errors, or skips. The seven-test
  evaluation runner, Spotless, Prettier, both production builds, locked npm
  install with zero vulnerabilities, Compose validation, repository checks,
  and `git diff --check` also passed.
- Active source and current documentation contain no `qwen3.5` model reference;
  K5 live retrieval used only `nomic-embed-text`, and chat remained disabled.

## Remaining limitations

- The fresh K5 artifact factually remains FAIL: primary runbook retrieval is
  19/22, supporting policy retrieval is 12/20, and primary-over-weak ranking is
  16/21 against a required 19. K5 completion does not reinterpret or lower
  those fixed thresholds.
- Passing the S001 operator proof does not imply that every possible
  investigation has eligible knowledge.
- Live report generation and chat-model selection remain deferred and were not
  exercised by K5.

## Decisions needed

None. The owner authorized K5 on 2026-09-01; ADR-0010 and this chosen contract
define the implementation boundary.
