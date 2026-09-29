# Task: Complete Q1 evaluation integrity

Status: Completed
Created: 2026-09-24
Owner: Christopher Guzowski

## Goal

Complete Q1 by removing the deterministic answer key from every evaluated
evidence, retrieval, report, and decision input while preserving an auditable
post-decision reveal and the immutable v1 corpus record.

## User story

As an evaluator, I want observable scenario inputs and operational knowledge to
be independent of the sealed oracle, so retrieval and report quality are not
measured against answers copied into their own inputs.

## Chosen contract

- Split the scenario catalog into an observable catalog and a separately loaded
  `scenario-oracle/v1`; only the post-decision reveal service may depend on the
  oracle catalog.
- Archive the exact v1 corpus sources, PDFs, manifest, inventory, authoring
  standard, and generator/validation implementation before creating v2.
- Create `synten-auth-knowledge/v2` with bumped document versions and neutral,
  independently maintained signal interpretations; no corpus tool or artifact
  may read or reproduce oracle root-cause, disposition, confidence, required
  evidence, recommendation, or decision-rule fields.
- Preserve document IDs, keys, approval states, coverage, and retrieval labels
  unless a measured requirement demands a separate owner decision.
- Keep `synten-retrieval-eval/v1` labels fixed while binding them explicitly to
  corpus v2.

## In scope

- Observable/oracle scenario resource split and dependency tests.
- Immutable v1 corpus archive with byte/hash verification.
- Version-bumped v2 Markdown sources, PDFs, validation manifest, and ingestion
  contract.
- Corpus generation, extraction, rendering, automated validation, and visual
  inspection of every regenerated PDF page.
- Retrieval-evaluation contract and runner corpus-version updates.
- Documentation, ADR, status, and full repository verification.

## Out of scope

- Changing retrieval ranking, labels, thresholds, or query construction.
- Running a new live embedding/retrieval benchmark or claiming Q2 success.
- Automated report grading, live report generation, authentication, or AWS.
- Persisting reveal events in the copilot audit timeline.

## Constraints

- Preserve all v1 bytes and recorded hashes.
- Use only synthetic data and keep PDFs at 1-15 pages.
- Never change document bytes without changing their version and filename.
- Evidence and retrieval paths must compile without an oracle dependency.
- Automated tests remain deterministic and require no live model provider.

## Acceptance criteria

- [x] Observable scenario resources contain no answer-key fields, and runtime
      evidence/generation plus retrieval evaluation consume only those resources.
- [x] Only the terminal-decision reveal service can load `scenario-oracle/v1`.
- [x] The exact v1 corpus remains inspectable and hash-verifiable in its archive.
- [x] Corpus v2 contains exactly 30 source/PDF pairs with bumped versions and no
      oracle-derived prose or generator dependency on oracle fields.
- [x] All v2 PDFs pass automated validation and complete visual inspection.
- [x] Catalog ingestion and retrieval evaluation accept v2 while preserving
      document keys, IDs, eligibility, labels, and thresholds.
- [x] Focused tests and the full repository verification gate pass without skips.

## Test plan

- Red tests for observable/oracle resource schemas and package dependencies.
- Static contamination tests that reject oracle fields or phrases in the v2
  corpus generator, sources, PDFs, and manifest.
- Archive tests comparing v1 manifest hashes to archived source/PDF bytes.
- Existing corpus validation, parser/chunker, catalog, and evaluation-contract
  suites updated for v2.
- Render every PDF page, inspect contact sheets plus required full-size pages,
  and record page counts and defects.
- Run focused generator/corpus/backend tests followed by `./verify.ps1`.

## Progress notes

- 2026-09-24: Owner authorized completion of Q1 after the terminal-decision
  reveal slice passed the full repository gate.
- 2026-09-24: Inspected the remaining contamination path: the corpus generator
  reads `truth.rootCause` for signal tables and scenario decision matrices, and
  retrieval evaluation currently deserializes truth with observable scenarios.
- 2026-09-24: Split the 36-scenario runtime catalog from
  `scenario-oracle/v1`; generation, evidence, and retrieval now deserialize only
  observable fields, while a dependency test limits oracle loading to the
  terminal human-decision reveal path.
- 2026-09-24: Preserved the complete v1 corpus under
  `corpus/versions/synten-auth-knowledge-v1/` and verified every archived source
  and PDF byte against the archived manifest hashes.
- 2026-09-24: Generated `synten-auth-knowledge/v2` as 30 bumped-version Markdown
  and PDF pairs. Automated validation found 113 pages (3-5 per document), and
  visual inspection covered all 113 rendered pages with no defects.
- 2026-09-29: Bound ingestion and fixed `synten-retrieval-eval/v1` labels to
  corpus v2 and accepted its deterministic 30-document, 705-chunk catalog
  fingerprint `5d704fee24f9754176f1be2e449050190e0b78ffa3fdb9ddc92b464006d198e9`.
- 2026-09-29: The full repository verification gate passed after Docker-backed
  integration execution, with zero skipped tests.

## Completion evidence

- Corpus validation: 8 Python tests passed; source/PDF hashes, archive hashes,
  metadata, page limits, exact artifact counts, and oracle-contamination checks
  passed for all 30 active documents.
- PDF validation and visual QA: 30 PDFs, 113 pages, 3-5 pages per PDF; every page
  rendered and inspected, with zero clipping, overlap, unreadable-table, banner,
  or pagination defects.
- Focused generator verification: 31 tests passed with zero failures, errors, or
  skips.
- Focused catalog/retrieval verification confirmed 30 documents, 705 chunks, and
  the accepted corpus-v2 fingerprint.
- `./verify.ps1` passed on 2026-09-29: 290 copilot API, 9 operations MCP, 31
  generator, and 78 Angular tests passed with zero failures, errors, or skips;
  format, package, build, Compose, and repository diff checks also passed.

## Remaining limitations

- The retrieval benchmark was not rerun; Q1 establishes input integrity, not Q2
  retrieval or report quality.
- Corpus v2 still contains generic procedural overlap and requests some handoff
  records the operator console does not capture.
- Answer-key reveal events are emitted to the generator audit sink but are not
  persisted in the copilot audit timeline.
- Frontend installation reported six dependency advisories (five moderate, one
  high); the repository gate has no failing npm-audit step.

## Decisions needed

None.
