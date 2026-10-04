# Task: Preserve report context and cancel timed-out generation

Status: Complete within authorized scope; live throughput/timeouts remain
Created: 2026-10-04
Owner: Christopher Guzowski

## Goal

Resolve the measured prompt truncation and continued provider work behind the
O3 timeout cases without concealing failures or changing report semantics.

## User story

As an analyst, I want generation to receive the complete supplied context and
stop when its deadline expires so that later requests are not delayed by
abandoned work and every outcome remains auditable.

## Chosen contract

Keep Qwen, report-prompt/v8, the report-v1 schema, temperature zero, disabled
thinking, no tools, 1,536 output tokens and the existing two-minute deadline.
Set an explicit 8,192-token Ollama context for measured inputs. Send top-level
truncate=false and shift=false: oversized input or exhausted context fails
closed rather than silently dropping instructions or evidence. Use Spring AI's
reactive transport internally, assemble only a completed response, and cancel
its subscription on deadline/interruption. The HTTP report API remains a single
completed result. Persist versioned context/transport settings for new attempts;
leave historical settings unknown and historical rows unchanged.

## In scope

Report model transport/options, deterministic wire and cancellation regressions,
additive Flyway provenance, documentation, focused/full verification, and one
fresh live attempt each for S301, S302 and S303 in an isolated evaluation runtime.

## Out of scope

O3 prose quality, retrieval/corpus changes, model substitution, longer deadlines,
automatic retries or decisions, oracle inputs, comparator changes, historical
mutation, commits and pushes.

## Constraints

Preserve tenant isolation, exact persisted snapshots/citations, schema validation,
INSUFFICIENT_EVIDENCE/LOW/null for degraded evidence, human review and immutable
original failures. No live provider in automated tests or new dependency.
Preserve O3 as unfinished in 2026-10-04-unfinished-o3-grounded-recommendations.md.

## Acceptance criteria

- [x] Deterministic wire tests prove complete input, num_ctx=8192, top-level
  truncate=false/shift=false, existing schema/output settings and one call.
- [x] Deadline and caller interruption cancel the actual HTTP subscription;
  partial output and provider errors never become successful reports.
- [x] New attempts retain exact versioned settings; historical attempts remain
  readable with unknown settings and unchanged report/provenance bytes.
- [x] Focused tests and the full verification gate pass with no skipped tests.
- [x] Fresh isolated S301/S302/S303 attempts and runtime evidence are retained;
  original timeouts remain unchanged and any unresolved live failures are explicit.

## Test plan

SpringAiReportModelTest covers options, fragment assembly, timeout/error mapping
and cancellation. SpringAiReportTransportTest uses a deterministic local HTTP
server through the real Spring AI client to verify the JSON wire request,
rejection, premature termination and socket cancellation. Report persistence
PostgreSQL tests cover new settings, nullable legacy settings and immutable
terminal outcomes. Existing prompt/parser/service/API tests cover schema,
citations, degraded evidence and lifecycle. Run focused report tests, then
./verify.ps1. Live checks use fresh synthetic incidents without retries/reveal;
retain exact snapshots, prompt/schema/settings, timing and provider runtime logs.

## Progress notes

- Owner authorized a separate reliability task after the timeout diagnosis.
- Preserved the unfinished O3 task byte-for-byte before activating this task.
- Plan: reproduce transport/options and provenance defects, implement one
  behavior at a time, verify focused/full gates, then measure three fresh cases.
- Owner explicitly chose to keep the two-minute deadline and retain the measured
  throughput limitation; no longer-deadline validation is authorized.
- Risks: 8,192 context uses more local memory; preserving input cannot guarantee
  generation speed or semantic quality. Cancellation closes the HTTP request;
  live logs must confirm the installed provider stops its work.

## Completion evidence

- Red model tests fail four cases against blocking generation (red-model.log).
  The corrected wire fixture fails once for absent top-level truncate=false
  (red-wire-final.log); the first fixture failure is retained separately.
- Red PostgreSQL executes six tests with two intended missing-column failures
  (red-provenance-docker.log). The initial sandbox Docker skips are retained
  in red-provenance.log and do not count as verification.
- Green-focused.log passes all 52 focused report tests without failures/errors/
  skips, including actual HTTP cancellation/interruption, incomplete/error
  rejection, exact wire options and nullable legacy provenance.
- Full ./verify.ps1 passes 380 API, 9 MCP, 82 generator and 103 console tests,
  zero failures/errors/skips, eight nested Node cases, formatting, builds,
  repository-script, Compose and diff checks (full-verification.log).
- Three fresh first attempts remain TIMED_OUT at 120.019/120.016/120.008 seconds.
  Ollama receives 4,419/4,427/4,339 full input tokens in an 8,192 context and
  confirms cancellation/slot release at every deadline with truncated=0.
  Provider throughput remains roughly 4-5 tokens/second; no partial report,
  automatic retry, decision, reveal or comparison was persisted.
- Seven original O3 histories and all 373 protected hashes remain unchanged.
  Three reconstructed prompt/schema hashes match persisted settings and exact
  snapshot bindings. The unfinished O3 archive equals the previous Git blob.
- Both console loopback listeners were restored with HTTP 200. The first
  sandbox launches failed with esbuild EPERM; outside-sandbox restores passed.
- [Retained live evaluation](../../../SynTen%20Inc/evaluation/2026-10-04-report-context-and-cancellation.md)
  records exact artifacts, first-attempt failures and limitations. Logs and
  reconstruction/verification scripts are under tmp/report-reliability/.

## Remaining limitations

The owner retained the two-minute deadline. All three full-context live cases
still time out because local throughput is limited; timely report completion
and semantic quality are not established. Context preservation and cancellation
are verified. The existing main API was not restarted; only the isolated runtime
uses this artifact. Historical attempts remain unchanged.

## Decisions needed

None. Owner authorized context budgeting and cancellation as a separate task.
