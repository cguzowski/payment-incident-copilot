# Project status

Last updated: 2026-10-01

## Current state

The owner closed the core MVP/local demo loop on 2026-09-24. The application
implements synthetic intake, active/completed incident queues, investigations,
MCP evidence, approved knowledge, advisory reports, human decisions, and audit
history. Live S001 evidence, retrieval, and Qwen report generation were recorded;
Q4 now proves a complete terminal human rejection on a newly generated live
Qwen report.

The [current task](tasks/current.md) completes expansion E2. The
[independent payment library](../../SynTen%20Inc/payment-knowledge/v1/README.md)
is frozen as synten-payment-knowledge/v1: 11 runbooks and 5 policies, 16 PDFs / 48
pages, each three pages. All 24 domain risks have editorial coverage. All rendered
pages passed visual review; metadata/substantive text extraction and byte-identical
rebuilds passed. Exact source/PDF/tool/font/domain-input hashes are retained. The
historical 30-PDF baseline remains separate and unchanged; no new incident fixtures,
oracle, labels or report outputs were authoring inputs. Full repository verification
passed after starting Docker to prevent skipped database tests. E3 is next: accepted
catalogs, explicit preparation and PDF-only/shared-policy retrieval. This asset task
does not import the PDFs or add item-level evidence to the application.

The completed [evidence availability task](tasks/completed/2026-10-01-available-demo-evidence.md) prevents accidental local evidence
unavailability. The latest reported failure was S312, whose provider deliberately
returned UNAVAILABLE, rather than a startup failure. Under owner-approved
ADR-0019, normal generation now selects only AVAILABLE scenarios across all
seven families, preserving rarity weights and explicit degraded fixtures.
Startup selects the correct provider and verifies the effective API MCP endpoint
before reuse. Mismatched or unverifiable APIs receive an actionable restart error.
Full verification passed: 339 API, 9 MCP, 52 generator and 103 console tests,
with zero failures/errors/skips, formatting, builds and repository checks.
Live startup and a fresh S014 investigation returned AVAILABLE with two sourced
observations; the original S312 failure remains unchanged. API, generator and
console are left running locally.

Completed E1's
[successor payment domain](../../SynTen%20Inc/domain/v2/README.md) defines conceptual
lifecycle transitions, synthetic amount/timing rules, source authority, 24 independent
risks and 11 evidence requirements. Only aggregate service errors are currently
exposed through MCP; payment/event/refund records and PDF-only retrieval remain
future work. The independent library is frozen before new incident creation.
E1 changes documentation only and preserves Q6, seven-family behavior and historical
corpus/scenario/evaluation inputs.

The completed [retrieval recovery](tasks/completed/2026-10-01-restore-additional-family-knowledge.md)
restored guidance for additional families. The configured local database originally had
only the 30 authorization-decline PDF documents / 705 chunks. Webhook and
reconciliation investigations returned NO_MATCH because their approved sources
had not been imported. Explicit Markdown preparation added fourteen documents /
87 embedded chunks, including two documents and thirteen chunks for each new
family. All use nomic-embed-text with 768 dimensions; the original PDFs and
package hashes are unchanged.

Live retries of both affected investigations return AVAILABLE with seven
family-matching approved runbook/policy chunks, retained version/line/model/evidence
provenance, and all prior NO_MATCH attempts preserved. Both remain INVESTIGATING
with zero decisions. API and generator are now running and healthy. Repository
verification passed; no production behavior or ranking changes were needed.

The completed six-family expansion passed full verification on 2026-10-01:
337 API, 9 MCP, 50 generator and 103 Angular tests, zero failures/errors/skips,
plus formatting, builds, Compose and repository checks. Launcher -PrepareKnowledge
imports the guidance; normal startup remains explicit about using a prepared
index. [Package and preparation](../../SynTen%20Inc/multi-incidents/v1/README.md)
and ADR-0018 describe the seven-family workflow. New-family live report quality
has not been measured.

Q6 insufficient-evidence report safety remains complete. All 48 focused report
tests passed; report-prompt/v5 requires insufficient-evidence/LOW/null reports
for degraded or empty observations. Fresh S111/S211 live checks both passed,
with ten audit events and zero decisions. [Q6 results](../../SynTen%20Inc/evaluation/q6-live-results.md)
retain the fresh attempts and an explicitly mixed diagnostic with 34 Q5 results.

Q5 broader live-model coverage is complete.
Live execution, reproducible grading and the documentation verification gate passed.
All 36 corpus-v2 scenarios produced AVAILABLE reports, with 34 AVAILABLE,
1 PARTIAL and 1 UNAVAILABLE evidence result. Reports remain AWAITING_REVIEW
with zero human decisions. Q4's live S012 rejection and audit proof remain complete.

Live grading measured 34/36 exact dispositions, 9/36 exact confidence levels,
26/36 exact-signal coverage checks and 374/374 valid citation identifiers.
S111/S211 both asserted a cause and recommendation despite insufficient evidence:
0/2 null-contract passes and four bounded unsupported-claim indicators.
Median report latency was 90.764 seconds, p95 109.187 seconds; no terminal model
failures occurred. These are measured quality gaps, not a promotion judgment.
[Retained Q5 results](../../SynTen%20Inc/evaluation/q5-live-results.md) contain
all inputs, grades, observations, provenance and reproduction steps.

Live evidence recovery is complete.
Neither evidence provider was listening when collection returned UNAVAILABLE.
Starting the generator and restarting the API explicitly against port 8082
restored AVAILABLE evidence for S207, with two sourced error observations.
The original API endpoint/session state was not established; no executable
behavior changed, and historical failed attempts remain retained.

Duplicate-citation report validity is complete. Two live S013 attempts safely failed as `MALFORMED` because Qwen
repeated the sole eligible evidence identifier despite the schema's
`uniqueItems` constraint. `report-prompt/v4` now states the uniqueness rule and
the per-request schema caps reference-array lengths at the count of distinct
eligible identifiers; strict parsing and fail-closed behavior are unchanged.
The same investigation then produced an AVAILABLE/PROPOSED report with unique
eligible citations after the API restart.

Q3 automated report grading is complete.
`synten-report-eval/v1` now grades complete 36-scenario post-run artifacts
against the sealed oracle with exact bounded correctness, citation,
unsupported-claim, latency, and terminal-failure metrics. The evaluator is
offline, fail-closed, hash-bound, and outside every runtime/model input. Its
retained deterministic fixture proves grader behavior; it is not a live-model
quality result. Q2 remains complete: the unchanged retrieval benchmark passes
with exact-signal ranking and bounded, source-derived policy relationships.
U3 reviewer links are complete: resolvable identifiers
open exact rendered records or immutable cited PDFs, while unresolved
identifiers remain plain text.

## Verification evidence

- Combined worktree integration passed ./verify.ps1 on 2026-10-01: 339 API,
  9 MCP, 52 generator and 103 Angular tests, zero failures/errors/skips; script,
  formatting, build, Compose and diff checks passed. Eight PDF tooling tests
  passed and the frozen library hashes/provenance remain unchanged. Original
  planning history and the completed evidence-availability task are retained.

- Evidence availability on 2026-10-01: persisted investigation
  8245ff66-ce77-4ceb-8567-9f44fd1a07cf has scenario S312 and status detail
  `Synthetic observation source unavailable.` Confirmed selector regressions
  red (PARTIAL selected; unavailable-only rarity accepted), then green across
  every eligible candidate and all seven families. Two actuator-info tests and
  PowerShell provider-selection/reuse/failure tests passed. ./verify.ps1 passed
  503 Java/Angular tests plus all script checks. Actual Windows PowerShell
  preflight and startup passed using local ignored configuration. Live generated
  incident f0a43589-2864-40ec-9c0c-ff72bdcb1da3 / investigation
  da62c985-a5c2-44c3-ac13-1d8b0bec8a24 (S014) returned AVAILABLE with two
  payment-authorization observations and API provider http://localhost:8082.
  The original unavailable attempt was read back unchanged. No reports or human
  decisions were generated by this verification.

- E2 passed ./verify.ps1 on 2026-10-01: 337 API, 9 MCP, 50 generator and
  103 Angular tests with no failures/errors/skips; builds/format/Compose checks passed.
  Eight focused PDF tooling tests passed. All 16 PDFs / 48 pages passed extraction,
  visual/editorial review and two byte-identical rebuilds matching viewed PDF bytes.
  Freeze manifest SHA-256: a1fe13333de61f51a9406827a577f880ca7d88b1bcd2e0a4def438f6b134b421.
  Initial gate correctly rejected skipped database tests while Docker was stopped;
  the repeated full gate passed after Docker startup. Runtime adoption is unperformed.

- Expansion E1 passed ./verify.ps1 -Scope Repository on 2026-10-01. Changed/new
  Markdown links, 24 unique risk IDs, 11 evidence IDs and cross-references passed
  static review; lifecycle/source/uncertainty tables passed editorial review.
  Historical sources, PDFs, manifests, scenario/oracle/evaluation and runtime
  inputs have no diff against aa4ccef. Completed recovery archive matches exactly.
  No new live-model, PDF or runtime capability is claimed.

- Local retrieval recovery on 2026-10-01 imported 14 Markdown documents / 87
  embeddings, preserving 30 PDF documents / 705 embeddings. Webhook retry
  6febe78b-4c46-4b81-b1ed-ea920987f7dc and reconciliation retry
  8046adbf-3a00-4673-a602-b96fca3e6d2b are AVAILABLE with seven family-matching
  chunks each. Their two/one earlier NO_MATCH attempts are retained. Exact
  approved version 1.0.0 line provenance, source hashes, 768-dimensional nomic
  metadata and evidence bindings were verified; both services are UP.
  ./verify.ps1 -Scope Repository and diff checks passed.

- ADR-0018 expansion passed ./verify.ps1 on 2026-10-01: 337 API, 9 operations
  MCP, 50 generator and 103 Angular tests, zero failures/errors/skips. All twelve
  new complete/degraded workflows preserve exact family/source bindings through
  human decision and audit. Fourteen added asset hashes match; original corpus,
  scenarios, oracle, evaluations and legacy guidance have no diff. API runtime
  packaging contains no oracle. Launcher preparation tests cover the third
  explicit Markdown import step. npm ci reports eight advisories with unchanged
  lockfile; npm-audit is not a failing gate step.

- Q6 passed ./verify.ps1 on 2026-09-30: 317 API, 9 operations MCP, 31 generator
  and 91 Angular tests with zero failures/errors/skips, formatting, builds,
  Compose validation and repository checks. All 48 focused report tests passed.
  Fresh S111/S211 first attempts passed INSUFFICIENT_EVIDENCE/LOW/null/gap checks
  in 64.080393/44.985532 seconds, with ten audit events and zero human decisions.
  Mixed diagnostic regrade was byte-identical:
  `30dfb8aa354c5e5cb53e11bd9882131ad764081402993a2948300ae34bca09e9`.
  All 60 corpus file hashes and Q5 artifact hashes matched; 34 reused Q5 result
  objects were unchanged. The diagnostic is not a new complete v5 live benchmark.

- Q5 passed `./verify.ps1 -Scope Repository`, its eight focused offline evaluator
  tests and diff checks on 2026-09-30. Live histories verified 36 unique cases,
  exact report/evidence/retrieval binding, 180 chronological audit events,
  unchanged report content and zero decisions. All 60 corpus-v2 source/PDF hashes
  matched the manifest. Regrade was byte-identical:
  `efb290272dfb073e701ef75e495fd91f446ffe188eef4e9b4c56e9196b7a14fb`.
  Exact artifacts and interpretation are linked in the Q5 results above.

- Q4 passed `./verify.ps1 -Scope Repository` and diff checks on 2026-09-30.
  Live report `64b09be2-1997-438d-a47c-272899e71736` completed in 96.1 seconds;
  human decision `44bc1c2c-2826-47b8-8573-dff5941d8023` recorded REJECTED
  with reason `Testing`. Exact report JSON matched its pre-decision SHA-256
  `64d3e183d0361e3acc5bad7f041ef2da2e70cbf46710a13c04b419063db05669`.

- Q3 passed `./verify.ps1` on 2026-09-30: 303 copilot API, 9 operations MCP,
  31 generator, and 91 Angular tests passed with zero failures/errors/skips.
  Formatting, builds, Compose validation and repository checks passed. The
  focused eight-test evaluator suite and separate Repository scope also passed.
- Q3 retained `q3-report-grader-fixture-v1.json` (122,566 bytes), SHA-256
  `a023fa81eac34732a619cd76850db936dac2f48358fa55afa9263a1846e93555`.
  It covers all 36 scenarios and deliberately exercises every terminal status
  plus all five bounded unsupported-claim indicators. Recalculated evaluator,
  catalog, oracle, and artifact hashes match its recorded provenance.
- The duplicate-citation regression initially failed because one eligible
  evidence ID retained the base `maxItems: 10`. After the v4 change, 35 focused
  report tests passed with zero failures/errors/skips, including one-source,
  multi-source, base-limit, and strict duplicate-rejection coverage.
- Live S013 attempt `721d1cd4-6adb-4191-adf1-69723370872c` completed AVAILABLE
  in 94.9 seconds with `report-prompt/v4`, `report-v1`, unique eligible
  citations, and retained 64-character prompt and constrained-schema hashes.
- The duplicate-citation fix passed Backend scope and authoritative
  `./verify.ps1`: 304 copilot API, 9 operations MCP, 31 generator, and 91
  Angular tests passed with zero failures/errors/skips. Formatting, builds,
  Compose validation, repository checks, and diff checks passed.
- Q2 passed `./verify.ps1` on 2026-09-30: 303 copilot API, 9 operations MCP,
  31 generator, and 91 Angular tests passed with zero failures/errors/skips.
  Formatting, builds, Compose validation and repository checks passed. The
  separate Backend and Repository scopes also passed.
- Q2 retained live run `1f66fee3cf194f268287a314651ba13f` passed all fixed
  thresholds: 22/22 primary runbooks, 20/22 applicable policy cases (20 required),
  and 20/21 ordering cases (19 required). All 37 variants ran with zero
  ineligible candidates and preserved special semantics. Artifact SHA-256:
  `7d904e6ec557acca8f79cb30a3eb09dc56c838c7e6739f5d6aad2a0509ae64bf`.
  [Retained results](../../SynTen%20Inc/evaluation/q2-retrieval-results.md)
  include the new baseline, isolated signal experiment and passing run.
- Q2 focused knowledge/catalog/retrieval/evaluator suite passed 136 tests with
  zero failures/errors/skips. Explicit catalog compatibility validation confirmed
  705 same-model vectors; relationship reimport added no documents or chunks.
- Q2 fresh corpus-v2 baseline run `d22280e533b04578bdd4f260e0f6a6c6`
  seeded and verified all 37 variants, with zero ineligible candidates and all
  special semantics preserved. It factually failed at 19/22 primary runbooks,
  12/20 required policies, and 17/21 primary-over-weak cases. These are recorded
  results; its 2,300,817-byte artifact is absent from this checkout. Its recorded
  SHA-256 is
  `c126a45551ebeaf8774e5731e4dbe6f8711d4e3d3d3e6bcdd2620ff75974cce1`.
- On 2026-09-29 the owner reported a replay using 705 existing
  `nomic-embed-text` embeddings that reproduced those aggregates, zero
  ineligible candidates and all special semantics; the expected nonzero exit
  confirmed benchmark failure. The owner also reported 32 focused retrieval
  tests passing with zero failures/errors/skips, seven runner tests passing,
  and all 37 plan variants resolving. The replay artifact was removed after
  extracting results and has no retained hash recorded here.
- U3 passed `./verify.ps1` on 2026-09-29: 294 copilot API, 9 operations MCP, 31
  generator, and 91 Angular tests passed with zero failures, errors, or skips;
  formatting, builds, Compose validation, and repository checks also passed.
  Populated browser QA confirmed 33 safe, accessible source links, exact async
  fragment scrolling, matching PDF response bytes, and no overflow or clipped
  source links at 390×844 CSS pixels.
- U2 passed `./verify.ps1` on 2026-09-29: 290 copilot API, 9 operations MCP, 31
  generator, and 86 Angular tests passed with zero failures, errors, or skips;
  formatting, builds, Compose validation, and repository checks also passed.
- The focused U2 panel suite passed 32 tests after an intentional red run failed
  the four missing section disclosures. Live browser QA confirmed native
  keyboard toggling, independent source state, visible focus, and no horizontal
  overflow at 390 CSS pixels.
- Q1 corpus checks passed for the exact hash-verifiable v1 archive and 30 active
  v2 source/PDF pairs. Automated validation covered 113 pages, and visual QA
  inspected every rendered page with zero layout defects.
- R2 records an AVAILABLE/PROPOSED S001 report in about 76 seconds using
  `qwen3:8b-q4_K_M`, `report-prompt/v3`, and `report-v1`, entering
  `AWAITING_REVIEW`. The current code and ADR-0012 cap output at 1,536 tokens;
  the archived task retains its original 4,096-token wording and reconciliation.

## Known limitations

- Q5 reports for partial S111 and unavailable S211 assert causes/recommendations
  at MEDIUM confidence instead of the required insufficient-evidence/LOW posture.
  These historical reports remain immutable. Q6's fresh attempts pass the corrected
  contract. Valid source IDs and schema do not establish supported conclusions;
  all PARTIAL evidence is now conservatively treated as insufficient, and a full
  new 36-scenario v5 live run has not been performed.
- Report attempt metadata records 4,096 output tokens although the running adapter
  sets 1,536; exact jar bytecode and persisted values confirm the discrepancy.
- The report-grading CLI input path fails under PowerShell 7.6.5 because default
  JSON date coercion violates its string timestamp contract. Direct invocation of
  the unchanged module with `ConvertFrom-Json -DateKind String` reproduces Q5
  byte-for-byte. Existing tests do not execute that CLI input path.

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
- The Q1 install recorded six dependency advisories (five moderate, one high).
  Q6 npm ci reported seven (four moderate, three high); the 2026-10-01 expansion gate reports eight
  (four moderate, four high), with unchanged lockfile.
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
