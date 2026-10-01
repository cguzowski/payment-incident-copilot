# SynTen Inc

SynTen Inc is the fictional tenant `8b860d80-d17f-4e6b-8c48-af35f26a4d61`.
All data is synthetic. The historical authorization corpus covers
`AUTHORIZATION_DECLINE_RATE_SPIKE`. The owner-authorized
[multi-incidents v1](multi-incidents/v1/README.md) package adds six distinct
families with twelve scenarios and twelve approved Markdown sources.

## Corpus authorities

The [successor payment domain](domain/v2/README.md) defines lifecycle, independent
risks and evidence requirements for the next PDF library. The original profile
and corpus remain historical authoring authorities. E1 adds no PDFs or runtime tools.

E2's [independent payment library](payment-knowledge/v1/README.md) is frozen:
16 new PDFs / 48 pages with editable sources, risk coverage and review/hash evidence.
It remains separate from the historical authorization corpus. E3 adds explicit
preparation and PDF-only retrieval with declared families and shared policies;
historical Markdown citations remain readable.

| File | Owns |
|---|---|
| [profile.md](profile.md) | Fictional systems, vocabulary, roles, and authority |
| [corpus/inventory.md](corpus/inventory.md) | Exact document membership and metadata |
| [corpus/authoring-standard.md](corpus/authoring-standard.md) | PDF authoring and validation contract |
| [evaluation/retrieval-cases.md](evaluation/retrieval-cases.md) | Fixed retrieval labels and thresholds |
| [evaluation/report-evaluation-v1.md](evaluation/report-evaluation-v1.md) | Offline report-grading contract and fixture evidence |
| [corpus/validation-manifest.json](corpus/validation-manifest.json) | Source/PDF hashes and recorded validation |

The active `synten-auth-knowledge/v2` corpus contains 30 maintained Markdown
sources and 30 PDFs: 22 runbooks and 8 policies, comprising 27 approved and 3
superseded versions. Its manifest reports 113 pages, 3-5 per document; all pages
passed automated validation and visual inspection. The hard maximum is 15. The
inventory covers the original 36 authorization-decline observable scenarios. The exact v1 sources,
PDFs, contract, tooling, and manifest remain under
`corpus/versions/synten-auth-knowledge-v1/` and are hash-verifiable there.

Original page targets are advisory budgets; shorter complete documents are
allowed by the inventory. Repeated source procedures and unsupported
workspace-record instructions remain limitations, not permission to expand the
UI. See [project limitations](../docs/agent/STATUS.md).

## Recorded retrieval results

Historical K4/K5 records and Q2 corpus-v2 results:

| Measure | K4 passed | K5 passed | Q2 baseline passed | Q2 v4 passed | Applicable | Required to pass |
|---|---:|---:|---:|---:|---:|---:|
| Primary runbook selected | 9 | 19 | 19 | 22 | 22 | 22 |
| Supporting policy selected | 1 | 12 | 12 | 20 | 22 | 20 |
| Primary outranks weak match | 16 | 16 | 17 | 20 | 21 | 19 |

**Q2 v4 passes all fixed aggregate thresholds.** K4, K5 and the Q2 baseline
failed them. The retained v4 run has zero ineligible candidates and preserved
partial/unavailable/superseded semantics. Two policy cases and one ordering
case still fail individually; see [retained results and limitations](evaluation/q2-retrieval-results.md).
Retrieval success does not establish report quality.

## Recorded report-evaluation result

Q3 adds the offline `synten-report-eval/v1` contract. Its retained deterministic
fixture covers all 36 oracle scenarios and exercises every terminal result plus
the bounded unsupported-claim indicators. The fixture proves grader behavior,
not live-model quality, and sets no promotion threshold. See the
[contract and reproduction steps](evaluation/report-evaluation-v1.md).

Artifact: `evaluation/results/q3-report-grader-fixture-v1.json`, SHA-256
`a023fa81eac34732a619cd76850db936dac2f48358fa55afa9263a1846e93555`.

Q5 completed all 36 live corpus-v2 scenarios: 36 AVAILABLE reports, 374/374
valid citation identifiers, 34/36 exact dispositions and 9/36 exact confidence
matches. Partial S111 and unavailable S211 both failed the insufficient-evidence
contract, producing four bounded unsupported-claim indicators. Median latency
was 90.764 seconds, p95 109.187 seconds. Reports remain undecided.
[Live results, retained artifacts and reproduction](evaluation/q5-live-results.md)
record these quality gaps and the provenance/tooling limitations. No quality
promotion threshold was introduced.

Q6's fresh S111/S211 attempts both satisfy INSUFFICIENT_EVIDENCE/LOW with null
cause/recommendation and explicit evidence gaps under report-prompt/v5. The fixed
grader confirms both corrections; its retained comparison combines those two
fresh results with 34 unchanged Q5 results, rather than rerunning all scenarios.
[Q6 live proof and mixed diagnostic](evaluation/q6-live-results.md) preserve the
historical Q5 failures and the limits of this focused remediation.

Evidence records:

- [K4 completion](../docs/agent/tasks/completed/2026-09-01-embed-and-evaluate-the-synten-inc-pdf-knowledge-catalog.md):
  artifact `14588db4735841ffb5711a962e2c5119-FAIL.json`.
- [K5 completion](../docs/agent/tasks/completed/2026-09-01-prove-live-approved-knowledge-retrieval-in-the-operator-workflow.md):
  artifact `375ebc04ba894e84b2d18aeb6bc4d3cb-FAIL.json`.
- [Q2 completion](../docs/agent/tasks/completed/2026-09-30-resolve-retrieval-quality-disposition-q2.md): artifact
  `d22280e533b04578bdd4f260e0f6a6c6-FAIL.json`, SHA-256
  `c126a45551ebeaf8774e5731e4dbe6f8711d4e3d3d3e6bcdd2620ff75974cce1`.

The K4/K5 and originally recorded Q2 artifacts are absent from this checkout;
their recorded hashes remain historical evidence references. On 2026-09-29 the
owner reported a corpus-v2 replay using 705 existing `nomic-embed-text`
embeddings that reproduced 19/22 primary runbooks, 12/20 required policy cases,
17/21 primary-over-weak cases, zero ineligible candidates and preserved special
semantics. Its timestamp-dependent artifact was removed after extracting the
results. These aggregates do not independently reverify the original
per-variant traces or hash. Subsequent implementation retained a new baseline
`5847a80f655349ec8c3f9be986e52663` and passing run
`1f66fee3cf194f268287a314651ba13f` under `evaluation/results/`, each with its
own recorded SHA-256. See [diagnosis](evaluation/q2-baseline-diagnosis.md) and
[artifact integrity and reproduction](evaluation/q2-retrieval-results.md).

## Maintenance boundary

The v2 inventory, profile and authoring standard describe the existing baseline.
Their scenario-derived coverage is historical, not a rule for future authoring.
For the next version, define risks from the payment domain without consuming
scenario inputs, freeze the reviewed PDF corpus, then create incidents without
target document mappings. Preserve v2 source/PDF bytes, hashes and evaluations.

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
