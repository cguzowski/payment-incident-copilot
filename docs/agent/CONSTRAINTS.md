# Constraints and guardrails

Last reviewed: 2026-08-31

## Product constraints

- Build one convincing vertical slice before adding additional incident types.
- Preserve the completed authorization-decline vertical slice while expanding
  knowledge depth; do not use the corpus phase to add another incident family.
- Use synthetic scenarios and synthetic operational records only.
- The platform investigates; it does not process payments.
- The model assists; the operator makes the final decision.
- Partial and unavailable evidence must remain visible.

## Technical constraints

- Java 21, Spring Boot, Spring AI, Maven, and Angular.
- PostgreSQL stores application state; pgvector stores knowledge embeddings.
- Ollama provides local chat and embedding models through Spring AI;
  PostgreSQL/pgvector remains the vector store.
- Automated tests use mocked or deterministic model responses and must not
  require a live model provider.
- Amazon Bedrock may be added as an optional production profile near the
  deployment milestone, after the local closed loop is complete.
- MCP integration begins with one deterministic synthetic server.
- Services remain independently deployable despite sharing one repository.
- Use Flyway for database changes.
- Use Docker Compose only for required local infrastructure.
- Do not introduce Redis, Kafka, Kubernetes, or a gateway without a measured
  requirement.

## Data and security constraints

- Never commit AWS keys, database secrets, tokens, or private endpoints.
- Never use real cardholder, bank-account, customer, or merchant data.
- SynTen Inc is fictional. Its profile, runbooks, policies, examples, names,
  identifiers, and operational history must be synthetic and must not reproduce
  a real company's confidential or proprietary material.
- Keep SynTen Inc-specific profiles, source content, PDFs, manifests, corpus
  validation assets, and retrieval-evaluation fixtures under `SynTen Inc/`.
- Make each SynTen Inc PDF operationally credible and no more than 15 pages,
  counting cover pages, document-control pages, appendices, and revision history.
- Use opaque synthetic identifiers rather than realistic sensitive values.
- Carry `tenant_id` through persistence and retrieval boundaries.
- Avoid sensitive data in prompts, logs, traces, exceptions, and audit details.
- Use least-privilege IAM roles when AWS integration is deployed.

## Responsible-AI constraints

- Reports must conform to an application-owned schema.
- Every observation and inference must be traceable to evidence identifiers.
- Store model identifier, prompt/template version, generation timestamp, and
  retrieval context identifiers.
- Preserve enough immutable document, extraction, and source-location metadata
  to trace each future PDF-derived chunk back to the exact synthetic source
  version.
- A schema-valid report can still be wrong; the UI must communicate this.
- Missing or contradictory evidence must reduce confidence, not invite
  fabrication.
- No generated recommendation may execute automatically.
- Operator approval and rejection are explicit, attributable events.

## Scope-change rule

When a task appears to require expanding these constraints, stop and document
the proposed decision and tradeoff before implementation.
