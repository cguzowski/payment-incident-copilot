# Architecture

Last reviewed: 2026-08-28

## System boundaries

| Component | Owns | Does not own |
|---|---|---|
| Operator console | Incident work queue, investigation UX, review and decision input | Investigation reasoning or persistence |
| Copilot API | Workflow, persistence, retrieval, report generation, decisions, audit | Synthetic source-system behavior |
| Operations MCP server | Deterministic synthetic operational tools and fixtures | LLM calls or investigation decisions |
| PostgreSQL | Transactional application state and audit records | Unstructured object storage |
| pgvector | Tenant-filtered knowledge chunks and embeddings | Final report truth |
| Amazon Bedrock | Embeddings and structured report generation | Autonomous operational authority |

## End-to-end scenario

```mermaid
sequenceDiagram
    participant S as Synthetic Alert Source
    participant A as Copilot API
    participant D as PostgreSQL/pgvector
    participant U as Operator Console
    participant M as Operations MCP Server
    participant B as Amazon Bedrock

    S->>A: Submit synthetic alert
    A->>D: Persist NEW incident
    U->>A: Load incident work queue
    A-->>U: Active incident summaries
    U->>A: Start investigation
    A->>M: Call required read-only tools
    M-->>A: Sourced operational evidence
    A->>D: Persist evidence and retrieval STARTED
    A->>B: Embed bounded derived retrieval query
    B-->>A: Normalized 1,024-dimension vector
    A->>D: Filtered full-text plus exact vector search
    D-->>A: Tenant-filtered knowledge chunks
    A->>D: Persist immutable retrieval snapshot
    A-->>U: Approved source excerpts and provenance
    A->>A: Normalize and classify evidence
    A->>B: Evidence plus versioned report schema
    B-->>A: Structured proposed report
    A->>A: Validate schema and citations
    A->>D: Persist report, evidence, and metadata
    A-->>U: Reviewable investigation snapshot
    U->>A: Approve or reject with reason
    A->>D: Append decision audit event
    A-->>U: Updated final state
```

## Primary states

Suggested incident lifecycle:

```text
NEW -> INVESTIGATING -> AWAITING_REVIEW -> APPROVED
                                     \-> REJECTED
```

Failure to gather sufficient evidence should remain visible and should not be
misrepresented as a successful investigation.

## Operator work queue

The MVP uses one tenant-scoped incident work queue rather than separate alert
and investigation queues. `NEW` means unprocessed, not recently received, so an
incident remains visible regardless of age until its state changes.

The same queue retains active incidents as they move through `NEW`,
`INVESTIGATING`, and later `AWAITING_REVIEW`. Newly received incidents appear
first by default, and the operator may change the sort without changing queue
membership. When terminal states are implemented, they may be hidden by default
and exposed through a filter or history mode within the same incident surface.

Starting an investigation updates the existing incident row's workflow state;
it does not transfer the incident into a separate list. Queue projections may
carry the active investigation identifier needed for a resume action, but they
must not expose tenant or internal persistence metadata.

## Multi-tenant preparation

The MVP exposes one tenant, but tenant identity should remain explicit on
incidents, knowledge, reports, and audit records. Every application query and
vector retrieval must be tenant-scoped. Do not claim production-grade tenant
isolation until it is tested and enforced at every boundary.

## Deployment shape

Keep the initial deployment simple:

- Static Angular application
- One copilot API container
- One synthetic MCP server container
- One managed PostgreSQL instance with pgvector
- Amazon Bedrock through IAM roles

Document the chosen AWS services in an ADR when deployment work begins.
