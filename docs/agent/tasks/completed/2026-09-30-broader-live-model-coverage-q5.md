# Task: Broader live-model coverage (Q5)

Status: Complete — all 36 live reports and reproducible grades retained; quality gaps documented
Created: 2026-09-30
Owner: Christopher Guzowski

## Goal

Measure live report generation across the complete 36-scenario observable catalog,
including common, uncommon, rare, partial-evidence and unavailable-evidence cases.

## User story

As an operator, I want broad, reproducible live-model measurements so that I can
assess report availability, bounded correctness, citations, unsupported-claim
indicators and latency beyond a single demonstration.

## Chosen contract

Run one fresh investigation and one live report attempt per observable scenario
through existing tenant-scoped product HTTP boundaries. Use the dedicated
`payment_copilot_k4_eval_q2_v2` database, whose catalog contains only the 30
corpus-v2 PDF versions and 705 existing embeddings. Keep the existing model,
prompt, schema, retrieval ranking and evaluation contracts fixed. Load the sealed
oracle only in offline post-generation grading with `synten-report-eval/v1`.
No quality promotion threshold is introduced; measured failures remain results.

## In scope

Preflight, 36 fresh alerts/investigations, evidence collection, retrieval, live
report attempts, persisted readback, retained hash-bound input and grading
artifacts, coverage analysis, factual documentation and static verification.

## Out of scope

Model/retrieval/corpus tuning, executable application changes, automated human
decisions, answer-key reveal in runtime, new grading semantics or thresholds,
deployment and authentication.

## Constraints

Synthetic data only. Preserve every attempt and missing/unavailable evidence.
Keep oracle content outside generation inputs. Use exact persisted provenance
and eligible identifiers rather than invented hashes or output. Leave generated
reports for human review. Do not modify corpus, oracle or evaluator bytes.

## Acceptance criteria

- [x] Preflight verifies healthy API, generator and Ollama, exact corpus-v2
      source membership, embeddings, and fixed model/configuration provenance.
- [x] All 36 catalog scenarios have fresh tenant-scoped investigations,
      persisted evidence/retrieval attempts and one terminal live report attempt.
- [x] Coverage includes COMMON, UNCOMMON, RARE, PARTIAL and UNAVAILABLE evidence;
      actual evidence statuses are checked against observable scenario definitions.
- [x] Retained input contains exact report metadata, timestamps, hashes, eligible
      citations and structured reports or explicit terminal failures for all cases.
- [x] Unchanged offline Q3 grader executes after generation and retains reproducible
      aggregate/per-scenario results, hashes and explicit bounded-metric limitations.
- [x] Factual coverage, quality, citation, latency and failure findings are documented;
      Repository verification and diff checks pass.

## Test plan

Manual live HTTP/persistence checks against existing behavior; no production
behavior changes or artificial new tests. Verify source hashes against corpus-v2
manifest, evidence status against catalog, exact persisted report/source binding,
terminal outcomes and 36 unique scenario/attempt identities. Regrade retained
input and compare evaluator results. Run the existing focused offline evaluator
tests, `./verify.ps1 -Scope Repository` and `git diff --check`.

## Progress notes

- Owner activated Q5 with `proceed with Q5`; archived completed Q4 unchanged.
- Working tree was clean before work began.
- Default API, generator and Ollama are healthy. Dedicated evaluation database
  contains 27 approved and 3 superseded PDF versions, 705/705 embedded chunks,
  and Flyway V10. No Markdown sources are present.

- Live run `1d9996ed-eeae-4295-a925-6d46709bb943` started at
  2026-09-30T14:22:24.8689323Z on the isolated port-8083 API.
- Exact source/PDF hashes and approval states matched all 30 manifest versions.
  Jar SHA-256: `b5def6cf1015d473700b9c6ba457d658d412fe8c00408c67f3925235d0bbe53f`.
- Q3's eight focused evaluator tests and Repository scope passed during execution;
  final Repository scope and diff checks also passed after documentation updates.
- S001 succeeded in 101.2 seconds. The POST response's request timestamp includes
  nanoseconds while PostgreSQL readback stores microseconds; grading uses the
  persisted timestamp. Resume reused the same attempt, without another model call.
- Existing provenance discrepancy: `SpringAiReportModel` sets maxTokens=1536,
  but `ReportGenerationAttempt.started` records max_output_tokens=4096.
  `javap -c -p` verified both constants in the exact running jar. Preserve the
  recorded value and report this limitation; no executable behavior was changed.

## Completion evidence

- All 36 scenarios completed: 14 COMMON, 11 UNCOMMON, 11 RARE; evidence was
  34 AVAILABLE, S111 PARTIAL and S211 UNAVAILABLE, matching the observable catalog.
- Exactly 36 fresh incidents/investigations, 36 evidence attempts, 36 retrieval
  attempts, 36 report attempts, 180 ordered audit events and zero human decisions
  were verified through tenant-scoped histories. All reports are AVAILABLE and
  incidents remain AWAITING_REVIEW. Full snapshot/content/actor/source binding
  checks passed; no generation retry was submitted.
- Execution ran 16:22:24–17:19:40 Europe/Warsaw on 2026-09-30;
  final history verification completed at 17:20:36.
- The unchanged offline Q3 module graded the complete retained input after
  generation. A second invocation produced a byte-identical grade artifact.
- Grading measured 34/36 exact dispositions, 9/36 exact confidence levels,
  26/36 required-signal coverage (24/34 signal-bearing cases), 374/374 valid
  citation references and 241/241 required claim citations. Both insufficient-
  evidence cases asserted cause/recommendation: 0/2 null-contract passes and
  four bounded unsupported-claim indicators. These are measured quality gaps.
- Report latency: min 75.096 s, median 90.764 s, p95 109.187 s, max 113.636 s;
  zero report UNAVAILABLE/TIMED_OUT/MALFORMED outcomes.
- Retained observations SHA-256:
  `0d2db90e45f7fd542d4d2678b15ffa3571d1d593a519df507278c690566eb25f`.
  Input SHA-256:
  `09bd6895a26d75b4535832a2c11dbdb6ba26d1855c4adf81aa67c77464c6a320`.
  Grade/regrade SHA-256:
  `efb290272dfb073e701ef75e495fd91f446ffe188eef4e9b4c56e9196b7a14fb`.
- All 60 corpus-v2 source/PDF files matched the manifest hashes.
- Full metrics, reproduction, exact artifacts and limitations are retained in
  [Q5 results](../../../SynTen%20Inc/evaluation/q5-live-results.md).

- ./verify.ps1 -Scope Repository passed after result documentation on 2026-09-30;
  git diff --check passed. No production behavior changed, so the documentation
  gate applies. Eight focused evaluator tests passed; complete regrade matched bytes.

## Remaining limitations

Bounded Q3 checks cannot establish general natural-language entailment. No
quality promotion threshold or human approval is part of this measurement.
S111/S211 failed the insufficient-evidence disposition/confidence/null contract.
Attempt metadata records 4096 output tokens while the adapter sets 1536.
The CLI wrapper failed under PowerShell 7.6.5 with `createdAt must be non-blank`
because default JSON parsing converts timestamp strings to DateTime. Unchanged
module invocation with `ConvertFrom-Json -DateKind String` succeeded and
reproduced all grade bytes. Existing tests do not execute that CLI input path.
These defects remain outside Q5's executable-change scope.

## Decisions needed

None.
