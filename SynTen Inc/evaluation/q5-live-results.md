# Q5 live-model coverage

Status: Complete — coverage and grading measured; quality gaps remain
Run: `1d9996ed-eeae-4295-a925-6d46709bb943`
Execution: 2026-09-30, 16:22:24–17:19:40 Europe/Warsaw
Final persisted readback: 17:20:36 Europe/Warsaw

## Measured result

All 36 scenarios produced AVAILABLE reports on their first live attempt. All
36 remain AWAITING_REVIEW with zero human decisions. Coverage is complete;
this is not a quality promotion or approval of the reports.

| Coverage | Cases | AVAILABLE | Exact disposition | Exact confidence | Required-signal coverage |
|---|---:|---:|---:|---:|---:|
| COMMON | 14 | 14 | 14 | 2 | 10 |
| UNCOMMON | 11 | 11 | 10 | 2 | 7 |
| RARE | 11 | 11 | 10 | 5 | 9 |
| Total | 36 | 36 | 34 | 9 | 26 |

The fixed grader reports required-signal coverage for 26/36 cases, including
both cases without required machine signals. Among the 34 signal-bearing
cases, 24 cover every required exact token. Missing exact tokens are recorded
for S001, S005, S006, S007, S105, S106, S107, S110, S203 and S207. A prose
paraphrase can fail this bounded token check; it is not semantic correctness.

| Dimension | Measurement |
|---|---|
| Terminal outcomes | 36 AVAILABLE; 0 UNAVAILABLE, TIMED_OUT or MALFORMED reports |
| Evidence outcomes | 34 AVAILABLE, 1 PARTIAL (S111), 1 UNAVAILABLE (S211) |
| Retrieval outcomes | 36 AVAILABLE; 7 approved v2 PDF chunks per investigation |
| Exact disposition | 34/36; S111 and S211 failed |
| Exact confidence | 9/36; reports express HIGH in 2 cases, MEDIUM in 34, LOW in none |
| Insufficient-evidence null contract | 0/2 applicable cases |
| Required claim citations | 241/241 claims |
| Citation identifier membership | 374/374 references valid; zero unknown evidence/knowledge IDs |
| Recommendation knowledge citations | 36/36 present |
| Bounded unsupported-claim indicators | 4 assertions across S111/S211; zero unknown IDs or foreign machine-signal tokens |
| Report latency | min 75.096 s; median 90.764 s; nearest-rank p95 109.187 s; max 113.636 s |

Evidence status and available error-code counts/service names matched the
observable catalog. Every fresh investigation has exactly one evidence attempt,
one retrieval attempt, one report attempt and five chronological audit events:
180 events total, with exact source bindings and synthetic operator attribution.
Persisted history readback matched evidence content, selected knowledge snapshots
and report content. No generation retry or human decision was submitted.

## Important failures and interpretation

S111 (PARTIAL), report `b67f40e3-a5f3-4be2-baa2-643c739c81eb`, asserted a potential
token-vault cause and recommended investigating dependencies at MEDIUM confidence.
It acknowledged missing observability and independent confirmation but returned
PROPOSED rather than the oracle's INSUFFICIENT_EVIDENCE/LOW contract.

S211 (UNAVAILABLE), report `5eb5f7cb-5bec-4497-a221-b6eeb6afd453`, acknowledged no
retrieved observations but still asserted a probable cause and recommendation
at MEDIUM confidence. It also returned PROPOSED rather than
INSUFFICIENT_EVIDENCE/LOW. Each case produced two bounded assertion indicators.

These schema-valid reports show that identifier-valid citations and explicit
gap text do not establish a justified conclusion. The grader cannot establish
general natural-language entailment; zero foreign tokens is not proof of zero
unsupported prose. There is no promotion threshold, semantic score or human
report-quality judgment in this run. Remediation needs a separately activated
task; Q5 does not tune the model, retrieval, corpus or grader.

## Execution and provenance

One fresh alert, investigation, evidence collection, knowledge retrieval and
live report attempt per observable catalog scenario used existing product HTTP
endpoints with synthetic tenant/operator headers. The separate API on port 8083
used `payment_copilot_k4_eval_q2_v2`; the ordinary port-8080 API and its Q4 report
are separate. The database retains Q5 attempts for later human review.
The temporary port-8083 API was stopped after proof; the existing API and
generator remained healthy. Restart the evaluation API to review these records.

Preflight verified exactly 30 corpus-v2 PDF versions (27 approved, 3 superseded),
matching every manifest document/version, source hash, PDF hash and approval
status, with 705/705 existing embeddings. All 60 maintained source/PDF files
also matched manifest hashes. Each selected retrieval source was APPROVED/PDF
and a member of that exact manifest; no legacy Markdown source was present.

Fixed configuration: `qwen3:8b-q4_K_M`, temperature zero, disabled thinking,
`report-prompt/v4`, `report-v1`, two-minute report timeout,
`knowledge-query/v2`, `postgres-hybrid-related/v4`, `nomic-embed-text` at 768
dimensions, `pdfbox-text-pages/v1` and `pdf-page-sections/v1`.
The observations retain exact model digests and the running jar SHA-256
`b5def6cf1015d473700b9c6ba457d658d412fe8c00408c67f3925235d0bbe53f`,
built from Git commit `4bd4f49dd4e0251ed7b7734836abfab1e12e25b3`.
Per-attempt prompt/schema hashes come from tenant-scoped persisted rows.

Generation consumed only the observable catalog and persisted evidence/knowledge.
The execution loop did not read the oracle. The sealed oracle was accessed only
after all generation and history verification finished, by offline grading.
No application, model, prompt, corpus or evaluator bytes were changed in Q5.

## Retained artifacts and reproduction

| Artifact | Bytes | SHA-256 |
|---|---:|---|
| [Operational observations](results/q5-live-observations-v1.json) | 967224 | `0d2db90e45f7fd542d4d2678b15ffa3571d1d593a519df507278c690566eb25f` |
| [Grader input](results/q5-live-input-v1.json) | 125129 | `09bd6895a26d75b4535832a2c11dbdb6ba26d1855c4adf81aa67c77464c6a320` |
| [Per-scenario grades](results/q5-live-grades-v1.json) | 152107 | `efb290272dfb073e701ef75e495fd91f446ffe188eef4e9b4c56e9196b7a14fb` |

Observations retain preflight, model digests, IDs, evidence, selected source text
and immutable PDF locators, terminal report readback, persisted metadata and
projected timelines. Input retains all 36 reports, eligible citation sets,
persisted timestamps and exact model/prompt/schema metadata. Grades retain
per-scenario checks and complete evaluator/input/catalog/oracle provenance.

Regrade from the repository root using PowerShell 7.6.5:

```powershell
Import-Module ./scripts/evaluation/SynTenReportEvaluationV1.psm1 -Force
$inputRun = Get-Content 'SynTen Inc/evaluation/results/q5-live-input-v1.json' -Raw |
    ConvertFrom-Json -DateKind String
$evaluation = Invoke-SynTenReportEvaluation -InputRun $inputRun `
    -ObservableCatalogPath syntheticIncidentGenerator/src/main/resources/scenarios/catalog.json `
    -OraclePath syntheticIncidentGenerator/src/main/resources/scenarios/oracle.json
Write-SynTenReportEvaluationArtifact -Path <new-result.json> -Evaluation $evaluation
```

A second evaluation produced a byte-identical grade artifact and SHA-256.
The evaluator's `inputSha256` is the hash of compact canonical object
serialization (`f9a5c5dbf3a7e9227c966a43e2d779e13cf4c5c82cda6fcbbf5afed9bf752614`),
which differs from the pretty-printed input file hash above.
Evaluator module SHA-256:
`ac0e80cae3b7ce9e5fcd8bc162b00f1c71d49a7780e83764b331f97c83e4a4ef`.

For a new live run, start the existing API jar on a separate port with its
`SPRING_DATASOURCE_URL` pointing at the dedicated evaluation database and
`OPERATIONS_MCP_BASE_URL=http://localhost:8082`. Confirm generator/Ollama health
and exact v2 catalog membership. For every catalog scenario, create an alert
using its observable severity/title/description and a fresh
`sig-v1-{code}-{detectedUnixSeconds}-{12hexToken}` reference. POST investigation,
evidence collection, knowledge retrieval and report generation in sequence,
retaining each response before advancing. GET histories and timeline to verify
identity, source binding, content and terminal state. Retain failures without
retrying for a more favorable result. Construct the documented Q3 input from
persisted metadata only after complete coverage, then use the offline command
above. Fresh timestamps/identifiers mean a new live run need not be byte-identical.

## Verification and remaining tooling limitations

The eight existing offline report-evaluator tests passed. Complete live HTTP and
persistence checks verified all 36 scenario/incident/investigation/attempt
identities, exact snapshot bindings, 180 ordered audit events, zero decisions,
source membership and retained model/configuration provenance. Regrading matched
all bytes. Repository scope and final diff/static checks are recorded in the
[Q5 task](../../docs/agent/tasks/completed/2026-09-30-broader-live-model-coverage-q5.md).

The compiled model adapter uses maxTokens=1536, but the compiled attempt factory
and persisted metadata record max_output_tokens=4096. `javap -c -p` confirmed
both constants in the exact running jar. Q5 preserves the stored value and does
not repair this existing provenance defect.

The ordinary wrapper command
`./scripts/evaluation/run-synten-report-evaluation-v1.ps1 -InputPath 'SynTen Inc/evaluation/results/q5-live-input-v1.json' -OutputPath <new-result.json>`
failed under PowerShell 7.6.5 with `createdAt must be non-blank`: its default
ConvertFrom-Json converts timestamp strings to DateTime objects. The unchanged
module succeeded when invoked with `ConvertFrom-Json -DateKind String` as above.
The existing runner test checks structure, so its passing result does not cover
this compatibility defect. No grader logic, contract or tests were weakened.

A POST request timestamp can contain nanoseconds while PostgreSQL stores
microseconds. S001's initial strict readback check stopped on that precision
loss. Resuming reused its completed attempt, verified identical report content,
and used persisted timestamps for grading. No additional model call was made.
