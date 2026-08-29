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

The path through the evidence-linked report is implementation-complete;
authorized live Bedrock smoke verification for the embedding and report slices
remains pending. The remaining product work should follow the order below
because each slice supplies an auditable input or state required by the next
one.

## Ordered work

| Order | Outcome | Status | Completion boundary |
|---|---|---|---|
| E1 | Establish one authoritative verification entry point | Complete | Local and CI completion use the same full repository checks. |
| P1 | Retrieve approved operational knowledge | Implementation complete; external smoke pending | The operator can retrieve and inspect tenant-approved runbook and policy excerpts with complete retrieval provenance. |
| P2 | Generate a reviewable incident report | Implementation complete; external smoke pending | Bedrock produces a schema-valid report whose observations, inferences, and recommendation cite persisted evidence and retrieval context; the incident enters `AWAITING_REVIEW`. |
| P3 | Record the human decision and audit trail | Planned | The operator approves or rejects with attributable input, and the complete incident timeline remains reviewable. |
| P4 | Prove and harden the closed loop | Planned | One repeatable end-to-end scenario covers alert through decision, including partial-source and invalid-model failure paths. |
| D1 | Select and implement the initial AWS deployment shape | Deferred | The verified closed loop runs through least-privilege AWS infrastructure. |
| D2 | Add authentication and enforce operator identity | Deferred | Identity is authenticated and tenant authorization is enforced at every public boundary. |

## Next product slice

P3, `Record the human decision and audit trail`, is the next product slice. It
should add attributable approval/rejection and a reviewable incident timeline
without allowing AI output to decide or execute the human action.

The completed P2 contract remains in `tasks/current.md` until a reviewed P3
proposal is activated. Its accepted report/model contract is in
`decisions/ADR-0006-evidence-linked-report-generation.md`.

## Sequencing rules

- Preserve the completed P2 contract and verification evidence until a reviewed
  P3 proposal is activated and P2 is archived.
- Do not combine P1 with report generation or human decision behavior.
- Do not add another incident family before P4 is complete.
- Add further MCP evidence tools only when a report-quality evaluation shows
  that the current evidence is insufficient for the chosen incident family.
- Keep shared Testcontainers fixtures and other internal consolidation in the
  maintenance backlog unless duplication measurably blocks a product slice.
- P2 uses `${BEDROCK_CHAT_MODEL:global.amazon.nova-2-lite-v1:0}` as the selected
  configurable default; the embedding selection for P1 remains independent.

## Roadmap review triggers

Revisit the order if a required Bedrock capability is unavailable in the
target region, retrieval evaluation shows the chosen knowledge design is not
fit for the incident family, or a security constraint prevents safe local and
CI verification of the next slice.
