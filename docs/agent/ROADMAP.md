# Product roadmap

Status: Proposed  
Last reviewed: 2026-08-28  
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

The path through approved operational knowledge is implementation-complete;
authorized live Bedrock smoke verification for that slice remains pending. The
remaining product work should follow the order below because each slice
supplies an auditable input or state required by the next one.

## Ordered work

| Order | Outcome | Status | Completion boundary |
|---|---|---|---|
| E1 | Establish one authoritative verification entry point | Proposed future task | Local and CI completion use the same full repository checks. |
| P1 | Retrieve approved operational knowledge | Implementation complete; external smoke pending | The operator can retrieve and inspect tenant-approved runbook and policy excerpts with complete retrieval provenance. |
| P2 | Generate a reviewable incident report | Planned | Bedrock produces a schema-valid report whose observations, inferences, and recommendation cite persisted evidence and retrieval context; the incident enters `AWAITING_REVIEW`. |
| P3 | Record the human decision and audit trail | Planned | The operator approves or rejects with attributable input, and the complete incident timeline remains reviewable. |
| P4 | Prove and harden the closed loop | Planned | One repeatable end-to-end scenario covers alert through decision, including partial-source and invalid-model failure paths. |
| D1 | Select and implement the initial AWS deployment shape | Deferred | The verified closed loop runs through least-privilege AWS infrastructure. |
| D2 | Add authentication and enforce operator identity | Deferred | Identity is authenticated and tenant authorization is enforced at every public boundary. |

## Next product slice

After P1's authorized Bedrock smoke and owner review, P2, `Generate a reviewable
incident report`, is the next product slice. It will consume the persisted
observed evidence and approved-knowledge retrieval context while preserving the
boundary between facts, AI inference, and human decision.

The still-active P1 task contract is in `tasks/current.md`. The accepted
retrieval design is recorded in
`decisions/ADR-0002-hybrid-knowledge-retrieval.md`. P2 is not yet an approved
task contract.

## Sequencing rules

- Complete or explicitly supersede the active task before activating P2.
- Do not combine P1 with report generation or human decision behavior.
- Do not add another incident family before P4 is complete.
- Add further MCP evidence tools only when a report-quality evaluation shows
  that the current evidence is insufficient for the chosen incident family.
- Keep shared Testcontainers fixtures and other internal consolidation in the
  maintenance backlog unless duplication measurably blocks a product slice.
- Select the Bedrock chat model when P2 is proposed; the embedding selection
  for P1 does not decide the report-generation model.

## Roadmap review triggers

Revisit the order if a required Bedrock capability is unavailable in the
target region, retrieval evaluation shows the chosen knowledge design is not
fit for the incident family, or a security constraint prevents safe local and
CI verification of the next slice.
