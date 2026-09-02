# Domain glossary

Last reviewed: 2026-08-31

## Core terms

**Alert**  
A machine-generated signal indicating that something may require attention. An
alert is an input, not a confirmed root cause.

**Incident**  
The platform record used to track an operational problem or suspected problem
through investigation and human decision.

**Investigation**  
A bounded attempt to collect evidence, retrieve applicable knowledge, and
produce a reviewable explanation for an incident.

**Incident work queue**

The operator's single tenant-scoped incident surface. Its Active view contains
`NEW`, `INVESTIGATING`, and `AWAITING_REVIEW` work; its Completed view keeps
terminal `APPROVED` and `REJECTED` incidents discoverable.

**Evidence item**  
A normalized fact retrieved from a named source. It includes the source,
retrieval time, status, and identifiers needed for auditability.

**Observation**  
A fact directly supported by an evidence item.

**Inference**  
An interpretation derived from one or more observations. It must not be
presented as directly observed fact.

**Probable cause**  
The best-supported explanation currently available. It is not necessarily a
verified root cause.

**Runbook**  
Approved operational guidance describing diagnostic or response steps for a
known condition.

**Policy**  
An organizational or compliance rule that constrains investigation or action.

**Recommendation**  
A proposed next step for the operator. In the MVP it is advisory only.

**Source reference**

A stable pointer from a report claim to the exact persisted evidence attempt or
approved-knowledge chunk that supports it. Displayed provenance is resolved by
the application, not accepted from model-supplied source text.

**Proposed incident report**

An application-owned, schema-valid AI-assisted artifact that keeps observations,
inferences, probable cause, confidence, recommendation, contradictions, and
evidence gaps distinct and source-linked. Proposed means available for human
review, not approved or authoritative; a report may explicitly conclude that
evidence is insufficient.

**Human decision**  
An explicit, final approval or rejection of the exact proposed report under
review, recorded separately with the synthetic operator identity, UTC timestamp
and required bounded reason. It does not mutate the report or execute its
recommendation.

**Audit timeline event**
A safe chronological projection of one authoritative alert, investigation,
evidence, retrieval, report, or human-decision record. The timeline preserves
attempt outcomes and provenance without copying raw evidence, prompts, model
payloads, stack traces or secrets into a second write model.

**Unattributed actor**
An honest marker for a historical evidence or retrieval attempt created before
operator attribution was persisted. It must not be replaced with an invented
operator.

**Tenant**  
The financial-services organization whose incidents, knowledge, and audit data
are logically isolated. The MVP demonstrates one tenant while retaining tenant
identifiers in the model.

**SynTen Inc**

The fictional company name assigned to the existing MVP synthetic tenant,
`8b860d80-d17f-4e6b-8c48-af35f26a4d61`. It is not a real company or a second
tenant. All incidents, operational records, runbooks, policies, people, and
identifiers associated with it are synthetic. Tenant-specific assets live
under `SynTen Inc/`.

**Synthetic knowledge corpus**

The version-controlled collection of SynTen Inc runbooks, policies, metadata,
and later retrieval-evaluation cases. Source documents are not evidence of a
real event and do not become report truth merely because retrieval selected
them.

**Source PDF**

A human-readable, text-based synthetic runbook or policy artifact. PDF-derived
chunks must remain traceable to the exact document version and source location;
the extraction and locator contract is selected before ingestion is changed.

## Vocabulary rule

Use these terms consistently in code, APIs, UI labels, documentation, and
tests. Add or revise a definition before introducing an overlapping term.
