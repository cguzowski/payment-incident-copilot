# Operations MCP server instructions

These instructions extend the repository root `AGENTS.md` for work under this
service.

## Service responsibility

This service simulates disconnected operational systems and exposes narrowly
defined MCP tools. It supplies evidence to the copilot API; it does not perform
investigation reasoning or call an LLM.

## Tool rules

- Keep tool results deterministic for a given scenario and identifier.
- Begin with read-only tools. Do not expose mutation or remediation tools.
- Give every tool a narrow purpose, explicit parameters, and a stable response
  schema.
- Return source system, retrieval time, correlation identifier, and data
  availability status where applicable.
- Represent not-found, unavailable, and timeout cases distinctly.
- Do not hide simulated failures behind successful empty responses.
- Store synthetic fixtures in readable, reviewable files when practical.
- Never introduce real credentials, endpoints, customer data, or transaction
  data.

## Initial tool candidates

- `getTransaction`
- `getPaymentAttempts`
- `getGatewayResponse`
- `getAccountStatus`
- `getRecentServiceErrors`
- `getDeploymentHistory`
- `getFeatureFlags`

Only implement tools required by the active scenario.

## Testing

- Contract-test tool names, input schemas, and response schemas.
- Test unavailable and malformed-data scenarios.
- Keep fixtures stable so end-to-end demonstrations are repeatable.
