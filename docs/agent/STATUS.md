# Project status

Last updated: 2026-09-29

## Current state

The owner closed the core MVP/local demo loop on 2026-09-24. The application
implements synthetic intake, active/completed incident queues, investigations,
MCP evidence, approved knowledge, advisory reports, human decisions, and audit
history. Live S001 evidence, retrieval, and Qwen report generation were recorded;
a complete terminal decision on a newly generated live-model report remains
unproved.

The [latest task](tasks/current.md) completed U1 report-generation state
recovery. The operator console consumes exactly one generation response,
releases obsolete upstream work immediately, clears its loading presentation
for every terminal status and HTTP error, and distinguishes an active wait
cursor from a workflow-disabled control.

## Verification evidence

- U1 passed `./verify.ps1` on 2026-09-29: 290 copilot API, 9 operations MCP, 31
  generator, and 79 Angular tests passed with zero failures, errors, or skips;
  formatting, builds, Compose validation, and repository checks also passed.
- The focused U1 report-panel suite passed 13 tests covering non-completing
  terminal responses, upstream unsubscription, destroy cancellation, HTTP
  failures, busy semantics, and computed cursor behavior.
- Q1 corpus checks passed for the exact hash-verifiable v1 archive and 30 active
  v2 source/PDF pairs. Automated validation covered 113 pages, and visual QA
  inspected every rendered page with zero layout defects.
- R2 records an AVAILABLE/PROPOSED S001 report in about 76 seconds using
  `qwen3:8b-q4_K_M`, `report-prompt/v3`, and `report-v1`, entering
  `AWAITING_REVIEW`. The current code and ADR-0012 cap output at 1,536 tokens;
  the archived task retains its original 4,096-token wording and reconciliation.

## Known limitations

- The [recorded retrieval benchmark](../../SynTen%20Inc/README.md) fails all
  three quality thresholds. Its two referenced raw artifacts are absent from
  this checkout and are not tracked; historical task records retain names,
  hashes, and reported counts. Recover exact artifacts before independent
  result review; do not present a new run as the original.
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
