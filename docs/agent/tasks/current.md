# Task: Automatic post-reveal investigation comparison

Status: Complete — full gate, browser QA and live comparison passed
Created: 2026-10-01
Owner: Christopher Guzowski

## Goal

Automatically compare the frozen investigation with its revealed answer key.

## User story

As a demo evaluator, I want simple scored comparisons beneath the answer key,
with separate colored report and human-decision cards, without manual grading.

## Chosen contract

Only root-cause and recommendation prose is scored by a separately prompted
local Ollama evaluator. All other scores, aggregation, decision expectations and
colors are deterministic. Disposition and confidence match exactly (0/100).
The report score is the rounded equal-weight mean of the four metrics; Good is
80-100, OK is 50-79, Bad is 0-49. The expected human outcome is APPROVED only
when disposition/confidence match and both text scores are at least 80;
otherwise REJECTED. Its match is 0/100, separate from report quality.
Correct null cause/recommendation for INSUFFICIENT_EVIDENCE scores 100 in code;
forbidden assertions score 0. Missing comparison inputs or failed evaluation
are Not scored, never silently zero. A model failure retries once automatically.
Comparisons are advisory and cannot change reports or human decisions.
Retain immutable local comparison artifacts with inputs, outputs, IDs, hashes,
prompt/rubric/model versions and timestamps. Answer keys remain sealed until a
terminal human decision; compare its exact bound report, never an unrelated retry.

## In scope

Generator-only evaluation endpoint, tenant-scoped read-only API integration,
versioned prompt/rubric, local artifacts, two accessible cards and diamond gauges.

## Out of scope

Report generation changes, oracle/corpus changes, automatic operational actions,
new model downloads, manual grading, offline benchmark reinterpretation.

## Constraints

Synthetic data only. No oracle or evaluator input enters report generation,
retrieval, evidence or operator decisions. Tests require no live model provider.
Use the existing local model by default with an independently configurable judge.

## Acceptance criteria

- [x] Comparison occurs automatically after reveal, below the answer key.
- [x] Only root-cause/recommendation prose is LLM-scored; other scoring is deterministic.
- [x] Terminal gate, tenant/incident/investigation/report/decision bindings are enforced.
- [x] Separate report and decision cards display labels, colored borders and black diamond gauges.
- [x] Failures, missing inputs, insufficient evidence and stale UI requests are handled safely.
- [x] Artifacts retain reproducible input/prompt/output/provenance metadata.
- [x] Focused and full verification pass; desktop/mobile visual behavior is checked.

## Test plan

Write tests before production behavior. Comparison rubric tests cover exact
matches/misses, thresholds, correct rejection of a bad report, null semantics,
judge isolation and failures. HTTP tests cover tenant-scoped exact report selection,
mismatches, provider timeout/malformed/out-of-range JSON and successful requests.
Artifact tests cover immutable retained inputs/results. UI behavior tests cover
automatic sequencing, loading/failure/retry, reset and stale responses, and
endpoint-safe rendering. Run focused generator tests, full ./verify.ps1 and
browser QA at desktop/mobile widths. Live evaluation is an explicit smoke check,
not a deterministic test or proof of judge accuracy.

## Progress notes

- Read repository/product/quality/architecture context and oracle separation ADRs.
- Owner chose an LLM for text portions only; everything else uses code/rubric.
- Clean checkout before work. Previous completed CI task archived unchanged.
- Rubric, service and HTTP tests were added before their implementation; initial
  focused runs failed for missing comparison types. UI tests failed all seven
  cases before the UI behavior was added, then passed. Duplicate/trailing JSON
  and system/user separation assertions failed for their intended reasons before
  the stricter parser and prompt separation fixes.
- The first full gate passed backend checks but stopped at an existing UI
  string assertion after formatting changed quote style. Preserved existing
  JavaScript single-quote style; made HTML copy assertions whitespace-insensitive
  while adding both comparison-card assertions. Focused UI tests then passed,
  followed by the complete gate. No checks were skipped or weakened.
- Stopped only verified Angular preview PID 4336 for the npm ci/esbuild lock.
  Restored console PID 12664; restarted only generator PID 38000 as tested
  packaged snapshot PID 28812. Generator and console return HTTP 200.

## Completion evidence

- Focused ComparisonRubricTest passed six initial cases; focused comparison
  rubric/service/input-client/judge/controller suite passed 19 cases. Later
  duplicate/trailing JSON and system/user separation checks passed, bringing the
  comparison Java coverage to 21 cases. Focused StaticUiContractTest and
  ComparisonUiBehaviorTest passed after formatting correction.
- Full ./verify.ps1 passed on 2026-10-01: 362 API, 9 MCP, 74 generator and
  103 console tests, zero failures/errors/skips; all repository scripts,
  Java/Prettier formatting, builds, Compose and diff checks passed. The generator
  UI harness additionally ran seven deterministic Node behavior cases.
- Browser QA used a clearly labeled synthetic fixture on temporary port 8093;
  no live incident or human decision was created. Confirmed red/yellow/green
  report borders and independent green decision card; 0/100 diamond placement,
  accessible meter labels/numeric values, keyboard Enter reveal, preserved key
  and no fake zero gauges during outage. At 390x844, document scrollWidth 375
  (scrollbar) <= innerWidth 390; desktop width 914 also had no overflow. Temporary
  viewport override was reset.
- Live existing S303 incident 50726425-26de-4756-966d-2fbdcdfb80d3 / investigation
  1edc942f-c32c-47f7-9fc4-8e3332e946c0: comparison
  689ead6c-1936-4a5c-b5dd-f184dc4ce9cf AVAILABLE in 24.802 seconds, one local
  qwen3:8b-q4_K_M call. Exact report
  2d33467b-213c-4ab5-ad09-853ef00b6a3e and decision
  bdf93b2d-647f-42db-bb6b-e701760708b1 retained. Disposition 100, confidence 0,
  cause 40, recommendation 40 => report 45/BAD; REJECTED matched rubric =>
  decision 100/GOOD. Both API histories remained byte-identical before/after.
- Retained local artifact:
  syntheticIncidentGenerator/tmp/comparisons/8b860d80-d17f-4e6b-8c48-af35f26a4d61/689ead6c-1936-4a5c-b5dd-f184dc4ce9cf.json.
  Independently recomputed input, prompt and response SHA-256 hashes all matched.
  Full gate log: tmp/comparison-full-verify.log. No report generation or human
  decision was performed during verification.
- Final Repository scope, generator JavaScript syntax, explicit generator
  Prettier checks and focused static/UI behavior tests passed after documentation
  and CSS formatting. Reviewed the final diff for scope and generated/secret
  content. Temporary preview process/tab were closed; actual demo services remain
  running. No commit or push was performed.

## Remaining limitations

LLM text scores remain advisory and may vary or be wrong. Same-model judging is
the default for local compatibility; another model can be configured independently.
The four-field comparison rubric does not establish general semantic entailment
or complete evidence support. Artifacts are local ignored files requiring backup,
not new copilot database/audit-timeline records. GitHub verification was not run
for these uncommitted changes. Existing npm advisories remain unchanged.

## Decisions needed

None; owner authorized automatic text judging with deterministic remaining metrics.
