# Constraints and guardrails

Last reviewed: 2026-08-20

## Product constraints

- Build one convincing vertical slice before adding additional incident types.
- Use synthetic scenarios and synthetic operational records only.
- The platform investigates; it does not process payments.
- The model assists; the operator makes the final decision.
- Partial and unavailable evidence must remain visible.

## Technical constraints

- Java 21, Spring Boot, Spring AI, Maven, and Angular.
- PostgreSQL stores application state; pgvector stores knowledge embeddings.
- Amazon Bedrock provides the initial chat and embedding models.
- MCP integration begins with one deterministic synthetic server.
- Services remain independently deployable despite sharing one repository.
- Use Flyway for database changes.
- Use Docker Compose only for required local infrastructure.
- Do not introduce Redis, Kafka, Kubernetes, or a gateway without a measured
  requirement.

## Data and security constraints

- Never commit AWS keys, database secrets, tokens, or private endpoints.
- Never use real cardholder, bank-account, customer, or merchant data.
- Use opaque synthetic identifiers rather than realistic sensitive values.
- Carry `tenant_id` through persistence and retrieval boundaries.
- Avoid sensitive data in prompts, logs, traces, exceptions, and audit details.
- Use least-privilege IAM roles when AWS integration is deployed.

## Responsible-AI constraints

- Reports must conform to an application-owned schema.
- Every observation and inference must be traceable to evidence identifiers.
- Store model identifier, prompt/template version, generation timestamp, and
  retrieval context identifiers.
- A schema-valid report can still be wrong; the UI must communicate this.
- Missing or contradictory evidence must reduce confidence, not invite
  fabrication.
- No generated recommendation may execute automatically.
- Operator approval and rejection are explicit, attributable events.

## Scope-change rule

When a task appears to require expanding these constraints, stop and document
the proposed decision and tradeoff before implementation.
