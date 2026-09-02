# Product roadmap

Status: Active
Last reviewed: 2026-08-31
Owner: Christopher Guzowski

## Purpose

Preserve the completed payment-incident investigation vertical slice while
increasing approved-knowledge depth and proving the same workflow through live
local embedding and chat models. Each product slice must produce a reviewable,
auditable outcome without adding real data, autonomous authority, or a second
incident family.

This roadmap orders outcomes. It does not replace the active contract in
`tasks/current.md`, and a planned product task does not authorize executable
changes until the owner activates it.

## Completed baseline

```text
synthetic alert
-> tenant-scoped incident work queue
-> operator-started investigation
-> sourced operational evidence
-> approved runbook and policy retrieval
-> structured evidence-linked AI report
-> explicit operator approval or rejection
-> complete audit timeline
```

The owner declared this initial end-to-end vertical slice complete on
2026-08-31. The recorded authoritative gate covers the API, MCP server, Angular
console, PostgreSQL/pgvector integration, lifecycle failures, formatting,
builds, Compose configuration, and diff checks with zero skipped tests. Model
boundaries remain deterministic in automated verification; exercising the
configured live local models moves into the knowledge-expansion phase below.

## Next target path

```text
SynTen Inc profile and corpus contract
-> generated text-based PDF runbooks and policies
-> versioned PDF extraction and source provenance
-> deterministic chunking and PostgreSQL persistence
-> live Ollama embeddings and pgvector indexing
-> measured hybrid retrieval
-> live evidence-linked report and human review
```

All tenant-specific assets for this path live under `SynTen Inc/`.

## Ordered work

| Order | Outcome | Status | Completion boundary |
|---|---|---|---|
| E1 | Establish one authoritative verification entry point | Complete | Local and CI completion use the same full repository checks. |
| P1 | Retrieve approved operational knowledge | Complete for baseline | The deterministic slice retrieves tenant-approved Markdown excerpts with provenance. |
| P2 | Generate a reviewable incident report | Complete for baseline | The deterministic slice persists a schema-valid, evidence-linked report and enters `AWAITING_REVIEW`. |
| P3 | Record the human decision and audit trail | Complete | Approval/rejection is attributable and the full incident timeline remains reviewable. |
| P4 | Prove and harden the initial closed loop | Complete | The repeatable synthetic workflow and its important terminal/failure states pass the authoritative gate. |
| K1 | Define the SynTen Inc PDF knowledge corpus | Complete | The exact tenant profile, document inventory, authoring rules, validation contract, and retrieval-evaluation cases are approved. |
| K2 | Generate and validate the SynTen Inc PDF corpus | Complete | Every inventoried runbook and policy has reproducible source, a valid text-based PDF, matching metadata, and successful render/extraction checks. |
| K3 | Ingest and chunk PDFs with source provenance | Complete | The tested catalog path produces deterministic, versioned chunks traceable to exact PDF locations. |
| K4 | Embed, vectorize, and evaluate the corpus | Complete with factual FAIL | Live `nomic-embed-text` indexed all 705 chunks; the retained evaluation artifact records the exact fixed-threshold misses without tuning the contract. |
| K5 | Prove live approved-knowledge retrieval in the operator workflow | Active | A repeatable synthetic investigation uses live `nomic-embed-text` retrieval, and **Retrieve approved knowledge** displays eligible cited PDF guidance instead of the no-match state. |
| D1 | Select and implement the initial AWS deployment shape | Deferred | The verified closed loop runs through least-privilege AWS infrastructure. |
| D2 | Add authentication and enforce operator identity | Deferred | Identity is authenticated and tenant authorization is enforced at every public boundary. |

## Active product slice

K4 is complete with a retained factual FAIL artifact. K5 is the next planned
slice: improve the approved retrieval path under a separately accepted contract,
then prove through the real operator action that an eligible, cited PDF result
is displayed. K5 uses live `nomic-embed-text`; live chat/report-model selection
is deferred.

The active contract is in `tasks/current.md`. The maintained sources, generated
PDFs, manifest, validation tooling, and retrieval oracle remain under
`SynTen Inc/`.

## Sequencing rules

- Treat the completed K1 inventory and K2 PDF/version metadata as fixed inputs;
  change them only through a separately approved corpus-version task.
- Keep PDF generation reproducible and separately verifiable from ingestion.
- Accept a PDF extraction, locator, and chunking ADR before changing the
  existing Markdown ingestion contract.
- Persist exact source and model metadata; never treat regenerated chunks or
  embeddings as the same version silently.
- Keep automated tests deterministic and network-free. Run live Ollama work as
  an explicit local evaluation with recorded model identifiers and results.
- Keep PostgreSQL/pgvector and exact hybrid retrieval until measurements justify
  a different index or search service.
- Do not add another tenant or incident family during K1-K5.
- Keep an operator decision mandatory; a live model never approves a report or
  executes a recommendation.

## Roadmap review triggers

Revisit the order if the PDF extraction design cannot preserve reliable source
locations, corpus size or measured latency justifies approximate indexing,
retrieval evaluation shows the current RRF/chunking contract is unfit, or live
Ollama quality is inadequate for the selected synthetic scenario.
