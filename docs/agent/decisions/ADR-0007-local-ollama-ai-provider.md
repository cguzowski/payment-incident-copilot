# ADR-0007: Use Ollama for local AI development

Status: Accepted
Date: 2026-08-30
Decision owner: Christopher Guzowski

## Context

The repository needs a repeatable local path for embedding and report
development without depending on expiring AWS credentials
or a network model provider. Automated tests must remain deterministic and
offline. The existing approved-knowledge index also contains auditable Titan
V2 metadata and 1,024-dimensional vectors that must survive the transition.

## Decision

Use Spring AI as the provider boundary with these local defaults:

| Function | Local choice |
|---|---|
| Report generation | Ollama `qwen3.5:4b`, temperature `0` |
| Embeddings | Ollama `nomic-embed-text`, normalized 768 dimensions |
| Vector storage/search | PostgreSQL with pgvector |
| Orchestration | Spring AI |
| Tool integration | Repository Spring MCP server |

The copilot API uses `spring-ai-starter-model-ollama`, connects to
`http://localhost:11434`, and never pulls models automatically. The completed
report feature uses the same Spring AI boundary with Ollama chat options. This
ADR supersedes the provider-specific local-development portion of ADR-0006; it
does not change ADR-0006's report schema, validation, citation, persistence, or
human-review controls.

Automated tests disable both Spring AI model providers and use mocked or
deterministic model responses. Live Ollama checks are explicit developer
actions and are never part of the normal automated test contract.

Flyway V6 remains the immutable provider-neutral report-persistence migration
already present in the preserved production branch. Flyway V7 changes the
pgvector column from a fixed 1,024-dimensional type to a
dimension-validated vector that permits either the current `nomic-embed-text`
768-dimensional contract or the historical Titan V2 1,024-dimensional
contract. Stored model ID, declared dimensions, actual vector dimensions, and
normalization must agree. Vector scoring filters candidates to the query
embedding's model ID and dimensions; eligible historical chunks remain
available to lexical retrieval.

Do not automatically delete or re-embed historical rows. A developer who needs
complete local semantic coverage uses a fresh synthetic database or creates a
new document version and runs explicit ingestion.

Amazon Bedrock is preserved as an optional production profile to evaluate and
implement near the deployment milestone. It is not an active dependency,
fallback, credential requirement, or profile in local development.

## Consequences

### Positive

- Local AI work does not consume cloud credentials or model-invocation cost.
- Tests are stable, fast at the provider boundary, and independent of live
  model availability.
- Spring AI keeps application orchestration separate from the provider choice.
- Historical Titan retrieval metadata remains auditable through the migration.

### Negative or accepted tradeoffs

- Developers must install Ollama and pull both pinned models themselves.
- Live model behavior is environment-dependent and requires an explicit smoke
  check outside automated verification.
- A mixed historical index cannot compare vectors across model/dimension pairs;
  older chunks contribute lexically until they are explicitly re-indexed.
- The later Bedrock production profile will require its own dependency,
  configuration, least-privilege identity, provider tests, and ADR update.

## Revisit trigger

Revisit the local models when retrieval or report-quality evaluation shows they
are insufficient, when a model upgrade changes dimensions or behavior, or when
deployment work begins and the optional Bedrock profile can be evaluated with
measured security, availability, latency, and cost requirements.
