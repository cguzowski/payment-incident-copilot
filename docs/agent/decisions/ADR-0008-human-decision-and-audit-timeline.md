# ADR-0008: Human decision ownership and projected audit timeline

Status: Accepted
Date: 2026-08-30
Decision owner: Christopher Guzowski

## Context

The implemented investigation path now preserves tenant-scoped incident,
evidence, knowledge-retrieval, and report-generation records. One schema-valid
report becomes the exact review candidate and moves the incident to
`AWAITING_REVIEW`, but the system cannot yet record the operator's final
decision or present one complete incident timeline.

P3 must keep the human decision separate from AI output, commit the final state
atomically, preserve tenant and report provenance, and expose all existing
attempt and failure history without coupling one persistence adapter to every
feature table.

## Decision drivers

- Human authority remains explicit and separate from model output.
- One exact report is the immutable subject of one attributable decision.
- Terminal lifecycle changes and decision persistence cannot diverge.
- Existing retries, failures, missing attribution, and historical metadata are
  preserved without fabricated backfill.
- Feature-owned persistence and B01-B06 package directions remain enforceable.
- The MVP avoids dual-write audit state, event infrastructure, and speculative
  operational complexity.

## Considered options

### Store the decision on the report row

- Advantages: Minimal schema and lookup work.
- Disadvantages: Conflates AI-generated content with human judgment, mutates the
  report artifact, and weakens independent decision provenance.

### Add a generic audit-event table and write every action twice

- Advantages: One physical timeline source and a conventional event-log shape.
- Disadvantages: Requires every feature to coordinate dual writes, duplicates
  authoritative records, needs a fabricated or incomplete backfill for existing
  data, and introduces synchronization failure modes without an MVP need.

### Add an append-only decision feature and project the timeline from feature records

- Advantages: Keeps the decision distinct, reuses exact authoritative history,
  exposes existing failures and retries, avoids dual writes, and respects
  persistence ownership through narrow read ports.
- Disadvantages: Timeline reads compose several feature snapshots and require
  stable ordering and response normalization in application code.

## Decision

Create a `decision` feature that owns one append-only human decision per
investigation and exact available report. The client supplies only `APPROVED` or
`REJECTED` plus a bounded reason. Tenant and operator come from validated
synthetic request context, and the server resolves the report candidate.

Persist the decision and conditionally transition the incident from
`AWAITING_REVIEW` to the matching terminal status in one transaction. Incident
continues to own lifecycle mutation behind a narrow port. The report remains an
unchanged AI-generated proposed artifact. An exact same-operator replay is
idempotent; any different second decision conflicts. Decisions are final and
non-amendable in the MVP.

Create a read-only `audit` feature that composes tenant-scoped timeline snapshot
ports published by incident, evidence, knowledge retrieval, report, and
decision. It returns bounded application-owned events in deterministic
chronological order. It does not own a generic audit-event table and no audit or
decision persistence adapter queries another feature's tables.

Capture operator identity on new evidence and knowledge-retrieval attempts.
Keep the new columns nullable for existing records, and display missing legacy
identity as `UNATTRIBUTED`. Never infer or backfill an actor.

Keep terminal incidents on the existing incident surface through explicit
Active and Completed views. The active view remains the default.

## Consequences

### Positive

- Approval and rejection are attributable human records, not report mutations.
- The decision and terminal incident state cannot commit independently.
- Every existing attempt, retry, and failure remains visible without copying
  or rewriting historical data.
- Missing historical actor identity is honest and reviewable.
- No event bus, generic event store, outbox, or cross-feature persistence query
  is introduced.
- Completed work remains discoverable after leaving the active queue.

### Negative or accepted tradeoffs

- A timeline request performs several bounded in-process reads and application
  composition. This is acceptable for one incident and small MVP histories.
- The timeline is an immutable-style projection, not a claim of cryptographic
  tamper evidence or event sourcing.
- Historical evidence and retrieval rows cannot gain operator attribution
  without fabrication, so they remain explicitly unattributed.
- Final decisions cannot be corrected inside the MVP. A future superseding
  decision workflow will need its own append-only semantics and owner approval.
- Synthetic headers provide attribution for the demonstration but are not
  authentication or production authorization.

## Validation or revisit trigger

Revisit the projected timeline if measured read cost becomes material, if an
external compliance integration requires durable exported events, or if
multiple services must consume audit events asynchronously. Revisit finality
when the product has an approved correction, reopening, or superseding-decision
workflow. Revisit identity semantics before production authentication is added.
