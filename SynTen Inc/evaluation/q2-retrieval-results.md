# Q2 retained retrieval results

All runs use unchanged `synten-retrieval-eval/v1` labels and thresholds, corpus
`synten-auth-knowledge/v2`, and the same 37 persisted evidence contexts. Each
evaluation has a distinct run ID; none replaces an absent historical artifact.

| Run | Primary (22 required) | Policy (20 of 22 required) | Ordering (19 of 21 required) | Result |
|---|---|---|---|---|
| [Baseline](results/5847a80f655349ec8c3f9be986e52663-FAIL.json) | 19/22 | 12/22 | 17/21 | FAIL |
| [Exact signals only](results/e3c2d84e042b4ceca711ef912d1df6d9-FAIL.json) | 22/22 | 12/22 | 18/21 | FAIL |
| [Signals, lexical tie-break and relationships](results/1f66fee3cf194f268287a314651ba13f-PASS.json) | 22/22 | 20/22 | 20/21 | PASS |

Every run has zero ineligible candidates and preserves KQ-020 partial evidence,
KQ-022 unavailable evidence and KQ-023 superseded-source exclusions.

## Artifact integrity

- Baseline: `5847a80f655349ec8c3f9be986e52663-FAIL.json`; 2,300,817 bytes;
  SHA-256 `cd072e1b9407f5713813292f6c2a1bb0797c2bc80d94e5c8d53b9eff8830ad7a`.
- Exact signals only: `e3c2d84e042b4ceca711ef912d1df6d9-FAIL.json`; 2,299,774 bytes;
  SHA-256 `3649058cbb50245bdd7ed6aee00c81af7ec9bc413805dbc169fea355e5826d08`.
- Signals, lexical tie-break and relationships: `1f66fee3cf194f268287a314651ba13f-PASS.json`; 2,713,356 bytes;
  SHA-256 `7d904e6ec557acca8f79cb30a3eb09dc56c838c7e6739f5d6aad2a0509ae64bf`.

## Remaining case-level misses

The aggregate PASS is not universal source coverage. KQ-004/S005 and
KQ-019/S109 miss PL-002 after relationship selection; both policy assertions
passed in the baseline. KQ-018/S201 still ranks RB-008 ahead of RB-018, although
RB-018 is now selected. The passing thresholds allow these misses. Reports may
still omit the expected supporting guidance; human review remains mandatory.

## Reproduction and environment

- Baseline diagnostic: [all failed assertions and pre-limit ranks](q2-baseline-diagnosis.md).
- Dedicated local database: `payment_copilot_k4_eval_q2_v2`, PostgreSQL 18.3.
- Java 21.0.11; Ollama `nomic-embed-text`, 768 dimensions, normalized.
  Model digest: `0a109f422b47e3a30ba2b10eca18548e944e8a23073ee3f3e947efcf3c45e59f`.
- The explicit `pdf-backfill` command validated COMPLETE_SAME_MODEL and
  705/705 existing vectors with `noOp=true` before baseline evaluation.
- The explicit `pdf-catalog` command applied derived relationship metadata
  after Flyway V10: 0 new documents, 30 existing versions, 0 new chunks.
  Source/PDF bytes, chunk identities, embedding inputs and vectors were preserved.
- Start the generator MCP endpoint and API against the dedicated database,
  then run `scripts/evaluation/run-synten-retrieval-evaluation-v1.ps1 -PlanOnly`
  and the same runner with `-EvaluationDatabaseName payment_copilot_k4_eval_q2_v2`
  and an external temporary `-OutputPath`. The runner seeds and verifies all
  37 contexts through product HTTP APIs.
- Configure `SYNTEN_CORPUS_ROOT`, `SYNTEN_RETRIEVAL_CASES_PATH`,
  `SYNTEN_SCENARIO_CATALOG_PATH`, `SYNTEN_RETRIEVAL_EVALUATION_SEED_MANIFEST`,
  `SYNTEN_RETRIEVAL_EVALUATION_RESULTS_DIR` and the dedicated datasource via
  environment. Run the built API jar with `--spring.main.web-application-type=none`,
  `--spring.ai.model.chat=none` and `--app.knowledge.retrieval-evaluation.enabled=true`.
  Catalog import and embedding validation are separate explicit command modes.
- Candidate experiments reused the baseline seed mappings and evaluatedAt;
  only runId and createdAt were refreshed in external temporary seed manifests.
  Retain a new output filename/hash for every rerun. Fresh timestamps and IDs
  mean byte-identical hashes are not expected.

Ranking details and test strategy are in [ADR-0015](../../docs/agent/decisions/ADR-0015-source-derived-retrieval-signals-and-relationships.md).
Final deterministic verification is recorded in [the Q2 task](../../docs/agent/tasks/current.md).
