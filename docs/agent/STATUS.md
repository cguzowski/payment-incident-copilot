# Project status

Last updated: 2026-09-29

## Current state

The owner closed the core MVP/local demo loop on 2026-09-24. The application
implements synthetic intake, active/completed incident queues, investigations,
MCP evidence, approved knowledge, advisory reports, human decisions, and audit
history. Live S001 evidence, retrieval, and Qwen report generation were recorded;
a complete terminal decision on a newly generated live-model report remains
unproved.

The [active task](tasks/current.md) is Q2 retrieval-quality disposition. It will
run a fresh, inspectable corpus-v2 baseline against the unchanged benchmark,
diagnose exact misses, and either pass the fixed thresholds through
generalizable retrieval improvements or require explicit owner acceptance of
the measured failure. U3 reviewer links are complete: resolvable identifiers
open exact rendered records or immutable cited PDFs, while unresolved
identifiers remain plain text.

## Verification evidence

- Q2 fresh corpus-v2 baseline run `d22280e533b04578bdd4f260e0f6a6c6`
  seeded and verified all 37 variants, with zero ineligible candidates and all
  special semantics preserved. It factually failed at 19/22 primary runbooks,
  12/20 required policies, and 17/21 primary-over-weak cases. Its retained
  2,300,817-byte artifact SHA-256 is
  `c126a45551ebeaf8774e5731e4dbe6f8711d4e3d3d3e6bcdd2620ff75974cce1`.
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

- The [retrieval benchmark](../../SynTen%20Inc/README.md) still fails all three
  quality thresholds. The fresh Q2 corpus-v2 artifact is retained, while the two
  historical K4/K5 raw artifacts remain absent. Do not present the new run as
  either historical artifact.
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
