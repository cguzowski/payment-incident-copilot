# Task: Resolve retrieval-quality disposition (Q2)

Status: Active
Created: 2026-09-29
Owner: Christopher Guzowski

## Goal

Produce current, inspectable evidence for the unchanged SynTen retrieval
benchmark and either make the general retrieval path pass it or obtain explicit
owner acceptance of the measured failure and its consequences.

## User story

As a payment operations analyst, I want approved knowledge retrieval to select
the reviewed runbook and supporting policy reliably across the full synthetic
scenario catalog, so report generation receives relevant, source-verifiable
guidance rather than a result proven only for S001.

## Chosen contract

- Keep `synten-retrieval-eval/v1` labels, thresholds, 23 cases and 37 variants
  unchanged and evaluate the active `synten-auth-knowledge/v2` corpus.
- Begin with a fresh live `nomic-embed-text` baseline. Historical K4/K5 counts
  remain context only because their raw artifacts are absent from this checkout.
- Preserve corpus bytes and hashes, eligibility filters, tenant boundaries,
  approval/effective-version rules, superseded exclusions, exact source
  provenance, and the four-runbook/three-policy context allocation.
- Diagnose misses from exact derived queries, candidate ranks, fused ranks and
  selected chunks before changing retrieval behavior.
- Any retrieval change must be general, versioned, auditable and justified by
  measured diagnostics. Do not hard-code scenario IDs, labels, expected
  document IDs, error-code mappings or benchmark-only branches.
- Keep automated verification deterministic and independent of live Ollama.
- Q2 completes only when the fixed benchmark passes or the owner explicitly
  accepts a fresh measured failure and its documented product consequences.

## In scope

- Reproducing and retaining a fresh corpus-v2 evaluation result with its hash,
  environment, model, query, ranking and source-provenance metadata.
- Root-cause analysis of each missed primary runbook, supporting policy and
  primary-over-weak assertion.
- The smallest evidence-backed query, hybrid-ranking or context-selection
  change needed to address general failure modes.
- Test-first backend and PostgreSQL regressions for every changed retrieval
  behavior and its important failure paths.
- A post-change live benchmark, focused verification and the full repository
  verification gate.
- Factual status, roadmap, ADR and task updates where the resulting behavior or
  accepted disposition requires them.

## Out of scope

- Editing corpus sources, PDFs, validation hashes, evaluation labels,
  thresholds or expected sources to manufacture a pass.
- Hard-coded scenario/document routing, weakening source eligibility, changing
  tenant scope, or admitting superseded documents.
- Report generation or grading, human-decision flow, authentication, AWS or a
  second tenant or incident family.
- Pulling model weights automatically or making automated tests depend on a
  live model provider.

## Constraints

- Follow red-green-refactor for every executable behavior change.
- Preserve the observable/oracle separation in ADR-0014 and the immutable v1
  corpus archive.
- Keep model, query, ranking, retrieval and source metadata sufficient for
  audit; never log or publish vectors, secrets, sensitive data or unrestricted
  model payloads.
- Do not present a fresh run as either missing historical artifact.
- If the fresh benchmark still fails after justified general remediation, stop
  for explicit owner acceptance rather than weakening the contract.

## Acceptance criteria

- [x] A fresh live baseline executes all 23 cases and 37 variants against
      corpus v2, preserves the partial/unavailable/superseded semantics, and
      produces a complete inspectable artifact with a recorded SHA-256.
- [x] Every baseline miss is classified from persisted query, candidate,
      fusion and selection evidence; proposed changes cite the measured failure
      mode they address.
- [ ] Tests written before production changes fail for the intended retrieval
      behavior, then pass without scenario IDs, expected-source mappings or
      relaxed eligibility entering product code.
- [ ] The final live run has zero ineligible candidates, preserves KQ-020,
      KQ-022 and KQ-023 semantics, and either passes all fixed aggregate
      thresholds or records explicit owner acceptance of the exact failure and
      consequences.
- [ ] Query/ranking versions and relevant architecture documentation match the
      resulting behavior, with prior evidence and provenance retained.
- [ ] Focused backend/PostgreSQL tests and `./verify.ps1` pass with zero skipped
      tests; any unavailable live prerequisite is reported with its exact
      command and remaining risk.

## Test plan

1. Run the existing plan-only validator and fresh live baseline before changing
   retrieval behavior.
2. Convert each diagnosed general failure mode into the smallest meaningful
   unit or PostgreSQL regression and confirm the intended red result.
3. Implement one behavior at a time, then run the focused query, search,
   selector, persistence, grader and evaluation suites.
4. Re-run all 37 live variants through the unchanged evaluator and compare the
   exact aggregate and semantic results with the baseline.
5. Run `./verify.ps1 -Scope Backend`, `./verify.ps1 -Scope Repository`, and the
   authoritative unscoped `./verify.ps1` gate.

## Progress notes

- 2026-09-29: Owner activated Q2 by asking to proceed with the next ordered
  roadmap task after U3 completion.
- 2026-09-29: Archived U3 before replacing the active task. The working tree was
  clean at activation.
- 2026-09-29: The existing runner's plan-only validation resolved all 37
  variants and confirmed `synten-retrieval-eval/v1` against
  `synten-auth-knowledge/v2`.
- 2026-09-29: Created isolated database
  `payment_copilot_k4_eval_q2_v2`, applied Flyway V1-V9, imported all 30 corpus-
  v2 documents/705 chunks, and prepared 705 complete normalized
  `nomic-embed-text` embeddings.
- 2026-09-29: Fresh baseline run `d22280e533b04578bdd4f260e0f6a6c6`
  seeded and read-back verified all 37 variants. Its 2,300,817-byte factual FAIL
  artifact has SHA-256
  `c126a45551ebeaf8774e5731e4dbe6f8711d4e3d3d3e6bcdd2620ff75974cce1`.
- 2026-09-29: Baseline passed 19/22 primary-runbook cases, 12/20 required
  supporting-policy cases, and 17/21 primary-over-weak cases. It had zero
  ineligible candidates and preserved partial, unavailable, and superseded-
  source semantics.
- 2026-09-29: Exact diagnostics classify the three primary misses as RB-002
  absent for S002, RB-003 present at fused position 43 for S003, and RB-018
  absent for S201. The four outrank misses are KQ-001/S002, KQ-005/S006,
  KQ-017/S110, and KQ-018/S201.
- 2026-09-29: Required-policy misses consistently show generic PL-005, PL-001,
  and PL-002 consuming the three policy slots. Required PL-006 or PL-003 is
  absent or at fused positions 12-62 for KQ-001, KQ-002, KQ-005, KQ-006,
  KQ-009, KQ-010, KQ-013, KQ-014, KQ-018, and KQ-021 variants.
- 2026-09-29: Tested two general, non-label-aware remediations under fresh run
  IDs. Document-diverse modality depth changed aggregates to 17/22, 13/20, and
  16/21; compact boilerplate-free queries changed them to 18/22, 14/20, and
  15/21. Both regressed other thresholds and were fully reverted.
- 2026-09-29: Post-revert focused verification passed 7 query, executor, and
  PostgreSQL API tests with zero failures or skips. Repository verification
  passed the verification-system, knowledge-preparation, AI-prerequisite,
  seven evaluation-runner, Compose, and diff checks.

## Completion evidence

- Plan-only validation passed for all 37 variants.
- Fresh baseline artifact:
  `SynTen Inc/evaluation/results/d22280e533b04578bdd4f260e0f6a6c6-FAIL.json`
  (SHA-256
  `c126a45551ebeaf8774e5731e4dbe6f8711d4e3d3d3e6bcdd2620ff75974cce1`).
- Red evidence: the document-diversity PostgreSQL regression failed because
  repeated chunks from one document consumed both modality-depth slots.
- Green experiment evidence: the focused search/version suite passed 6 tests;
  the compact-query focused suite passed 7 tests, all with zero skips. Neither
  experiment met the full live-benchmark non-regression bar, so neither remains
  in production code.
- Post-revert command
  `mvn.cmd -pl backend/copilot-api '-Dtest=KnowledgeRetrievalQueryBuilderTest,KnowledgeRetrievalExecutorTest,KnowledgeRetrievalApiPostgresIntegrationTest' test`
  passed 7 tests with zero failures, errors, or skips.
- `.\verify.ps1 -Scope Repository` passed, including all seven evaluation-
  runner tests, Compose validation, and `git diff --check`.

## Remaining limitations

- Historical K4/K5 raw JSON artifacts remain absent. The new Q2 artifact is an
  inspectable corpus-v2 baseline, not a replacement for either historical run.
- Fixed thresholds remain unmet. The dominant policy failure reflects generic,
  near-duplicate policy text and a one-stage query that has no structured way
  to follow runbook-to-policy relationships.
- Passing without corpus or label changes now requires a consequential
  relationship-aware retrieval design and new persisted/audited metadata, not
  a safe local ranking-constant adjustment.

## Decisions needed

- Owner decision required: explicitly accept the measured Q2 failure and its
  report-quality consequences, or authorize a new relationship-aware retrieval
  contract with structured document-key/related-document metadata and an ADR.
