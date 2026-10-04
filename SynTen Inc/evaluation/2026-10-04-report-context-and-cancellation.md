# Report context and cancellation evaluation — 2026-10-04

The context-preservation and cancellation defects are fixed and verified.
Timely report completion is not established: all three fresh first attempts
still timed out. The owner explicitly retained the two-minute deadline and the
throughput limitation. O3 remains unfinished with its original findings intact.

## Boundary and provenance

The isolated API at port 8087 uses a new database,
`payment_copilot_report_reliability_20261004`, with the same 1,562 knowledge
chunks and initially zero incidents. Its independent generator is at port 8088.
New synthetic incidents use the observable catalogs; generation never receives
oracle content. No retries, decisions, reveals or comparisons were performed.
The original O3 evaluation API/database are unchanged.

- Model: `qwen3:8b-q4_K_M`; Ollama 0.35.1.
- Prompt: `report-prompt/v8`, SHA-256
  `98d9ce4d3ea5ac8b3c33005adb885605f46fad55e8e7095c531f96b3b8ae1094`.
- Schema: context-constrained `report-v1`; each exact hash was reconstructed
  from persisted evidence/retrieval snapshots and verified against the database.
- Retrieval: `postgres-pdf-family-related/v5`; ranking and corpus unchanged.
- `report-model-settings/v2`: 8,192 context tokens, stream=true,
  truncate=false and shift=false. Temperature zero, output maximum 1,536,
  thinking disabled, no tools; the two-minute deadline is unchanged.
- API artifact SHA-256:
  `ed4cb4d892203ab4569a95f6d3da05ea1eaf71018d628dd9d4d5b3d8700d3464`.
- Generator artifact SHA-256:
  `68c306fd5534ba17e4aee45df21cd423c13bf2648abd5af033386f8e76dd2d7e`.

## First-attempt results

Times use persisted requested/completed timestamps; HTTP round trips were
120.14, 120.05 and 120.03 seconds respectively.

| Case | Attempt | Outcome | Seconds | Prompt characters | Provider input tokens |
|---|---|---|---:|---:|---:|
| S301 | 9af19c48-44ad-48dd-981e-e9041d09d249 | TIMED_OUT | 120.019 | 19,249 | 4,419 |
| S302 | fb4a908a-410e-4122-82ae-ba0f6e609cce | TIMED_OUT | 120.016 | 19,891 | 4,427 |
| S303 | 72cfa443-7f8a-4561-85ba-9de73cb26753 | TIMED_OUT | 120.008 | 19,161 | 4,339 |

Ollama reports 8,192-token context for all three requests. No prompt-truncation
warning occurs; each slot release records truncated=0. At 18:39:24, 18:41:26
and 18:43:27, the HTTP calls end at two minutes and the provider logs cancellation
of tasks 0, 407 and 901, followed by slot release. The next request starts after
cancellation rather than waiting for an abandoned completion. The final slot
state is idle. Partial generated content is discarded; the API stores no report
and leaves each investigation open for an operator.

Generation runs roughly 4-5 tokens/second. The Qwen load offloads 25/37 layers
to the GPU; its 8,192-token f16 KV cache occupies 1,152 MiB across CPU and GPU.
These logs show a remaining local throughput limit. They do not establish that
a longer deadline or another model would produce a valid or grounded report.
The owner chose to retain this limitation rather than authorize seven minutes.

## Verification and preservation

- Red model regressions: four intended failures against the blocking adapter.
  Corrected deterministic wire fixture then reproduces the missing top-level
  truncate control with one failure and no errors/skips.
- Red provenance regressions: two intended missing-column PostgreSQL failures;
  all six tests executed. The first sandbox run skipped Docker tests and is
  preserved as an unsuccessful verification attempt.
- Green: all 52 focused report tests pass with zero failures/errors/skips.
  Real Spring AI/Boot clients against a deterministic loopback server verify
  complete request JSON, actual deadline/interruption socket closure, fragment
  assembly, incomplete/error rejection and no retries. PostgreSQL verifies new
  settings and unchanged historical report/provenance with unknown settings.
- Full `./verify.ps1`: 380 API / 9 MCP / 82 generator / 103 console tests pass,
  zero failures/errors/skips, eight nested Node UI cases, formatting, builds,
  repository-script, Compose and diff checks. This is not an npm-audit gate;
  installation reported ten advisories with unchanged dependency manifests.
- All seven original O3 report histories are unchanged. All 373 protected
  corpus/baseline/comparator/oracle/evaluator hashes match. The unfinished O3
  archive matches the previous task Git blob `4fa61dd5de81038f06a71fad14cd6969360d17c9`.
- All three exact evidence/retrieval bindings, prompt/schema hashes and stored
  settings match; there is exactly one report attempt per new investigation.
- Both original console listeners were stopped only for npm's file lock and
  restored at 127.0.0.1:4200 and [::1]:4200 with HTTP 200. Sandbox restores
  failed with esbuild EPERM; successful restores ran outside the sandbox.

Artifacts remain in ignored `tmp/report-reliability/`: red/green/full logs,
inputs and exact API histories, reconstructed prompts/schemas, provenance,
provider runtime log, model/runtime snapshots, jar hashes and verification
summaries. Original artifacts remain in `tmp/o3/`.

## Limits

No completed live report, confidence/semantic score or grounding improvement is
claimed. The fixed code is verified in the isolated runtime; existing main-demo
API processes were not restarted. Their settings remain those of their loaded
build until restarted with this artifact. Original failures are immutable.
