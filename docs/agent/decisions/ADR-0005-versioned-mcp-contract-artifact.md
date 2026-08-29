# ADR-0005: Immutable repository-owned MCP wire contract

Status: Accepted  
Date: 2026-08-29  
Decision owner: Christopher Guzowski

## Context

The operations MCP server and copilot API implemented
`getRecentServiceErrors` with separate Java records and duplicated assumptions
about tool metadata, schemas, and structured results. Unit tests could pass even
if provider discovery or a canonical response drifted from the consumer's
decoder.

The services must remain independently buildable and must not share a compiled
DTO jar. The contract uses synthetic fixtures only.

## Decision drivers

- One inspectable definition of the cross-service wire contract
- Provider and consumer verification against the same canonical data
- Independent implementation types and deployment builds
- Explicit compatibility rules for future changes
- Separation of transport failures from payload validation

## Considered options

### Keep duplicated Java expectations

- Advantages: No new artifact format or resource setup.
- Disadvantages: Drift is detected late or not at all, and implementation
  records become accidental wire specifications.

### Publish a shared compiled DTO library

- Advantages: Compile-time reuse of Java types.
- Disadvantages: Couples service releases, does not by itself verify MCP
  discovery metadata, and makes one language implementation the contract.

### Store schemas, metadata, and fixtures as a versioned repository artifact

- Advantages: Language-neutral, reviewable, testable by both services, and
  independent of implementation records.
- Disadvantages: Requires semantic contract tests and deliberate version
  management.

## Decision

Own the immutable `getRecentServiceErrors` v1 artifact at
`contracts/mcp/get-recent-service-errors/v1`. It contains tool metadata, input
and output JSON schemas, and canonical available and unavailable synthetic
fixtures. Both Java service test suites load this exact directory as test
resources while retaining separate provider and consumer records.

Provider tests compare live MCP discovery, annotations, schemas, and structured
responses semantically with the artifact. Consumer tests decode the canonical
fixtures and reject unknown fields, identifier mismatches, invalid
status/content combinations, and bounded-value violations. The consumer MCP
gateway maps transport failures; an evidence-owned typed decoder validates the
payload.

A backward-incompatible change creates a sibling `v2` artifact. It never
rewrites `v1`.

## Rationale

The artifact tests the actual integration boundary without coupling production
code to shared Java types. Canonical fixtures make availability behavior and
validation limits concrete, while semantic comparisons avoid dependence on
irrelevant JSON property order.

## Consequences

### Positive

- Provider and consumer verify the same wire definition.
- Live provider discovery and response drift fail tests.
- Consumer validation remains evidence-owned and transport-independent.
- Historical v1 behavior remains reviewable after a future v2 is introduced.

### Negative or accepted tradeoffs

- Test-resource configuration references a repository-level artifact.
- Semantic checks must be maintained when compatible optional fields are added.
- Version selection and coexistence require an explicit migration in both
  services.

## Validation or revisit trigger

Revisit if MCP tooling provides a sufficiently complete, language-neutral
contract publication and compatibility system, or if services move to separate
repositories and require a released artifact. Any replacement must preserve
immutable versions, shared provider/consumer verification, and separate
implementation records.
