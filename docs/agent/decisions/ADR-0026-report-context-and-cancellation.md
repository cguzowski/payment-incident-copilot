# ADR-0026: Preserve report context and cancel abandoned model work

Status: Accepted
Date: 2026-10-04
Decision owner: Christopher Guzowski

## Context

The owner authorized a separate reliability task after O3's S301/S302/S303
first attempts timed out. Retained Ollama 0.35.1 logs show approximately 4,400
input tokens truncated to 2,050 in a 4,096-token context. Two blocking requests
continued beyond the API's two-minute deadline and delayed later requests.
Cancelling a virtual-thread FutureTask did not cancel that HTTP transport.
O3 remains unfinished; its prompt and semantic shortcomings are preserved.

## Decision

Keep the ADR-0012 model, temperature, disabled thinking, no tools, application
schema, 1,536 output-token budget and two-minute attempt deadline. Explicitly
request num_ctx=8192. This fits the measured inputs and output reserve; it is
not a universal input-size guarantee. Send Ollama's top-level truncate=false
and shift=false to reject oversized input or exhausted context instead of
discarding instructions/evidence. Never shorten or reorder persisted context
at the report boundary.

Use Spring AI ChatModel.stream internally and consume its reactive subscription
on the existing worker. Interruption cancels the subscription and closes the
HTTP stream. Assemble fragments only after normal completion with a stop
finish reason. Errors, cancellation, empty/incomplete transport completion and
output-limit termination fail closed; partial prose never becomes a report.
The report HTTP API still returns one terminal result. No retry or fallback.

Spring AI 2.0.0's Ollama ChatRequest lacks top-level truncate/shift fields.
A narrowly selected WebClient JSON encoder serializes that request through its
existing annotated record shape and adds the two controls. Other request types,
including embeddings, retain their codecs. Deterministic loopback tests use the
real Spring AI client and Boot WebClient wiring to check the transmitted JSON
and socket closure. Remove this compatibility codec when Spring AI supports
these controls directly and wire tests prove equivalent behavior.

V12 adds nullable JSONB model_settings. New attempts retain
report-model-settings/v2, contextTokens=8192, stream=true, truncate=false and
shift=false. Existing temperature/output fields remain. Shared constants drive
the adapter and provenance. Historical settings remain NULL; no fabricated
backfill or historical updates. The public report response stays compatible.

## Consequences

More context increases memory demand and may reduce local generation speed.
Provider rejection remains the existing safe UNAVAILABLE outcome. Closing a
socket allows server cancellation; workstation smoke logs must verify the
installed Ollama honors it. This does not guarantee timely or semantically
correct reports. Preserve every live failure and measure fresh attempts once.
No retrieval, corpus, oracle, evaluator, model or human-decision changes.

## Validation

Deterministic tests cover wire controls, full prompt, fragment assembly,
midstream errors, premature completion, provider rejection, actual deadline and
caller-interruption socket closure, nullable legacy provenance and immutable
terminal settings. Focused and full verification plus retained fresh S301/S302/
S303 checks are recorded in the active reliability task.

The owner subsequently chose to retain the two-minute deadline after fresh
full-context attempts still timed out at roughly 4-5 generated tokens/second.
A seven-minute alternative was not authorized. Context preservation and
cancellation are verified; timely completion remains a measured limitation.
