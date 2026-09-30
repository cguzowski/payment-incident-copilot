# Q6 insufficient-evidence live regression proof

Run: `eb34c025-a08e-480b-8300-b848eedf5028`
Execution: 2026-09-30, 19:39:19–19:41:18 Europe/Warsaw
Final persisted readback: 19:42:07

## Fresh results

Two fresh investigations exercised the unchanged observable S111/S211 scenarios.
Both completed on the first model call and remain AWAITING_REVIEW with zero human
decisions. Historical Q5 failures remain unchanged in their original histories.

| Scenario | Evidence | Report | Disposition | Confidence | Cause / recommendation | Latency |
|---|---|---|---|---|---|---|
| S111 | PARTIAL | AVAILABLE | INSUFFICIENT_EVIDENCE | LOW | null / null | 64.080393 s |
| S211 | UNAVAILABLE | AVAILABLE | INSUFFICIENT_EVIDENCE | LOW | null / null | 44.985532 s |

S111 preserves TOKEN_VAULT_TIMEOUT with count 28 and describes missing independent
confirmation and broader partition context. S211 preserves no observations and
describes the unavailable source and missing operational observations. Neither
report includes inferences or contradictions. Each retains two evidence gaps.
All five report citation references resolve to the eligible evidence identifiers.

Exact live report identities:

- S111 investigation `7e3bb6c4-761e-4209-b340-1bc42d735b17`, report
  `dfe540eb-8e6b-43d8-b67e-f322a85e7ee6`.
- S211 investigation `65db9554-7c1a-4598-9730-a10abba487ef`, report
  `2c4cc380-5aa5-4219-9e34-f7b66f85e959`.

Tenant-scoped histories verify two fresh incidents/investigations, two evidence
attempts, two retrieval attempts, two report attempts, ten chronological audit
events and zero decisions. Readback verifies exact report content, latest/applicable
evidence, retrieval membership and content, operator attribution, and terminal state.

## Configuration and provenance

The isolated port-8083 API uses `payment_copilot_k4_eval_q2_v2`: 30 exact corpus-v2
PDF versions and 705/705 existing embeddings. Source/PDF hashes and approval states
match the manifest; all 60 repository source/PDF hashes were checked. Retrieval,
corpus, catalog, oracle, evaluator and model settings are unchanged.

Generation uses `qwen3:8b-q4_K_M`, temperature zero, disabled thinking, a two-minute
deadline, report-prompt/v5 and the context-constrained report-v1 schema. The exact
API jar SHA-256 is
`e317649c93a0d8ff2483753b0c29b18c23b3b75f13b2457d3eb6344f2d25c378`.
The observations retain model digests, per-attempt prompt/schema hashes and source
file hashes for the uncommitted implementation. The recorded Git commit is the
base revision, not a claim that Q6 code was committed.

The adapter's 1,536-token setting and recorded 4,096-token metadata discrepancy
remain unchanged. The generator and evaluation API were launched from temporary
jar copies so Windows file locks would not block clean-build verification.
Both temporary evaluation services were stopped after proof. The original Angular
dev server was restored at 127.0.0.1:4200 and returned HTTP 200. Restart the evaluation
API against the dedicated database to review the retained attempts.

## Fixed-grader diagnostic

The unchanged synten-report-eval/v1 requires all 36 scenarios. The retained input
therefore combines the two fresh Q6 attempts with 34 byte-equivalent result objects
from Q5 input `09bd6895a26d75b4535832a2c11dbdb6ba26d1855c4adf81aa67c77464c6a320`.
This is a mixed-run diagnostic, not a fresh 36-scenario run or evidence that v5
preserves live quality on the other 34 scenarios. Every result retains its actual
prompt version and attempt identity. Generation finished before oracle access.

| Bounded metric | Q5 baseline | Mixed diagnostic |
|---|---|---|
| Exact disposition | 34/36 | 36/36 |
| Exact confidence | 9/36 | 11/36 |
| Insufficient-evidence null contract | 0/2 | 2/2 |
| Required-signal coverage | 26/36 | 26/36 |
| Valid citation identifiers | 374/374 | 357/357 |
| Insufficient cause/recommendation indicators | 4 | 0 |

These checks do not establish general natural-language entailment. Confidence and
signal-coverage gaps in the 34 reused Q5 reports are unchanged. No promotion
threshold, automatic approval or full new live benchmark is introduced.

## Retained artifacts and reproduction

| Artifact | Bytes | SHA-256 |
|---|---:|---|
| [Fresh observations](results/q6-live-observations-v1.json) | 66223 | `7bcd0ab2f7ba9bb4db02aab8accf5ac138a8dad78f6c9c90ed041164f980caf8` |
| [Mixed grader input](results/q6-mixed-input-v1.json) | 122794 | `06534fbb99c25375fe82d9c57346425ecc98b595128c31c31bb499ac00bd66aa` |
| [Mixed grades](results/q6-mixed-grades-v1.json) | 149766 | `30dfb8aa354c5e5cb53e11bd9882131ad764081402993a2948300ae34bca09e9` |

Regrade under PowerShell 7.6.5 using the unchanged module and string timestamps:

```powershell
Import-Module ./scripts/evaluation/SynTenReportEvaluationV1.psm1 -Force
$inputRun = Get-Content 'SynTen Inc/evaluation/results/q6-mixed-input-v1.json' -Raw |
    ConvertFrom-Json -AsHashtable -DateKind String
$evaluation = Invoke-SynTenReportEvaluation -InputRun $inputRun `
    -ObservableCatalogPath syntheticIncidentGenerator/src/main/resources/scenarios/catalog.json `
    -OraclePath syntheticIncidentGenerator/src/main/resources/scenarios/oracle.json
Write-SynTenReportEvaluationArtifact -Path <new-result.json> -Evaluation $evaluation
```

A second evaluation matched all grade bytes and the hash above. All three Q5
artifact hashes remain unchanged. The known wrapper timestamp-coercion defect
is still a separate follow-up.

For fresh execution, follow the [Q5 product-HTTP reproduction](q5-live-results.md)
on the dedicated database using the Q6 jar, limiting the catalog loop to S111 and
S211. Retain fresh alert/evidence/retrieval/report responses, tenant-scoped histories,
metadata and timelines. Check all LOW/null/gap fields without retrying failures.
After generation, copy the 34 other Q5 result objects unchanged into a separately
named mixed input and apply the fixed grader. Fresh identities/times/model output
may differ; never overwrite prior proof.

All 48 focused report tests passed. ./verify.ps1 passed on 2026-09-30 with 317 API,
9 MCP, 31 generator and 91 Angular tests, zero failures/errors/skips, formatting,
builds, Compose validation and diff checks. Deterministic and full-gate evidence is recorded in the
[Q6 task](../../docs/agent/tasks/current.md).
