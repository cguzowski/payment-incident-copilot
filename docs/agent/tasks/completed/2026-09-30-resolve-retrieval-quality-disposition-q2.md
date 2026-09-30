# Task: Resolve retrieval-quality disposition (Q2)

Status: Complete — fixed benchmark and full repository verification passed
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
- [x] Tests written before production changes fail for the intended retrieval
      behavior, then pass without scenario IDs, expected-source mappings or
      relaxed eligibility entering product code.
- [x] The final live run has zero ineligible candidates, preserves KQ-020,
      KQ-022 and KQ-023 semantics, and either passes all fixed aggregate
      thresholds or records explicit owner acceptance of the exact failure and
      consequences.
- [x] Query/ranking versions and relevant architecture documentation match the
      resulting behavior, with prior evidence and provenance retained.
- [x] Focused backend/PostgreSQL tests and `./verify.ps1` pass with zero skipped
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

- 2026-09-30: Completed the documented sequence. Backend scope and the full
  `./verify.ps1` gate passed with zero failures/errors/skips; the Repository
  scope also passed. Q2 is complete under the unchanged aggregate contract.
- 2026-09-30: Resumed final verification. The earlier Backend gate stopped on
  the evaluation generator's open JAR; that process has exited. A resumed run
  correctly failed the no-skips check because Docker was stopped. Started
  Docker and reran the complete Backend scope; no skipped run is counted as
  passing evidence.
- 2026-09-29: Owner authorized implementation of the documented proposal.
  [ADR-0015](../../decisions/ADR-0015-source-derived-retrieval-signals-and-relationships.md)
  records the bounded, source-derived relationship contract and ranking rules.
- 2026-09-29: Retained baseline `5847a80f655349ec8c3f9be986e52663`
  reproduced 19/22 primary, 12 policy cases (20 required), and 17/21 ordering.
  All 37 variants were seeded/read-back verified; the catalog compatibility
  check confirmed 705 same-model vectors and unchanged accepted fingerprint.
  [Diagnosis](../../../../SynTen%20Inc/evaluation/q2-baseline-diagnosis.md) records
  every failed variant and pre-limit lexical/vector ranks.
- 2026-09-29: Exact-token ranking alone reached 22/22 primary, 12 policy cases,
  and 18/21 ordering. Equal complete signal matches could still lose to weaker
  lexical matches with semantic agreement. Added a bounded lexical tie-break
  and source-derived policy expansion through at most two edges from four
  ranked runbooks. Both changes were preceded by failing regressions.
- 2026-09-29: V10 stores derived document keys/relationships and immutable
  selected-result ranking evidence. Reimport populated metadata for 30 existing
  source versions without adding chunks or rewriting corpus/embedding inputs.
  `knowledge-query/v2` remains unchanged; ranking is
  `postgres-hybrid-related/v4`. No scenario or expected-source routing was added.
- 2026-09-29: Combined live run `1f66fee3cf194f268287a314651ba13f`
  passed at 22/22 primary, 20/22 applicable policy cases (20 required), and
  20/21 ordering (19 required), with zero ineligible candidates and all special
  semantics preserved. Retained baseline, intermediate and passing artifacts
  with hashes are listed in [results](../../../../SynTen%20Inc/evaluation/q2-retrieval-results.md).
- 2026-09-29: Focused `mvn.cmd -pl backend/copilot-api '-Dtest=Knowledge*Test,SynTen*Test' test`
  passed 136 tests with zero failures/errors/skips. Final gates remain pending.

- 2026-09-29: Owner reported a replay using the existing 705
  `nomic-embed-text` embeddings: 19/22 primary runbooks, 12/20 required
  policies, 17/21 primary-over-weak cases, zero ineligible candidates, and
  preserved partial/unavailable/superseded semantics. The evaluator exited
  nonzero as expected. Owner also reported 32 focused retrieval tests passing
  with zero failures/errors/skips, seven evaluator-runner tests passing, and
  all 37 plan variants resolving against corpus v2.
- 2026-09-29: Planning review confirmed the originally recorded Q2 artifact
  is absent. Owner removed the timestamp-dependent replay artifact after
  extracting results; it must not be assigned the original run's hash.
  Reopened the first two checkboxes pending retained evidence and renewed
  per-variant diagnosis. Earlier run details and diagnoses below remain
  historical records, not independently reverified candidate traces.
- 2026-09-29: Owner requested documentation of the recommended sequence below.
  This records a proposal, not authorization to implement a new retrieval
  contract or acceptance of the benchmark failure.

### Recommended sequence (planning proposal)

1. Restore inspectable evidence: retain a new corpus-v2 baseline under its own
   run ID and SHA-256, with environment, model, query, ranking and provenance
   metadata. Preserve prior recorded hashes as history; do not recreate or
   relabel an absent artifact. Reuse existing embeddings only after verifying
   their model and source/index compatibility.
2. Rebuild a per-variant diagnosis from that artifact. Distinguish relevance
   threshold exclusion, modality-depth truncation, fusion ordering and final
   selection loss. Where bounded candidate traces cannot explain absence,
   inspect eligible pre-limit ranks and scores. Equal aggregate counts do not
   establish identical misses. Map each proposed change to measured evidence.
3. Address runbook ranking separately. Current lexical search ORs query terms;
   test the hypothesis that generic terms compete with distinguishing observed
   signals. Evaluate exact observed-code coverage derived from approved source
   content, without hand-written code-to-document mappings, scenario IDs or
   evaluator labels in product code. Do not assume relationships alone fix
   missing or poorly ranked primary runbooks, and do not repeat the reverted
   diversity/compact-query experiments without new evidence.
4. Propose relationship-aware policy retrieval for owner authorization and an
   ADR before implementation. Existing source metadata contains `documentKey`
   and `relatedDocuments`; the ingestion record does not expose relationships.
   The proposed design persists source-derived, versioned links, resolves
   eligible policies from ranked runbooks, and combines them with direct policy
   matches inside the existing three-policy allocation. Specify traversal
   bounds, target-version resolution, ranking/ties and fallback behavior in the
   proposed contract. Retain relationship origin, source version, ranking and
   selection reasons in immutable audit metadata. Incorrect runbook selection
   can propagate to policies; this remains an unproved design hypothesis.
5. After required authorization, verify changes independently and together.
   Map changed behaviors to named tests before production edits and record the
   intended red results. Cover ranking, relationship parsing/resolution,
   missing/ambiguous links, tenant/approval/effective-version/superseded
   exclusions, bounded traversal, provider failure/fallback and immutable
   snapshots using deterministic unit and PostgreSQL tests. Version changed
   query/ranking/metadata behavior and use Flyway for schema changes. Replay
   all 37 variants after each justified change and compare per-variant results
   as well as aggregates; finish with the focused suites, Backend and Repository
   scopes, and the full `./verify.ps1` gate.

The unchanged success boundary is 22/22 primary runbooks, 20 of 22 applicable
policy cases, at least 19/21 primary-over-weak cases, zero ineligible candidates,
and preserved KQ-020/KQ-022/KQ-023 semantics. Neither hypothesis guarantees a
pass. Preserve corpus bytes/hashes, labels, thresholds, eligibility and the
four-runbook/three-policy allocation. If justified remediation still fails,
seek explicit owner acceptance of the fresh measured failure and its product
consequences. This proposal does not amend the locked contract or Test plan.

### Earlier execution history

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

- 2026-09-30: `./verify.ps1 -Scope Backend` and authoritative `./verify.ps1`
  passed: 303 copilot API, 9 operations MCP, 31 generator and (full gate)
  91 Angular tests, zero failures/errors/skips. Formatting, all builds,
  repository/runner tests, Compose validation and diff checks passed.
  Earlier infrastructure failures were resolved; no skipped run counts as
  completion evidence. No frontend behavior changed, so new visual QA was
  not required.
- Static review confirmed locked task sections/acceptance wording, corpus
  sources/PDFs/manifest and evaluation labels are unchanged. Local Markdown
  targets resolve, artifact hashes match, and retained payloads contain none
  of the prohibited vector/input/connection fields. All 176 recorded
  relationship paths in the passing artifact resolve to approved manifest
  source hashes with retained anchors and valid traversal/candidate bounds.
- Current inspectable baseline: `5847a80f655349ec8c3f9be986e52663-FAIL.json`,
  SHA-256 `cd072e1b9407f5713813292f6c2a1bb0797c2bc80d94e5c8d53b9eff8830ad7a`.
- Passing live artifact: `1f66fee3cf194f268287a314651ba13f-PASS.json`,
  SHA-256 `7d904e6ec557acca8f79cb30a3eb09dc56c838c7e6739f5d6aad2a0509ae64bf`.
  Both are retained under `SynTen Inc/evaluation/results/`; all 37 variant
  queries, independent/fused ranks, selection and source locators are present.
- Test-first evidence: `preservesExactMachineSignalsBeforeDepthLimitingAndFusion`
  failed with `generic-12` instead of `specific`; the focused search suite then
  passed four tests. `retrievesRelatedPolicyThroughOneIntermediateRunbookBeyondDirectDepth`
  failed with `generic-2` instead of `target` before expansion was implemented.
- Catalog red evidence: `retainsSourceDerivedRelationshipsOnIdempotentImportWithoutChangingChunks`
  failed on the missing metadata column; `rejectsMalformedAndDuplicateRelationships`
  failed because malformed links were accepted. The initial parser test also
  exposed a sandbox fixture-copy restriction and was rerun outside the sandbox
  before accepting its behavioral red result.
- Audit/tie red evidence: `favorsStrongerLexicalEvidenceWhenExactSignalsTieDespiteSemanticOnlyAgreement`
  selected `weak`; `persistsCompleteRetrievalSnapshotAndEveryRetryNewestFirst`
  and `executesAll37VariantsThroughTheSharedExecutorAndResolvesManifestProvenance`
  returned null ranking evidence; `acceptsBoundedRelationshipExpansionWithTwentyAdditionalCandidates`
  rejected the expanded union. The combined focused suite then passed 30 tests.
- Failure-path coverage includes `rejectsIneligibleAnchorsIntermediatesAndTargetsWithoutCrossTenantResolution`,
  `doesNotTraverseAmbiguousMissingOrRecursiveLinks`, and
  `resolvesDirectLinksWithLexicalFallbackAndPreservesTheirSourceProvenance`.
  Existing executor tests cover embedding unavailability, timeout and malformed
  responses; existing PostgreSQL tests preserve tenant and source exclusions.
- 2026-09-29 documentation update: `.\verify.ps1 -Scope Repository` passed,
  including verification-system, knowledge-preparation, AI-prerequisite,
  seven evaluation-runner tests, Compose validation and `git diff --check`.
  This verifies the documentation change, not retrieval quality; no live
  benchmark or full application gate was rerun for this documentation-only work.
- Plan-only validation passed for all 37 variants.
- Originally recorded baseline artifact (currently absent from checkout):
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

- K4/K5 and originally recorded Q2 artifacts remain absent. Newly retained runs
  have distinct identities/hashes and do not replace them.
- Passing aggregate thresholds allow individual misses: KQ-004/S005 and
  KQ-019/S109 omit PL-002 (both policy assertions passed in the baseline), and
  KQ-018/S201 still ranks RB-008 ahead of RB-018. All primary runbooks are now
  selected. This is not universal source coverage or proof of report quality.
- Relationships can propagate an incorrect anchor. Human review remains
  mandatory; no report-generation or operational authority changed.
- Existing installations must explicitly reimport the unchanged corpus to
  populate relationship metadata after V10. Migration alone does not derive
  links, and documents without metadata retain direct retrieval.

## Decisions needed

- Resolved on 2026-09-29: owner authorized the documented relationship-aware
  retrieval implementation. The fixed live benchmark now passes; no acceptance
  of a failed aggregate is required. Completion still requires final gates.
