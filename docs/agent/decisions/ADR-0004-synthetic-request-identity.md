# ADR-0004: Centralized synthetic HTTP request identity

Status: Accepted  
Date: 2026-08-29  
Decision owner: Christopher Guzowski

## Context

The demonstration HTTP API transported tenant and operator identity
inconsistently through query parameters, request bodies, and feature-specific
frontend code. That obscured which values were resource data, made endpoint
contracts drift, and invited one feature to omit tenant context.

The MVP does not yet implement authentication or authorization. Its identity
transport must therefore remain visibly synthetic and must not imply that a
caller-supplied identifier is a security credential.

## Decision drivers

- One request convention across every application endpoint
- Central validation and consistent malformed-request behavior
- Explicit tenant parameters below the HTTP boundary
- No compatibility period with two active identity conventions
- Honest separation between demonstration context and authentication

## Considered options

### Continue feature-specific query and body fields

- Advantages: Avoids an HTTP migration.
- Disadvantages: Duplicates identity logic and mixes caller context with
  resources and commands.

### Introduce an authentication token now

- Advantages: Closer to a production security model.
- Disadvantages: Requires unresolved identity-provider, authorization, and
  deployment decisions outside the MVP boundary task.

### Use explicit synthetic request headers

- Advantages: Centralizes transport without pretending to solve authentication;
  leaves resource identifiers in paths and commands focused on behavior.
- Disadvantages: Callers can still spoof the values, so the convention must not
  be described as a security control.

## Decision

Require `X-Synthetic-Tenant-Id` on application HTTP requests. Require
`X-Synthetic-Operator-Id` on operator-attributed mutations, initially
investigation start. Remove tenant and operator identity from application
resource paths, query parameters, and request bodies. Queue reads use
`GET /api/incidents`; alert intake obtains tenant context from the header; and
investigation start has no operator request body.

Use one backend request-context resolver for header presence, syntax,
duplication, and legacy-parameter rejection. Use one frontend interceptor to
attach context. Continue passing tenant identity explicitly through application
and persistence ports and continue tenant-scoped lookups with indistinguishable
cross-tenant not-found behavior.

These headers are synthetic demonstration context, not authentication,
authorization, or a production security boundary.

## Rationale

The header convention removes identity plumbing from feature resource clients
and gives the HTTP surface one auditable migration point. Keeping identity
explicit below the resolver prevents centralization from becoming implicit
global state or weakening persistence isolation.

## Consequences

### Positive

- Every endpoint uses one documented identity transport.
- Feature controllers and frontend services stop constructing identity fields.
- Missing, malformed, duplicate, and legacy inputs receive consistent errors.
- A later authentication layer can replace the synthetic resolver without
  changing application-port tenant parameters.

### Negative or accepted tradeoffs

- Existing callers must migrate atomically; the old convention is rejected.
- The frontend still has configured synthetic identifiers for the demonstration.
- Headers provide no proof of identity and are unsuitable for production use.

## Validation or revisit trigger

Replace this decision when authentication and authorization are selected. That
design must derive trusted tenant and operator identity from verified
credentials, preserve explicit downstream tenant scope, and define migration
and audit behavior separately.
