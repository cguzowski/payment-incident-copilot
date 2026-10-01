# Project status

Last updated: 2026-10-01

## Current state

The local demo is complete and the owner has paused feature work. Seven
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
The [thorough cleanup](tasks/current.md) retires all 13 Python authoring scripts
into a [hash-verified historical archive](../../SynTen%20Inc/history/pdf-authoring-tools/README.md)
and removes an unused report helper and ORM dependency. Frozen assets are
unchanged. The prepared JDBC-only API package is smaller, but Docker-backed
post-change verification was rejected by the permission prompt and awaits owner
authorization. The running demo remains on its previous tested API JAR.
Feature expansion and AWS deployment remain deferred;
see the [roadmap](ROADMAP.md).

## Verification and retained evidence

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
