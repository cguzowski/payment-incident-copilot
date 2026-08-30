# Task: Use Ollama for local AI development

Status: Completed
Created: 2026-08-30
Owner: Christopher Guzowski

## Goal

Make Ollama the active local-development AI provider while keeping automated
tests deterministic and network-free, preserving PostgreSQL/pgvector retrieval,
Spring AI orchestration, and the Spring MCP server. Keep Amazon Bedrock
preserved as a future optional production profile rather than an active local
dependency.

The owner authorized implementation and selected the local models on
2026-08-30. This is the active, locked behavioral contract.

## User story

As the repository maintainer, I want report and embedding development to run
against local Ollama models while automated tests use mocks, so development is
repeatable without AWS credentials and the provider can be changed through
Spring AI configuration.

## Chosen contract

| Function | Local choice |
|---|---|
| Report generation | Ollama with `qwen3.5:4b` |
| Embeddings | Ollama with `nomic-embed-text` |
| Vector storage/search | PostgreSQL with pgvector |
| Orchestration | Spring AI |
| MCP tools | Repository Spring MCP server |

- Local runtime defaults to Ollama at `http://localhost:11434`, chat temperature
  `0`, and explicit chat and embedding model names.
- Automated tests disable provider auto-configuration and use mocked or
  deterministic model responses. They never require a running Ollama or AWS
  service.
- The active copilot API dependency is
  `spring-ai-starter-model-ollama`; no active Bedrock model dependency or
  Bedrock runtime configuration remains on this branch.
- `nomic-embed-text` embeddings use a validated 768-dimensional normalized
  contract. pgvector continues to preserve exact vector search and auditable
  model/dimension metadata.
- The V7 forward migration preserves historical 1,024-dimensional Titan metadata
  while allowing the local 768-dimensional index. Vector comparison considers
  only chunks produced by the same model and dimension as the query.
- The completed report slice is retained on this branch. Its Spring AI adapter
  uses the local Ollama chat model without changing the locked report schema,
  citations, persistence, lifecycle, HTTP, or UI behavior.
- Bedrock is documented as an optional production profile to add during the
  deferred deployment phase, after the local closed loop is complete. It is not
  implemented in this task.

## In scope

- Copilot API Maven model dependency and Spring AI configuration.
- Provider-neutral report-model adapter and smoke wording for the existing
  report feature.
- Ollama embedding adapter/model contract and provider-neutral smoke wording.
- A forward Flyway migration and compatible vector-search filtering.
- Deterministic test-provider isolation and mocked embedding behavior.
- Local Ollama setup, architecture, roadmap, decision, quality, and status
  documentation.

## Out of scope

- Changing the implemented report schema, persistence, HTTP/lifecycle contract,
  or report UI.
- Installing Ollama or downloading model artifacts automatically.
- Adding an active Bedrock dependency/profile or using AWS credentials.
- Changing MCP tool behavior, PostgreSQL as the data store, hybrid RRF ranking,
  public HTTP contracts, or frontend behavior.
- Destructive automatic deletion or re-embedding of the local knowledge index.

## Constraints

- Follow red-green-refactor for executable behavior.
- Do not rewrite Flyway V1-V5.
- Preserve historical model and retrieval metadata.
- Keep normal tests independent from Ollama, Bedrock, and external networks.
- Keep AI output advisory and preserve evidence/inference boundaries.
- Use synthetic data only and never add credentials.

## Acceptance criteria

- [x] The copilot API uses the Spring AI Ollama starter and has no active
      Bedrock model starter.
- [x] Default local configuration selects Ollama chat and embeddings,
      `qwen3.5:4b`, `nomic-embed-text`, temperature `0`, and
      `http://localhost:11434`.
- [x] The existing report feature invokes `qwen3.5:4b` through Ollama options,
      retains its deterministic bounded settings, and has no active Bedrock
      model dependency.
- [x] Automated tests explicitly disable chat and embedding provider
      auto-configuration and model-facing tests use mocks or deterministic
      doubles.
- [x] The embedding adapter validates and records
      `nomic-embed-text`, 768 dimensions, normalization, malformed output,
      provider unavailability, and timeout behavior.
- [x] Flyway V7 preserves historical Titan metadata and permits validated
      768-dimensional local embeddings without rewriting V5.
- [x] Vector scoring excludes chunks with a model or dimension incompatible
      with the current query while lexical fallback remains available.
- [x] PostgreSQL/pgvector, hybrid RRF retrieval, Spring AI orchestration, and
      the Spring MCP server remain in place.
- [x] Documentation explains Ollama installation/model prerequisites, local
      flow, test isolation, clean re-index expectations, and deferred optional
      Bedrock production-profile work.
- [x] Focused tests and the authoritative repository verification pass with zero
      skipped tests.

## Test plan

- `usesOllamaForLocalRuntimeAndDisablesProvidersInTests`
- `callsOllamaOnceWithDeterministicBoundedOptionsAndNoTools`
- `validatesOnePromptGuidedOllamaReportWithoutPersistence`
- `requestsAndValidatesNormalizedNomicEmbedding`
- `rejectsWrongDimensionNonFiniteOrUnnormalizedOutput`
- `storesNomicEmbeddingAndPreservesHistoricalTitanContract`
- `scoresOnlyVectorsFromTheQueryModelAndDimension`
- Existing mocked embedding and retrieval failure tests.
- Full backend and aggregate repository verification.

## Validation commands

```powershell
./mvnw.cmd -pl backend/copilot-api -Dtest=AiModelConfigurationTest,SpringAiOllamaKnowledgeEmbeddingClientTest test
./mvnw.cmd -pl backend/copilot-api -Dtest=KnowledgeSchemaPostgresIntegrationTest,KnowledgeHybridSearchPostgresIntegrationTest test
./verify.ps1
```

## Decisions needed

None. The owner selected the local provider, models, storage/search,
orchestration, MCP boundary, testing strategy, and deferred Bedrock direction.

## Progress notes

- 2026-08-30: Confirmed the current branch is clean and starts exactly at
  `origin/main`, before the separately preserved Bedrock implementation.
- 2026-08-30: Confirmed Spring AI 2.0 documents the Ollama starter and the
  `spring.ai.model.chat`, `spring.ai.model.embedding`,
  `spring.ai.ollama.chat.*`, and `spring.ai.ollama.embedding.*` properties.
- 2026-08-30: Ollama is not installed in the current task environment, so live
  local-model verification is not available during this task.
- 2026-08-30: Replaced the Bedrock starter and active configuration with the
  Ollama starter and pinned local chat/embedding defaults. Test resources now
  disable both model providers.
- 2026-08-30: Added the mocked Ollama embedding boundary and Flyway V7. The
  migration retains historical Titan rows while validating the new normalized
  768-dimensional contract.
- 2026-08-30: Vector ranking now filters candidates by the query embedding's
  model and dimensions; incompatible historical chunks remain eligible for
  lexical ranking.
- 2026-08-30: Recorded the local-provider decision in ADR-0007 and aligned the
  local setup, project, constraints, architecture, quality, roadmap, and status
  documents.
- 2026-08-30: Reopened after local startup exposed that the preserved report
  implementation already owns Flyway V6. The owner approved restoring that V6
  unchanged and moving the Ollama vector migration to V7.
- 2026-08-30: Restored the original provider-neutral report-persistence V6 at
  checksum `-54318256`, moved the unchanged Ollama migration to V7 at checksum
  `1491731562`, and added a regression assertion for both history entries.
- 2026-08-30: The existing native PostgreSQL 18.3 database validated V1-V7,
  migrated from V6 to V7 without repair or reset, and completed a non-web
  copilot API startup.
- 2026-08-30: Recovered the completed P2 report implementation, retained its
  V6 checksum and UI/API contracts, and adapted only its provider boundary from
  Bedrock-specific options to local Ollama options through a failing regression
  test followed by the focused green suite.
- 2026-08-30: Applied the recovered report snapshot to `main` and this local
  Ollama branch. The active AWS Bedrock branch already contained the identical
  report UI, API, persistence, prompt, and validation implementation.

## Completion evidence

- Red-phase evidence: the focused configuration/adapter suite first failed to
  compile because the Ollama adapter did not exist; the 768-dimensional schema
  test then failed against V5's 1,024-dimensional constraint; and the search
  test failed to compile until model/dimension metadata became part of the
  query contract. The migration-history regression then failed because V6 had
  the Ollama checksum instead of the already-applied report checksum and V7 was
  absent.
- Green-phase evidence: 25 focused configuration, mocked adapter, ingestion,
  Flyway, hybrid-search, retrieval-service, persistence, controller, and API
  tests passed with zero failures, errors, or skips.
- Verification: `./verify.ps1` passed 150/150 copilot API tests, 9/9 operations
  MCP server tests, and 53/53 Angular tests with zero skips, plus Spotless,
  Prettier, the Angular production build, Compose validation, and
  `git diff --check`.
- Manual verification: the Spring AI dependency tree contains the MCP client
  and `spring-ai-starter-model-ollama` with no Bedrock model starter. A
  sensitive-name scan found no AWS bearer token or credential value in the
  project changes. Native PostgreSQL 18.3 validated the immutable V6 checksum,
  applied V7, and started the application successfully without invoking
  `flyway repair`.
- Remaining limitations: Ollama and the pinned models are not installed in this
  task environment, so the documented live embedding/report smoke and import
  commands were not run. The optional Bedrock production profile remains
  deferred.
