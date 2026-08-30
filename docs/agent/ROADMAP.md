# Product roadmap

Status: Proposed  
Last reviewed: 2026-08-30
Owner: Christopher Guzowski

## Purpose

Complete one convincing payment-incident investigation path before expanding
to another incident family or adding broad platform capabilities. Each product
slice must produce an observable operator outcome across the necessary UI,
API, persistence, integration, and audit boundaries.

This roadmap orders outcomes. It does not replace the active contract in
`tasks/current.md`, and a proposed product task does not authorize executable
changes until the owner approves and activates it.

## Target end-to-end path

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

The path through the evidence-linked report is implementation-complete; live
local Ollama smoke verification for the embedding and report paths remains
pending. The remaining product work should follow the order below
because each slice supplies an auditable input or state required by the next
one.

## Ordered work

| Order | Outcome | Status | Completion boundary |
|---|---|---|---|
| E1 | Establish one authoritative verification entry point | Complete | Local and CI completion use the same full repository checks. |
| P1 | Retrieve approved operational knowledge | Implementation complete; local smoke pending | The operator can retrieve and inspect tenant-approved runbook and policy excerpts with complete retrieval provenance. |
| P2 | Generate a reviewable incident report | Implementation complete; local smoke pending | Ollama locally produces a schema-valid report whose observations, inferences, and recommendation cite persisted evidence and retrieval context; the incident enters `AWAITING_REVIEW`. |
| P3 | Record the human decision and audit trail | Planned | The operator approves or rejects with attributable input, and the complete incident timeline remains reviewable. |
| P4 | Prove and harden the closed loop | Planned | One repeatable end-to-end scenario covers alert through decision, including partial-source and invalid-model failure paths. |
| D1 | Select and implement the initial AWS deployment shape | Deferred | The verified closed loop runs through least-privilege AWS infrastructure. |
| D2 | Add authentication and enforce operator identity | Deferred | Identity is authenticated and tenant authorization is enforced at every public boundary. |

## Next product slice

P3, `Record the human decision and audit trail`, is the next product slice. It
should add attributable approval/rejection and a reviewable incident timeline
without allowing AI output to decide or execute the human action.

The completed P2 contract is archived under `tasks/completed/`, and its accepted
report contract is in `decisions/ADR-0006-evidence-linked-report-generation.md`.
ADR-0007 selects Ollama as the active local provider.

## Sequencing rules

- Preserve the completed P2 contract and verification evidence while preparing
  a reviewed P3 proposal.
- Do not combine P1 with report generation or human decision behavior.
- Do not add another incident family before P4 is complete.
- Add further MCP evidence tools only when a report-quality evaluation shows
  that the current evidence is insufficient for the chosen incident family.
- Keep shared Testcontainers fixtures and other internal consolidation in the
  maintenance backlog unless duplication measurably blocks a product slice.
- Local P1/P2 development uses `nomic-embed-text` and `qwen3.5:4b`; an optional
  Bedrock production profile remains a deployment decision.

## Roadmap review triggers

Revisit the order if local Ollama quality is insufficient, retrieval evaluation
shows the chosen knowledge design is not fit for the incident family, or a
security constraint prevents safe local and CI verification of the next slice.
