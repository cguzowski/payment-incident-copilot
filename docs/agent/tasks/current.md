# Task: Reduce report timeouts within 150 seconds

Status: Complete; measured local speed improvement, zero timeouts in seven final attempts
Created: 2026-10-04
Owner: Christopher Guzowski

## Goal

Make report timeouts uncommon and improve generation speed within the owner's
hard maximum of two minutes thirty seconds.

## User story

As an analyst, I want reports to finish reliably within 150 seconds so that
investigations do not repeatedly fail while waiting for the local model.

## Chosen contract

The owner rejects seven minutes and authorizes a maximum 150-second deadline
plus performance improvements. Investigate and measure provider execution and
output overhead. Preserve full evidence/guidance, strict validation, cancellation,
tenant isolation, immutable histories and mandatory human review. Start with
the installed local model; no hosted provider or automatic report retries.

## In scope

Deadline configuration, measured local-provider optimizations, deterministic
regressions, provenance, focused/full verification, fresh synthetic diagnostics
and activation in the local demo after verification.

## Out of scope

Corpus/retrieval/answer-key/comparison changes, historical mutation, automatic
human decisions, cloud providers, commits and pushes.

## Constraints

No evidence truncation or weakening of report-v1 and O4 validation. Preserve
user-owned pending O4 changes. No new dependency. Use red-green-refactor.

## Acceptance criteria

- [x] Default report deadline is 150 seconds and configuration cannot exceed it.
- [x] A measured optimization improves local generation speed without dropping
  evidence or accepting partial/invalid output.
- [x] Deadline cancellation, invalid responses, tenant/citation binding and
  immutable legacy provenance remain covered by passing deterministic tests.
- [x] Fresh first attempts record latency, outcomes and timeout frequency;
  insufficient sample or remaining failures are explicitly reported.
- [x] Focused tests and full ./verify.ps1 pass; verified local runtime is activated.

## Test plan

AiModelConfigurationTest and ReportModelCallExecutorTest cover the default and
maximum deadline. Model/transport/persistence tests cover measured provider
options and their provenance, unchanged context and cancellation. Run focused
report tests, then ./verify.ps1. Use nonpersisted exact-context diagnostics to
select an optimization, then fresh isolated first attempts with no oracle access,
retry or decisions. Review preserved historical/corpus hashes where applicable.

## Progress notes

- Archived the completed O4 task unchanged before replacing current.md.
- Existing logs show 25/37 GPU layers, CPU spill and roughly 4-6 generated
  tokens/second; Flash Attention is already enabled on the 6 GiB GTX 1060.
- Plan: measure safe GPU placement; implement/test the selected optimization
  and 150-second cap; verify and retain fresh outcomes; activate the local demo.
- The initial profile exposes idle embedding-model memory pressure; the final
  profile combines 35 GPU layers, batch 128 and immediate embedding unload.
- Risks: GPU memory pressure can worsen speed; concise model output or changed
  execution precision can affect quality and require separate validation.

## Completion evidence

- Deadline red: AiModelConfigurationTest and ReportModelCallExecutorTest execute
  five tests with two intended failures; green passes all five. Above-maximum
  integer and fractional deadlines are rejected, and 150 seconds is accepted.
- GPU/batch regressions fail before options are applied; service provenance fails
  with automatic -1 instead of configured 35. All 78 focused report tests pass
  without failures/skips, including actual socket cancellation and legacy v2 reads.
- First full ./verify.ps1 passes 405 API / 9 MCP / 82 generator / 103 console
  tests with no failures/errors/skips. Console processes were temporarily stopped
  to avoid the previously reproduced esbuild lock and their commands restored.
- Seven initial GPU-only first attempts retain four AVAILABLE reports and three
  TIMED_OUT outcomes at 150 seconds. Citation bindings and degraded contracts
  pass; 373 protected hashes and 17 original report histories remain unchanged.
  One manual idle-embedding unload during S302 is retained with its timestamp;
  this initial profile is not a clean estimate of the final combined settings.
- Embedding-residency wire regression reproduces missing keep_alive even after
  property binding. An explicit provider request preserves keep_alive=0s and
  the configured Nomic model. Five focused configuration/embedding tests pass,
  preserving exact query/vector, dimensions, normalization and failure mapping.
- Final ./verify.ps1 passes 406 API / 9 MCP / 82 generator / 103 console
  tests with zero failures/errors/skips, eight nested Node cases, formatting,
  builds, repository scripts, Compose and diff checks (full-verification-final.log).
- Seven final combined-profile first attempts return five AVAILABLE and two
  MALFORMED, with zero TIMED_OUT. Persisted latency ranges 36.46–93.07 seconds,
  median 68.50; full-input provider throughput ranges 12.09–13.76 tokens/sec.
  All 34/34 accepted references are valid, degraded LOW/null is preserved,
  and seven exact prompt/schema hashes and v3 GPU/batch settings match storage.
- All 373 protected hashes, 17 original histories and the main-demo report-history
  fingerprint remain unchanged. The main API is activated with the exact final
  isolated artifact, health UP and unchanged localhost:8082 evidence binding;
  IPv4/IPv6 consoles return HTTP 200. Two October 1 STARTED records predate the
  current API process and remain unchanged; no live report is interrupted.
- [Retained evaluation](../../../SynTen%20Inc/evaluation/2026-10-04-report-performance.md)
  records both profiles and limitations. Raw evidence remains under
  tmp/report-speed/ and tmp/report-speed-final/. No retries, oracle access,
  answer-key reveal, comparisons or human decisions occur.

- Final documentation/static verification passes ./verify.ps1 -Scope Repository
  and git diff --check. Only the four newly created evaluation services are
  stopped afterward to free memory; retained databases/artifacts and the main
  demo remain available. Main history/health and both console listeners pass again.

## Remaining limitations

Zero timeouts in seven final sequential cases is a limited sample, not a general
frequency guarantee. Two MALFORMED reports and O3 semantic-quality limitations
remain. Explicit GPU placement can degrade under other GPU workloads; bulk
knowledge preparation can use 5m residency to avoid repeated embedding loads.
Two historical STARTED records remain unreconciled and unchanged.

## Decisions needed

None for the authorized deadline and reversible performance diagnostics. A new
provider/model or material report-contract change requires an owner decision.
