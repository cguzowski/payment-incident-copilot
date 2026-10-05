# ADR-0028: Bound report generation to 150 seconds and expose measured GPU tuning

Status: Accepted
Date: 2026-10-04
Decision owner: Christopher Guzowski

## Context

The owner reports frequent timeouts, rejects a seven-minute alternative, and
authorizes at most two minutes thirty seconds plus other speed improvements.
ADR-0026's cancellation and preserved context work, but the latest isolated
O4 run timed out six of seven first attempts. Ollama on this workstation's
6 GiB GTX 1060 automatically placed only 25 of 37 Qwen layers on GPU.

## Decision

The default deadline is 150 seconds. Reject nonpositive configuration and any
value above 150 seconds during construction, while preserving shorter test and
operator limits. Cancellation and fail-closed incomplete output remain unchanged.
This supersedes ADR-0012/0026 only for the previous two-minute limit.

Expose REPORT_GPU_LAYERS (default -1, automatic) and REPORT_BATCH_TOKENS
(default 128) as explicit Ollama per-call options. Reject GPU layers below -1
and nonpositive batch sizes. These affect execution placement and processing
batch size, not the model, evidence selection, context length, precision, prompt,
output budget or validation. Embedding semantics and the comparison judge are unchanged.
The local workstation may use measured 35-layer placement; do not make that
hardware-specific setting the portable application default.

Expose KNOWLEDGE_EMBEDDING_KEEP_ALIVE with the existing five-minute portable
residency default. On this constrained workstation, set it to 0s so each completed
embedding call unloads that model before report generation. Spring AI 2.0's
embed(String) passes generic runtime options and drops provider-specific default
keepAlive during its merge. The existing Ollama embedding adapter now uses an
explicit EmbeddingRequest with the configured Ollama keepAlive option. A real
loopback wire test reproduces the dropped setting and proves the transmitted 0s,
unchanged query/model and exact 768-dimensional normalized returned vector.
Embedding content, dimensions, normalization, retrieval ranking and judge remain
unchanged. Explicit bulk preparation can retain 5m to avoid repeated load costs.

Retain effective options from the model adapter on each new attempt using
report-model-settings/v3, adding gpuLayers and batchTokens to existing JSONB.
Terminal attempts preserve their start settings. Historical v2 rows deserialize
new nullable options as unknown; historical NULL settings remain unknown.
No migration or historical backfill. Public report-v1 remains unchanged.

## Evidence and consequences

Same-input 96-token diagnostics measured 4.72 tokens/s with automatic placement,
7.17 with 32 layers, and 13.09 with 35 layers and a 128-token batch. A warm
complete S301 exact-context replay finished in 67.33 seconds, 790 output tokens,
12.19 tokens/s, normal stop, and passed the unchanged parser. This replay does
not measure cold-request latency or timeout frequency. Fresh first attempts and
the full deterministic gate must establish the practical limits separately.

Explicit placement can exceed available GPU memory on another machine or while
other GPU workloads run, causing allocation failure or slower paging. Operators
can return to -1 without changing persisted history. Neither faster generation
nor passing bounded validation proves semantic report quality. No fallback,
automatic retries, cloud transfer, decisions or oracle inputs are introduced.
