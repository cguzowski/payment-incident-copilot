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

The corpus contains 30 maintained Markdown sources and 30 PDFs: 22 runbooks and
8 policies, comprising 27 approved and 3 superseded versions. The K2 record
reports 112 pages, 3-4 per document, with page-by-page visual review; the hard
maximum is 15. K3 produced 705 page-aware chunks. The inventory covers all
36 generator scenarios.

Original page targets are advisory budgets; shorter complete documents are
allowed by the inventory. Repeated source procedures and unsupported
workspace-record instructions are known v1 limitations, not permission to
expand the UI. See [project limitations](../docs/agent/STATUS.md).

## Recorded retrieval results

Historical K4 and K5 task records report:

| Measure | K4 passed | K5 passed | Applicable | Required to pass |
|---|---:|---:|---:|---:|
| Primary runbook selected | 9 | 19 | 22 | 22 |
| Supporting policy selected | 1 | 12 | 22 | 20 |
| Primary outranks weak match | 16 | 16 | 21 | 19 |

**Both runs failed all three quality thresholds.** Records also report zero
ineligible candidates and preserved partial/unavailable/superseded semantics.
K5's S001 operator proof displayed RB-002 with PDF provenance; it does not
establish general retrieval quality.

Evidence records:

- [K4 completion](../docs/agent/tasks/completed/2026-09-01-embed-and-evaluate-the-synten-inc-pdf-knowledge-catalog.md):
  artifact `14588db4735841ffb5711a962e2c5119-FAIL.json`.
- [K5 completion](../docs/agent/tasks/completed/2026-09-01-prove-live-approved-knowledge-retrieval-in-the-operator-workflow.md):
  artifact `375ebc04ba894e84b2d18aeb6bc4d3cb-FAIL.json`.

These artifacts are absent from this checkout and untracked. Their recorded
hashes remain in the completed tasks; counts above are historical reports, not
independently reverified results. Restore exact files to `evaluation/results/`,
or identify a verifiable archive, before treating them as inspectable evidence.
A fresh run is new evidence and must have its own ID.

## Maintenance boundary

Keep new tenant-specific assets here. The two legacy API Markdown knowledge
resources remain under `backend/copilot-api/src/main/resources/knowledge/`
for compatibility.

Do not casually edit source versions, PDFs, hashes, evaluation labels, or
chunking contracts. Corpus changes require explicit versioning, regeneration,
validation, and evaluation. Superseded documents are deliberate exclusion
fixtures; retain them for audit while excluding them from retrieval.

The generator derives runbook text from scenario answer-key causes, so the
corpus is not independent of that oracle. Evaluation integrity work is ordered
in the [roadmap](../docs/agent/ROADMAP.md).
