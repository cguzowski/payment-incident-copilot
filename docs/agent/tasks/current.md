# Task: Restore approved knowledge for additional incident families

Status: Complete - live retrieval recovery and Repository verification passed
Created: 2026-10-01
Owner: Christopher Guzowski

## Goal

Fix the reported NO_MATCH retrievals in the configured local application.

## User story

As an operator, I want approved guidance for the new incident families available
in my current database so I can continue triaging existing investigations.

## Chosen contract

The owner requested recovery of approved-knowledge retrieval. Read-only diagnosis
found only 30 authorization-decline PDF documents / 705 chunks, with no additional
family guidance. Existing webhook investigation 9b5daa6f-fed5-4bff-8f38-604ae86d676e
and reconciliation investigation 94deb248-9dea-44e6-bcbb-d22ca6dce5d0 have persisted
NO_MATCH attempts. Explicitly prepare the unchanged approved Markdown sources
using the configured local database and Ollama embedding model, then start the
existing API/generator and retry retrieval through product HTTP boundaries.
Preserve prior attempts, incidents, reports and human decisions.

## In scope

Local index preparation, restarting stopped local API/evidence services, retries
of the two affected investigations, provenance/coverage verification, documentation.

## Out of scope

Retrieval ranking/filter changes, source/version changes, automatic startup imports,
model tuning, report generation or human decisions, new infrastructure.

## Constraints

Synthetic data only. Keep tenant/family/approval/effective-time filters intact.
Do not delete failed attempts or fabricate results. Never print local credentials.
Use existing explicit knowledge preparation; no production behavior change is
needed if the prepared index restores retrieval.

## Acceptance criteria

- [x] Persisted NO_MATCH reproduced and missing family index coverage established.
- [x] Existing approved sources import successfully with complete embeddings and
      two approved documents per additional family; original PDF catalog unchanged.
- [x] Both affected investigations retrieve nonempty matching approved knowledge
      through the running API, with immutable prior NO_MATCH history retained.
- [x] Source/model metadata and unchanged source hashes verified; API and generator
      healthy; Repository documentation/static checks pass.

## Test plan

Read-only SQL proves current family coverage and failed attempts. Invoke existing
KnowledgeIngestionCommand with chat disabled, web application disabled and the
configured local nomic embedding provider. Verify family document/chunk counts,
embedding metadata and original 30 PDF documents/705 chunks. Retry the two existing
investigations with synthetic tenant/operator HTTP headers, inspect source families,
versions, line/hash provenance, retained histories and unchanged incident/decision
state. No executable behavior changes or artificial new tests; run Repository gate.

## Progress notes

- Preserved prior completed task and all pre-existing working-tree changes.
- API and generator were not listening; native PostgreSQL and Ollama were running.
- Installed PostgreSQL 18 client readback confirmed only authorization-decline
  knowledge. Both recent new-family attempts have NO_MATCH.

## Completion evidence

- Diagnosis: only AUTHORIZATION_DECLINE_RATE_SPIKE had indexed knowledge:
  30 PDF documents / 705 chunks. Webhook had two NO_MATCH attempts and
  reconciliation one; no new-family guidance existed.
- Existing explicit import command succeeded on 2026-10-01 with the configured
  local database, SPRING_MAIN_WEB_APPLICATION_TYPE=none,
  SPRING_AI_MODEL_CHAT=none, SPRING_AI_MODEL_EMBEDDING=ollama,
  APP_KNOWLEDGE_INGESTION_ENABLED=true and PDF import/backfill disabled:
  ./mvnw.cmd -pl backend/copilot-api spring-boot:run. It imported 14 documents
  and embedded 87 chunks. Credentials were loaded from ignored .env without output.
- PostgreSQL verification: original 30 PDF documents / 705 embeddings retained;
  each additional family has two Markdown documents and 13 embedded chunks.
  Two legacy Markdown documents add nine chunks. All use nomic-embed-text,
  768 dimensions. All fourteen package source hashes still match the manifest.
- Started the already-built generator and API from temporary jar copies to
  avoid locking target output. API uses generator MCP at localhost:8082.
  Both health endpoints are UP. Angular remains available at localhost:4200.
- Live webhook retry: investigation 9b5daa6f-fed5-4bff-8f38-604ae86d676e,
  retrieval 6febe78b-4c46-4b81-b1ed-ea920987f7dc, AVAILABLE with seven chunks,
  version 1.0.0 runbook/policy, exact Markdown line locators. Three retained
  attempts: AVAILABLE, NO_MATCH, NO_MATCH.
- Live reconciliation retry: investigation 94deb248-9dea-44e6-bcbb-d22ca6dce5d0,
  retrieval 8046adbf-3a00-4673-a602-b96fca3e6d2b, AVAILABLE with seven chunks,
  version 1.0.0 runbook/policy, exact Markdown line locators. Two retained
  attempts: AVAILABLE, NO_MATCH.
- Both use nomic-embed-text query embeddings and postgres-hybrid-related/v4.
  Exact evidence bindings retained: webhook 5fe0685a-eded-45fe-b2a2-31c3eebcf718;
  reconciliation 3196aa52-669e-42e6-a243-c8c3fbc239da. Both remain INVESTIGATING
  with zero human decisions. No reports or operational actions were requested.
- No production code, ranking, filters, corpus/source versions or dependencies
  changed in this recovery. Live before/after verification covers this local
  preparation failure; no artificial automated tests or new full gate required.
  Final ./verify.ps1 -Scope Repository passed, including preparation/evaluator
  tests, Compose validation and git diff --check.

## Remaining limitations

Live report quality is outside this retrieval recovery. New/reset databases and
future source additions still require explicit knowledge preparation. The API and
generator are left running locally. Existing dependency/model limitations from
prior completed tasks are unchanged.

## Decisions needed

None; the owner request explicitly authorizes preparing knowledge to fix retrieval.
