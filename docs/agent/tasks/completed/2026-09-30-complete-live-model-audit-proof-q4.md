# Task: Complete live-model audit proof (Q4)

Status: Complete — live rejection and audit proof verified
Created: 2026-09-30
Owner: Christopher Guzowski

## Goal

Verify one newly generated live-model report through an explicit human decision,
terminal incident state, and complete attributable audit timeline.

## User story

As an operator, I want to review a live report and record my decision so that
its evidence, knowledge, model provenance, and final disposition remain auditable.

## Chosen contract

Use the existing synthetic generator, tenant-scoped API, Ollama model, human
decision endpoint, and projected timeline. The owner supplies the final decision
and reason after reviewing the exact persisted report. Preserve all attempts.

## In scope

One fresh synthetic investigation, evidence collection, approved knowledge
retrieval, live report generation, owner review, final decision verification,
queue membership, and retained factual proof in documentation.

## Out of scope

Production behavior changes, model or retrieval tuning, broader scenario
coverage, oracle reveal, deployment, and authentication.

## Constraints

Synthetic data only; no automatic report approval, recommendation execution,
or invented human reasons. Preserve tenant scope and immutable source metadata.

## Acceptance criteria

- [x] A fresh investigation has persisted evidence and approved knowledge attempts.
- [x] A new live Ollama report is AVAILABLE/PROPOSED with source references and
      model, prompt, schema, and retrieval provenance; incident is AWAITING_REVIEW.
- [x] The owner explicitly supplies APPROVED or REJECTED and a reason; the
      persisted decision binds the exact report and synthetic operator.
- [x] The incident reaches the matching terminal state and appears in Completed
      rather than Active; the proposed report remains unchanged.
- [x] The chronological timeline includes intake, investigation, every evidence,
      retrieval, and report attempt, and the final attributable human decision.
- [x] Repository verification and diff checks pass; exact live proof and
      remaining limitations are recorded.

## Test plan

Manual operational verification against existing HTTP contracts, with exact
IDs, statuses, timestamps, source references, and provenance recorded. Compare
report JSON before and after the human decision and verify timeline ordering
and queue membership. No executable change is planned, so no artificial new
tests are required. Run `./verify.ps1 -Scope Repository` and `git diff --check`.

## Progress notes

- Owner activated the next ordered roadmap outcome by requesting the next task.
- Existing staged documentation changes were inspected and preserved.
- API, generator, Ollama, and operator console responded locally.


- Fresh S012 incident: `6d34b88f-215a-481e-a181-10171fd69a47`;
  investigation: `70e57329-2d6d-49d9-ba11-98862268ebc2`.
- Evidence `d3fa5797-07ff-4080-bb9b-e1ac5e33a615` is AVAILABLE:
  NETWORK_PACKET_LOSS count 48, UPSTREAM_CONNECTION_RESET count 21.
- Retrieval `b88ffe00-8bf9-47ae-b981-0415f3a4e96f` is AVAILABLE using
  `knowledge-query/v2`, `nomic-embed-text` (768 dimensions), and
  `postgres-hybrid-related/v4`.
- Local retrieval includes historical `rb-002` v2.0.0 scenario-matrix content.
  This audit proof must not be interpreted as oracle-independent corpus-v2
  model-quality evaluation. No catalog or retrieval tuning is in Q4 scope.
- `./verify.ps1 -Scope Repository` passed on 2026-09-30.
- Browser recovery: the open page retained STARTED report and INVESTIGATING
  lifecycle snapshots after API-driven generation, despite its timeline showing
  AVAILABLE. Reloading displayed the exact completed report, AWAITING_REVIEW,
  and Approve/Reject/Reason controls. No executable behavior changed.
- The owner added AVAILABLE evidence and retrieval attempts after generation;
  the refreshed timeline retains all seven events. The report still references
  its original evidence and retrieval snapshots.


## Completion evidence

Report attempt `64b09be2-1997-438d-a47c-272899e71736` is AVAILABLE/PROPOSED,
using `qwen3:8b-q4_K_M`, `report-prompt/v4`, and `report-v1`.
Generation ran 12:52:25.918285 to 12:54:02.062065 UTC (96.1 seconds).
Readback confirmed AWAITING_REVIEW, no decision, and five ordered timeline
events with matching source IDs and synthetic operator attribution.
The pre-decision compact JSON readback is retained in the local temporary
directory for comparison. SHA-256: `64d3e183d0361e3acc5bad7f041ef2da2e70cbf46710a13c04b419063db05669`.

The owner submitted REJECTED with the exact reason `Testing` through the console.
Decision `44bc1c2c-2826-47b8-8573-dff5941d8023`, recorded at
2026-09-30T13:13:29.034760Z, binds report attempt
`64b09be2-1997-438d-a47c-272899e71736` and synthetic operator
`7b636625-53d1-46f7-92a9-9c8c27a243d1`.

Tenant-scoped HTTP readback verified:

- Investigation and incident are REJECTED; the incident is absent from Active
  and present exactly once in Completed.
- The complete report readback is byte-for-byte identical to the pre-decision
  compact JSON baseline, including AVAILABLE/PROPOSED disposition and provenance.
- Eight events occur in chronological order: alert intake, investigation start,
  two evidence attempts, two retrieval attempts, one live report, and the human
  decision. Every operator event retains the synthetic operator ID; final event
  source ID, exact report binding, outcome, actor, and reason match the decision.
- `./verify.ps1 -Scope Repository` and `git diff --check` passed on 2026-09-30.

## Remaining limitations

One live rejection proves workflow and provenance, not broad model quality or
production identity. The reason `Testing` is a demonstration input, not a report-
quality judgment. Historical scenario-matrix content was retrieved; this run
does not prove oracle-independent corpus-v2 quality. An already open page
required reload after report generation was triggered outside the console.

## Decisions needed

None.
