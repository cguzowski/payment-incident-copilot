# ADR-0003: Copilot feature ownership and snapshot boundaries

Status: Accepted  
Date: 2026-08-29  
Decision owner: Christopher Guzowski

## Context

The copilot API grew incident lifecycle, evidence collection, approved-knowledge
catalog, and investigation-time knowledge retrieval behavior inside broad
packages. Knowledge persistence reached into incident and evidence tables, and
evidence collection depended directly on incident implementation details. That
made the next report-generation slice likely to depend on storage records rather
than stable application contracts.

The three deployables must remain independently buildable. The current scale
does not justify turning each feature into a Maven module or introducing shared
compiled DTOs.

## Decision drivers

- Explicit ownership of behavior and persistence
- Tenant identity at every read and persistence boundary
- Stable, minimal inputs for later report generation
- Preservation of evidence applicability and audit semantics
- Independently buildable deployables without speculative module overhead
- Automated enforcement of allowed dependency directions

## Considered options

### Keep broad packages and rely on code review

- Advantages: No mechanical moves or new tests.
- Disadvantages: Ownership remains implicit and storage reach-through can recur.

### Split every feature into a Maven module

- Advantages: Compile-time module boundaries.
- Disadvantages: Adds build and dependency-management overhead inside one small
  deployable and encourages cross-module DTO extraction before it is needed.

### Feature packages with narrow snapshot ports and architecture tests

- Advantages: Establishes ownership and stable composition seams while keeping
  the current deployment and build shape.
- Disadvantages: Package rules require maintenance, and ports remain internal
  Java contracts rather than separately versioned service APIs.

## Decision

Keep the copilot API as one Maven module and organize it into `incident`,
`evidence`, `knowledge.catalog`, and `knowledge.retrieval` feature packages.
Enforce allowed feature directions and adapter ownership with architecture
tests.

Incident publishes tenant-scoped investigation and evidence-collection context
ports. Evidence publishes a normalized latest/applicable snapshot that does not
expose persistence records, MCP payload records, or its internal status enum.
Knowledge retrieval composes those ports before recording an attempt. Knowledge
retrieval persistence owns only retrieval attempt and result storage and does
not query or decode incident or evidence tables.

Catalog owns approved source loading, parsing, chunking, hashing, embeddings,
ingestion, and index writes. Retrieval owns investigation-time query derivation,
search, selection, attempts, history, and HTTP behavior. Do not introduce a
global common package, new deployable, feature Maven modules, or shared compiled
DTO artifact for these boundaries.

## Rationale

Package ownership plus narrow read ports removes the concrete reach-through
that creates coupling today, while preserving the operational simplicity of one
API deployable. Normalized snapshots give report generation a small, auditable
composition boundary without making persistence records into public contracts.

## Consequences

### Positive

- Each feature owns its behavior, tables, adapters, and HTTP surface.
- Knowledge retrieval and future report composition consume tenant-scoped
  snapshots instead of foreign storage.
- Evidence applicability semantics have one evidence-owned implementation.
- Architecture regressions fail automated tests.

### Negative or accepted tradeoffs

- Some incident and catalog contracts are intentionally visible to dependent
  feature packages within the same deployable.
- Mechanical package moves create a large diff without changing deployment.
- A future scale-driven Maven split would require a separate decision.

## Validation or revisit trigger

Revisit when measured build ownership, release cadence, team boundaries, or
runtime scaling justify an independently deployed service or Maven module. A
new consumer needing a network contract must not treat these internal snapshot
ports as an externally versioned API without a new decision.
