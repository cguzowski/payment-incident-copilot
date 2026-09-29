# SynTen Inc

SynTen Inc is the fictional tenant `8b860d80-d17f-4e6b-8c48-af35f26a4d61`.
All data is synthetic and belongs to the single
`AUTHORIZATION_DECLINE_RATE_SPIKE` incident family.

## Corpus authorities

| File | Owns |
|---|---|
| [profile.md](profile.md) | Fictional systems, vocabulary, roles, and authority |
| [corpus/inventory.md](corpus/inventory.md) | Exact document membership and metadata |
| [corpus/authoring-standard.md](corpus/authoring-standard.md) | PDF authoring and validation contract |
| [evaluation/retrieval-cases.md](evaluation/retrieval-cases.md) | Fixed retrieval labels and thresholds |
| [corpus/validation-manifest.json](corpus/validation-manifest.json) | Source/PDF hashes and recorded validation |

The active `synten-auth-knowledge/v2` corpus contains 30 maintained Markdown
sources and 30 PDFs: 22 runbooks and 8 policies, comprising 27 approved and 3
superseded versions. Its manifest reports 113 pages, 3-5 per document; all pages
passed automated validation and visual inspection. The hard maximum is 15. The
inventory covers all 36 observable generator scenarios. The exact v1 sources,
PDFs, contract, tooling, and manifest remain under
`corpus/versions/synten-auth-knowledge-v1/` and are hash-verifiable there.

Original page targets are advisory budgets; shorter complete documents are
allowed by the inventory. Repeated source procedures and unsupported
workspace-record instructions remain limitations, not permission to expand the
UI. See [project limitations](../docs/agent/STATUS.md).

## Recorded retrieval results

Historical K4/K5 task records and the fresh Q2 corpus-v2 baseline report:

| Measure | K4 passed | K5 passed | Q2 baseline passed | Applicable | Required to pass |
|---|---:|---:|---:|---:|---:|
| Primary runbook selected | 9 | 19 | 19 | 22 | 22 |
| Supporting policy selected | 1 | 12 | 12 | 22 | 20 |
| Primary outranks weak match | 16 | 16 | 17 | 21 | 19 |

**All three runs failed all three quality thresholds.** Records also report
zero ineligible candidates and preserved partial/unavailable/superseded
semantics. K5's S001 operator proof displayed RB-002 with PDF provenance; it
does not establish general retrieval quality.

Evidence records:

- [K4 completion](../docs/agent/tasks/completed/2026-09-01-embed-and-evaluate-the-synten-inc-pdf-knowledge-catalog.md):
  artifact `14588db4735841ffb5711a962e2c5119-FAIL.json`.
- [K5 completion](../docs/agent/tasks/completed/2026-09-01-prove-live-approved-knowledge-retrieval-in-the-operator-workflow.md):
  artifact `375ebc04ba894e84b2d18aeb6bc4d3cb-FAIL.json`.
- [Q2 active task](../docs/agent/tasks/current.md): artifact
  `d22280e533b04578bdd4f260e0f6a6c6-FAIL.json`, SHA-256
  `c126a45551ebeaf8774e5731e4dbe6f8711d4e3d3d3e6bcdd2620ff75974cce1`.

The K4/K5 artifacts are absent from this checkout; their recorded hashes remain
in completed tasks, so those counts are historical reports rather than
independently reverified results. The Q2 artifact is retained under
`evaluation/results/` and is current inspectable corpus-v2 evidence. It is new
evidence and must not be presented as either missing historical run.

## Maintenance boundary

Keep new tenant-specific assets here. The two legacy API Markdown knowledge
resources remain under `backend/copilot-api/src/main/resources/knowledge/`
for compatibility.

Do not casually edit source versions, PDFs, hashes, evaluation labels, or
chunking contracts. Corpus changes require explicit versioning, regeneration,
validation, and evaluation. Superseded documents are deliberate exclusion
fixtures; retain them for audit while excluding them from retrieval.

The v2 generator consumes only observable scenario evidence. Its signal tables
state neutral operational semantics and do not load or reproduce the sealed
oracle. The fixed `synten-retrieval-eval/v1` labels now bind to corpus v2; the
historical K4/K5 measurements remain v1 evidence and were not rerun.
