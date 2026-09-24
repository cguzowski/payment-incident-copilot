# Product roadmap

Status: Active
Last reviewed: 2026-09-24
Owner: Christopher Guzowski

## Purpose

Preserve the completed single-tenant authorization-decline workflow while
measuring and improving its quality. This file orders outcomes; only an
owner-activated [task contract](tasks/current.md) authorizes implementation.

## Completed milestones

| Milestone | Outcome |
|---|---|
| E1 / B01-B06 | Shared local/CI verification and explicit codebase boundaries |
| P1-P4 | Approved knowledge, advisory reports, human decisions, and audit workflow |
| K1-K3 | Synthetic profile, 30-document PDF corpus, and page-aware catalog |
| K4-K5 | Live embeddings, fixed retrieval evaluation, and operator retrieval proof |
| R1 | Generated incidents connected to their matching MCP evidence |
| R2 | Local Qwen S001 report validated and persisted for review |

K4/K5 completion did not pass the benchmark. See
[STATUS.md](STATUS.md) for current evidence limitations and
[SynTen Inc](../../SynTen%20Inc/README.md) for recorded results.

## Ordered future outcomes

| Order | Outcome | Completion boundary |
|---|---|---|
| Q1 — Next | Evaluation integrity | Isolate the answer key from evaluated evidence, retrieval, reports, and decision inputs until outputs are frozen; prove an auditable reveal boundary. |
| Q2 | Retrieval-quality disposition | Pass the unchanged benchmark or record explicit owner acceptance of the measured failure and consequences. |
| Q3 | Automated report grading | Retain reproducible correctness, citation, unsupported-claim, latency, and failure metrics across the scenario oracle. |
| Q4 | Complete live-model audit proof | Verify a new live report's explicit human decision, terminal state, and full audit timeline. |
| Q5 | Broader live-model coverage | Exercise common, uncommon, rare, partial-evidence, and unavailable-evidence scenarios. |
| D1 — Deferred | AWS deployment | Select services, tooling, networking, IAM, cost, teardown, and any Bedrock profile in an ADR before implementation. |
| D2 — Deferred | Authentication | Select identity and authorization and enforce tenant/operator access at every public boundary. |

Q1 has not been activated. Its contract must account for the current browser
answer-key exposure and oracle-derived corpus text described in STATUS.
The current documentation task does not authorize executable Q1 work.

## Sequencing rules

- Treat the v1 corpus and evaluation labels as fixed inputs. Corpus changes
  require an approved version task; keep source/PDF hashes and prior evidence.
- Keep generation, ingestion, embedding, and evaluation separately verifiable.
- Version parser, chunker, query, ranking, and model changes explicitly.
- Keep tests independent of live AI providers. Record live evaluation separately.
- Preserve exact PostgreSQL hybrid retrieval until measurements justify change.
- Keep human decisions mandatory and prohibit model-executed recommendations.
- Revisit ordering when measured retrieval/report quality, latency, or corpus
  scale invalidates the current approach.
