# Project status

Last updated: 2026-09-30

## Current state

The owner closed the core MVP/local demo loop on 2026-09-24. The application
implements synthetic intake, active/completed incident queues, investigations,
MCP evidence, approved knowledge, advisory reports, human decisions, and audit
history. Live S001 evidence, retrieval, and Qwen report generation were recorded;
a complete terminal decision on a newly generated live-model report remains
unproved.

The [current task](tasks/current.md), Q2 retrieval-quality disposition, is
complete. The unchanged benchmark passes with exact-signal ranking and bounded,
source-derived policy relationships, and the full repository gate passed.
U3 reviewer links are complete: resolvable identifiers
open exact rendered records or immutable cited PDFs, while unresolved
identifiers remain plain text.

## Verification evidence

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
