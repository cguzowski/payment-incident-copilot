# Domain glossary

Last reviewed: 2026-08-20

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

**Human decision**  
An explicit approval or rejection recorded by the operator with identity,
timestamp, and reason where required.

**Audit event**  
An append-oriented record of a meaningful action, state transition, tool call,
report generation event, or human decision.

**Tenant**  
The financial-services organization whose incidents, knowledge, and audit data
are logically isolated. The MVP demonstrates one tenant while retaining tenant
identifiers in the model.

## Vocabulary rule

Use these terms consistently in code, APIs, UI labels, documentation, and
tests. Add or revise a definition before introducing an overlapping term.
