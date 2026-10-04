# Project status

Last updated: 2026-10-04

## Current state

The local demo is complete; the owner has activated O3 grounded recommendations. Seven
synthetic incident families support intake, active/completed queues, MCP
service-error evidence, approved PDF retrieval, advisory reports, explicit
human decisions and audit history. Normal generation selects AVAILABLE
scenarios; deliberate degraded fixtures remain available to tests/evaluations.

The prepared local catalogs contain 30 authorization PDF versions / 705 chunks
and 16 independent payment PDFs / 65 chunks using nomic-embed-text / 768.
Startup verifies readiness without importing; use start-local.bat
-PrepareKnowledge for explicit preparation. Historical Markdown/PDF rows,
citations, frozen assets and previous attempts remain readable and unchanged.
See the [completed adoption task](tasks/completed/2026-10-01-adopt-frozen-payment-pdf-library.md).

Automatic post-reveal comparison is complete under ADR-0021. Separate report
and human-decision cards use colored borders and black-diamond 0-100 gauges.
Only cause/recommendation prose uses an independently prompted local LLM;
other scores and expected decisions are deterministic. Evaluation retries once
before leaving both cards Not scored. Immutable tenant-scoped local artifacts
retain exact terminal report/evidence/decision bindings and judge provenance.
See the [completed comparison task](tasks/completed/2026-10-01-automatic-post-reveal-comparison.md).

The [first cleanup](tasks/completed/2026-10-01-demo-maintenance-cleanup.md) is complete: unused CSS aliases,
a deployment placeholder and 2,865 disposable local files (40,096,595 bytes)
were removed. Active runtime files and retained artifacts are preserved.
The [deferred thorough cleanup](tasks/2026-10-04-deferred-demo-cleanup.md) retires all 13 Python authoring scripts
into a [hash-verified historical archive](../../SynTen%20Inc/history/pdf-authoring-tools/README.md)
and removes an unused report helper and ORM dependency. Frozen assets are
unchanged. Its original unfinished task and verification limitations are retained
byte-for-byte. O1's subsequent full verification passed against the current
checkout; the cleanup task has not been independently closed. The latest observed
main demo attempts now use report-prompt/v7.
Feature expansion and AWS deployment remain deferred;
see the [roadmap](ROADMAP.md).

The owner has queued [four next investigation-quality tasks](tasks/next.md):
confidence calibration, useful operational passages, specific grounded
recommendations and observed-fact/source preservation. The owner clarified
[confidence selection](tasks/completed/2026-10-04-attainable-high-confidence.md): report-prompt/v7 restores attainable
HIGH, normally favors MEDIUM, and uses LOW for weak support. HIGH requires
exceptionally strong support of the narrow mechanism, not independent
confirmation as a prerequisite. Degraded LOW/null behavior remains enforced.
[ADR-0024](decisions/ADR-0024-attainable-high-confidence.md) supersedes v6's
ceiling. O2/O4 remain queued; [O3](tasks/current.md) is active. The owner subsequently authorized
[explicit confidence expectations](tasks/completed/2026-10-04-evidence-based-confidence-expectations.md) under
[ADR-0025](decisions/ADR-0025-evidence-based-confidence-expectations.md).
confidence-evidence/v1 evaluates the decision-bound latest snapshot independently
of actual report confidence. comparison-rubric/v2 changes only that expected
input; exact match, other metrics, weights, bands and decision formula stay intact.
Original keys and historical artifacts remain unchanged. HIGH is attainable for
three consistent direct diagnostic signatures without independent confirmation.

O3 introduces report-prompt/v8 instructions for bounded observed mechanisms,
explicitly unverified deeper hypotheses and guidance-supported owners, records
and retry safeguards. The focused 28 prompt/parser and PostgreSQL HTTP tests
and full ./verify.ps1 pass (371 API / 9 MCP / 82 generator / 103 console, zero
failures/errors/skips, formatting/builds/Compose/diff checks). [Seven isolated
first attempts](../../SynTen%20Inc/evaluation/2026-10-04-o3-grounded-recommendations.md)
returned four reports and three timeouts. Both degraded cases preserve LOW/null
and 25/25 references are valid, but unsupported/generic prose and guidance copied
into observations persist. The owner authorized four fixed diagnostic rejections
in the isolated evaluation database; comparisons completed with report scores
88/100/90/100, cause/recommendation 70/80, 100/100, 80/80 and 100/100.
Two judge calls required no retries; degraded scores check null fields only.
Seven report histories and 373 protected hashes remain unchanged. O3's first two
grounding criteria remain unmet despite GOOD score bands; no reliable improvement
is established. The three timed-out cases remain open and unrevealed.

## Verification and retained evidence

- Evidence-based confidence calibration passed all 82 generator tests, including
  eight nested Node UI cases. Regression logs and exact retained-input replay are
  in tmp/confidence-rules/. Both recent v7 rate-limit/DNS comparisons independently
  expect MEDIUM and score confidence 100 using retained text scores; original
  artifact hashes remain intact. This replay makes no model calls or historical
  writes. Full ./verify.ps1 passed 368 API, 9 MCP, 82 generator and 103 console tests with zero failures/errors/skips plus formatting, builds, Compose and diff checks. The first full run failed only on a task-document trailing blank line; the repaired final run passed.

- Attainable-HIGH v7 correction passed 25 focused report tests in final full
  verification, and full ./verify.ps1 passed 368 API, 9 MCP, 74 generator and
  103 console tests with zero failures/errors/skips plus formatting/build checks.
  Logs are tmp/confidence-v7-*.log. Initial nonpersisted live diagnostics chose
  MEDIUM/MEDIUM/LOW; the weak MEDIUM relied on unsupported synthetic metadata.
  A tightened prompt's weak replay timed out at 120 seconds. All results remain
  in tmp/confidence-v7-live/. Frequency and semantic quality remain unestablished;
  exact confidence comparison, answer keys and historical scores are unchanged.

- O1's focused red-green cycle passed 24 prompt/parser and PostgreSQL HTTP tests.
  Full ./verify.ps1 passed 367 API, 9 MCP, 74 generator and 103 console tests,
  with zero failures/errors/skips, plus seven nested Node cases, formatting,
  builds, repository and Compose checks. Logs remain under tmp/o1-*.log.
  [Seven fresh O1 attempts](../../SynTen%20Inc/evaluation/2026-10-04-o1-confidence.md)
  yielded three AVAILABLE reports and four timeouts, no retries. Completed
  comparison scores are 88/100/64 with confidence matches 2/3 and 24/24 valid
  citation references. S002 expected HIGH and could not match v6's ceiling;
  these historical scores are unchanged. S301/S302/S303 timed out, leaving the original confidence
  misses unmeasured. No overall answer-key improvement is established.

- The [five-case investigation diagnostic](../../SynTen%20Inc/evaluation/2026-10-01-investigation-diagnostic.md)
  completed fresh terminal decisions/reveal/comparison with report scores
  78/45/48/63/100 under unchanged comparison-rubric/v1. Five of seven report
  calls succeeded; reconciliation timed out twice. Exact disposition matched
  5/5, confidence 2/5 and citation membership 43/43. Measured weaknesses include
  generic recommendations, unsupported HIGH confidence, metadata-only retrieval
  and an incorrect observed count. No production optimization or improvement
  claim is made; the cleanup's pending verification is unaffected.

- After the first cleanup, full ./verify.ps1 passed 362 API, 9 MCP, 74 generator and
  103 console tests with zero failures/errors/skips, plus seven nested Node UI
  cases, script checks, formatting, builds, Compose and diff checks.
  Console, API health, generator root and generator health return HTTP 200;
  console was restored after a temporary npm-install file-lock shutdown.
- Comparison desktop/mobile QA verified independent card colors, accessible
  gauges, keyboard reveal, unscored failures and no overflow at 390 CSS pixels.
  Existing terminal S303 compared with local Qwen in 24.802 seconds:
  report 45/BAD, rejection 100/GOOD. API histories stayed byte-identical and
  artifact hashes matched. This proves integration, not judge accuracy.
- The [CI repair](tasks/completed/2026-10-01-restore-github-repository-checks.md)
  passed [GitHub run 36883849928](https://github.com/cguzowski/payment-incident-copilot/actions/runs/36883849928)
  on 2566145. No GitHub verification is claimed for subsequent work.
- [Q2 retrieval results](../../SynTen%20Inc/evaluation/q2-retrieval-results.md)
  retain the passing benchmark and individual misses. K4/K5 and the originally
  recorded Q2 artifact remain absent; successor artifacts have separate hashes.
- [Q5 live results](../../SynTen%20Inc/evaluation/q5-live-results.md) retain
  all 36 scenarios: 34/36 dispositions, 9/36 confidence levels, 26/36 signal
  checks and 374/374 valid citations; median/p95 latency 90.764/109.187 seconds.
  There were no terminal model failures. The two degraded cases violated the
  null contract. [Q6 results](../../SynTen%20Inc/evaluation/q6-live-results.md)
  retain fresh passing insufficient-evidence attempts and the mixed diagnostic.
- Detailed milestone checks and original failures remain in
  [completed tasks](tasks/completed/), [ADRs](decisions/) and the
  [tenant documentation](../../SynTen%20Inc/README.md).

## Known limitations

- Confidence expectations are bounded explicit aggregate rules, not calibrated
  probabilities or general semantic verification. Three direct diagnostic pairs
  can reach HIGH; other substantial mechanisms stay MEDIUM until reviewed.
  Required-code extraction does not prove the key's prose conditions, affected
  paths, deeper causes, approved-guidance applicability or final payment outcomes.

- v7 confidence selection is advisory; structural validation permits HIGH but
  cannot prove its justification or guarantee how frequently a level is selected.
  Live timeout and grounding defects persist. The owner's webhook investigation
  had three v6 MALFORMED attempts; Ollama logs confirm each prompt was truncated
  from 4,209 to 2,050 tokens. A nonpersisted replay violated insufficient-evidence
  null fields. These failures remain unchanged; context budgeting is unresolved.

- Post-reveal text scores are advisory and not independently calibrated.
  The local default judge is the report model with a separate prompt; a different
  installed model is configurable. The four-field deterministic approval rubric
  is not general semantic verification of every oracle decision-rule condition.
  Comparison artifacts are ignored local files, not database/audit-timeline
  records; durable retention requires backing up their configured directory.

- Q5 reports for partial S111 and unavailable S211 assert causes/recommendations
  at MEDIUM confidence instead of the required insufficient-evidence/LOW posture.
  These historical reports remain immutable. Q6's fresh attempts pass the corrected
  contract. Valid source IDs and schema do not establish supported conclusions;
  all PARTIAL evidence is now conservatively treated as insufficient, and a full
  new 36-scenario v5 live run has not been performed.
- Historical Q5/Q6 report attempt metadata records 4,096 output tokens despite
  the adapter's 1,536 limit. New attempts and adapter options now share the
  ADR-0012 1,536-token setting; historical records remain unchanged. A running
  API must be restarted with the updated build to use corrected provenance.
- The report-grading CLI now preserves timestamp strings under PowerShell 7.6.5,
  retaining strict validation. Historical Q5 CLI failure and its direct-module
  workaround remain recorded; executable CLI regressions cover the corrected path.

- Q3's deterministic unsupported-claim detector is deliberately bounded. It
  cannot establish general natural-language entailment, and the fixture does
  not measure live-model quality. Q5 separately records broad live results with
  explicit confidence and insufficient-evidence failures; no promotion threshold exists.
- The benchmark PASS permits individual misses: KQ-004/S005 and KQ-019/S109
  omit PL-002, and KQ-018/S201 still ranks its weak match above its primary.
  The first two are policy regressions from baseline. Aggregate success is not
  universal source coverage or evidence of report quality.
- K4/K5 and the originally recorded Q2 artifact remain absent. New retained
  baseline and passing artifacts have separate identities and hashes.
- Existing databases require explicit corpus reimport after V10 to populate
  derived relationship metadata. Unprepared documents retain direct retrieval.
- Corpus v2 still repeats generic procedures and requests handoff records the
  UI does not capture. Its neutral signal guidance removes oracle contamination
  but does not establish retrieval or report quality.
- No authentication or production authorization. Synthetic identity headers
  are caller-supplied; tenant-scoped storage checks remain required.
- Only `getRecentServiceErrors` is implemented. Evidence sufficiency and broad
  live-model quality are not established by one successful S001 demonstration.
- The latest cleanup npm ci reports nine dependency advisories (three moderate,
  four high, two critical), with unchanged package manifests and lockfile.
  The verification gate has no failing npm-audit step. [QUALITY.md](QUALITY.md)
  defines what the gate actually checks.
- Knowledge preparation is explicit; normal startup does not populate a new
  database. Local database/container state is not guaranteed by repository docs.
- Historical Titan vectors remain auditable and lexically eligible but are not
  compared with local 768-dimensional `nomic-embed-text` query vectors.
- AWS deployment and an optional Bedrock profile are not implemented.
- Investigation workspace incident-context expansion remains deferred.

## Maintenance

Keep only current facts and the latest relevant verification summary here.
Use completed tasks for historical evidence, ADRs for decisions, and the
roadmap for ordered work. Never overwrite a measured failure with a completion
claim.
