# Report performance within 150 seconds — 2026-10-04

The final combined profile has zero timeouts in seven fresh first attempts.
Five reports are AVAILABLE and two are MALFORMED; application validation still
rejects invalid model output. All terminal attempts finish in 36.46–93.07 seconds,
with a 68.50-second median. This is a small local sample, not a guarantee of a
low timeout rate under every workload or a semantic-quality improvement claim.

## Scope and retained profiles

The owner authorizes a hard maximum of 150 seconds and performance improvements,
rejecting seven minutes. ADR-0028 preserves the local Qwen model, precision,
8,192-token full context, prompt/schema, 1,536-token output budget, disabled
thinking, no tools, cancellation and strict validation. No report retry, oracle
access, answer-key reveal, comparison or human decision occurs in this task.

- Original O4: six of seven first attempts time out at 120 seconds. Its retained
  outcomes, histories and evaluation remain unchanged.
- Initial GPU-only profile: four AVAILABLE and three TIMED_OUT at 150 seconds,
  retained under tmp/report-speed/. Idle Nomic shares GPU memory and reduces
  throughput. One manually requested idle-Nomic unload during S302 is retained
  with its timestamp; that mixed diagnostic profile is not the final estimate.
- Final combined profile: 35 report GPU layers, a 128-token processing batch,
  and embedding keep_alive=0s, retained under tmp/report-speed-final/. Each
  embedding call unloads Nomic before report generation. The portable application
  defaults remain automatic GPU placement and five-minute embedding residency.

Both isolated databases copy only prepared schema/catalog state: 1,562 knowledge
chunks and initially zero incidents. Each profile uses newly created incidents
and one attempt per case. The initial database is payment_copilot_speed_eval_20261004
on API/generator ports 8093/8094; the final is
payment_copilot_speed_final_eval_20261004 on ports 8095/8096. Historical records
are not copied as new evaluated attempts or overwritten.

## Final first attempts

| Case | Status | Seconds | Provider input tokens | Output tokens | Tokens/sec | Valid references |
|---|---|---:|---:|---:|---:|---:|
| S005 | AVAILABLE | 68.50 | 3,459 | 690 | 13.76 | 9 |
| S301 | MALFORMED | 86.56 | 4,836 | 747 | 12.09 | — |
| S302 | MALFORMED | 82.32 | 4,834 | 723 | 12.59 | — |
| S303 | AVAILABLE | 93.07 | 4,758 | 847 | 12.46 | 10 |
| S002 | AVAILABLE | 67.86 | 3,642 | 649 | 13.37 | 9 |
| S313 | AVAILABLE | 63.86 | 4,853 | 457 | 12.45 | 4 |
| S211 | AVAILABLE | 36.46 | 3,486 | 214 | 12.75 | 2 |

Times use persisted attempt timestamps. Provider metrics bind the last seven
sequential completed chat calls in the retained Ollama log to case order. All
seven complete normally at the provider boundary; the two MALFORMED outcomes
are application rejections. Their raw prose is not retained, so the exact
rejected assertion is not diagnosed or inferred from a later replay.

All 34/34 references in accepted reports belong to their exact persisted
snapshots. S313/S211 retain INSUFFICIENT_EVIDENCE, LOW, null cause/recommendation
and explicit gaps. No accepted statement/rationale violates the prompt's
300-character limit. These bounded checks do not prove general prose support.

## Diagnosis and provenance

Ollama 0.35.1 on a 6 GiB GTX 1060 automatically offloads only 25/37 report layers.
Same-input 96-token microbenchmarks measure 4.72 tokens/sec with automatic
placement, 7.17 with 32 layers, and 13.09 with 35 layers plus batch 128. These
intentionally output-limited diagnostics never become reports. A warm complete
S301 exact-context replay finishes in 67.33 seconds and passes the unchanged
parser. Idle embedding residency then explains the slower initial fresh profile.
The final profile achieves 12.09–13.76 tokens/sec with full inputs.

A real loopback test reproduces Spring AI 2.0 dropping keepAlive when generic
embedding runtime options are merged. An explicit provider-specific request
preserves configured model/query, keep_alive=0s and the exact normalized vector.
The setting affects residency, not vector/ranking semantics. Bulk catalog
preparation can retain 5m to avoid repeated model loading.

- Model qwen3:8b-q4_K_M, digest
  500a1f067a9f782620b40bee6f7b0c89e17ae61f686b92c24933e4ca4b2b8b41.
- report-prompt/v9, context-constrained report-v1; template SHA-256
  dabbb0a2e0082e29e813861a1c53b7acd5bc48af3a5dd880c14f617cd3f308d3.
- Retrieval postgres-pdf-family-related/v5, nomic-embed-text / 768;
  frozen corpus bytes and ranking remain unchanged.
- report-model-settings/v3 preserves 8,192 context tokens, streaming, disabled
  truncation/shift, GPU layers 35 and batch 128. All seven reconstructed prompt/
  schema hashes and options match immutable persisted bindings.
- Final API SHA-256:
  f3eb87846b04ac9a48c3da15bf88d78df406cac0ca30bb4909b789942172f2ff.
- Final generator SHA-256:
  2eb92ea9a47990442605db6db5a1968307f0c806235910e041d2a6fd23aebb32.
- All 373 protected hashes and 17 original O3/reliability/O4 report histories
  remain unchanged. Main-demo report-history fingerprint is unchanged too.

## Verification and activation

Deadline tests fail before the default/cap changes. GPU/batch and service
provenance tests fail before effective options are transmitted/retained. The
embedding wire regression fails before the explicit request. Final focused
report tests (78) and configuration/embedding tests (5, one shared configuration
case) pass without failures or skips. The first attempted PostgreSQL test is
skipped due to sandbox denial of Docker's named pipe; the escalated focused
suite and full gates execute that coverage with zero skips.

Final ./verify.ps1 passes 406 API / 9 MCP / 82 generator / 103 console tests,
including eight nested Node cases, plus formatting, builds, repository scripts,
Compose and diff checks. Logs remain in tmp/report-speed/. Consoles are
stopped only during locked npm verification and restored on both loopbacks.

The main API is restarted with the exact final isolated artifact and tuned
.env values. Health is UP, its provider remains localhost:8082, and both console
loopbacks return HTTP 200. Two historical STARTED attempts from October 1
predate the October 4 API process; they remain unchanged. No current-process
report is interrupted. Those abandoned historical records are not reconciled. The four evaluation
services created by this task are stopped after verification to free memory;
their databases and artifacts are retained, and the main demo stays healthy.

## Limits

Two cases still fail validation; faster inference does not repair grounding.
Seven sequential cases cannot establish a broad timeout frequency or performance
under concurrent workloads. Explicit GPU placement depends on available VRAM;
other GPU workloads can reintroduce paging. Shorter configured deadlines remain
allowed, while values above 150 seconds fail startup. Original semantic-quality
limitations and unfinished O3 work remain. No terminal decisions or comparisons
are added to make the evaluation look successful.
