# Project status

Last updated: 2026-09-24

## Current state

The owner closed the core MVP/local demo loop on 2026-09-24. The application
implements synthetic intake, active/completed incident queues, investigations,
MCP evidence, approved knowledge, advisory reports, human decisions, and audit
history. Live S001 evidence, retrieval, and Qwen report generation were recorded;
a complete terminal decision on a newly generated live-model report remains
unproved.

The [latest task](tasks/current.md) completed the repository Markdown cleanup.
[ROADMAP.md](ROADMAP.md) owns future sequencing; Q1 evaluation integrity remains
the next product outcome and has no activated implementation contract.

## Verification evidence

- The [R2 completion record](tasks/completed/2026-09-24-generate-live-local-reports-with-qwen3-8b.md)
  records the 2026-09-24 full gate: 290 copilot API, 9 operations MCP, 17
  generator, and 78 Angular tests, with zero failures, errors, or skips, plus
  formatting, builds, Compose, and repository checks. These are historical
  results, not a claim that this documentation change reran those suites.
- R2 records an AVAILABLE/PROPOSED S001 report in about 76 seconds using
  `qwen3:8b-q4_K_M`, `report-prompt/v3`, and `report-v1`, entering
  `AWAITING_REVIEW`. The current code and ADR-0012 cap output at 1,536 tokens;
  the archived task retains its original 4,096-token wording and reconciliation.
- Documentation-cleanup checks are recorded in the current task.

## Known limitations

- The [recorded retrieval benchmark](../../SynTen%20Inc/README.md) fails all
  three quality thresholds. Its two referenced raw artifacts are absent from
  this checkout and are not tracked; historical task records retain names,
  hashes, and reported counts. Recover exact artifacts before independent
  result review; do not present a new run as the original.
- Evaluation integrity is not established. The generator returns the answer
  key to the browser before review, and corpus generation copies scenario
  `truth.rootCause` into retrievable runbooks. Q1 must address both paths.
- The fixed v1 corpus repeats generic procedures and requests handoff records
  the UI does not capture. Corpus changes need explicit versioning, regeneration,
  and evaluation; documentation cleanup does not change those hashed inputs.
- No authentication or production authorization. Synthetic identity headers
  are caller-supplied; tenant-scoped storage checks remain required.
- Only `getRecentServiceErrors` is implemented. Evidence sufficiency and broad
  live-model quality are not established by one successful S001 demonstration.
- The R2 install recorded three dependency advisories (two moderate, one high).
  They were not rechecked here; the verification gate has no failing npm-audit
  step. [QUALITY.md](QUALITY.md) defines what the gate actually checks.
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
