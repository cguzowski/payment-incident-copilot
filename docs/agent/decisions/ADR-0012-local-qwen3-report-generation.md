# ADR-0012: Use local Qwen3 8B for advisory report generation

Status: Accepted  
Date: 2026-09-24  
Decision owner: Christopher Guzowski

## Context

The local evidence and approved-knowledge paths were live, but report attempts
were always `UNAVAILABLE` because Spring AI chat was intentionally disabled.
The owner installed Ollama `qwen3:8b-q4_K_M` and selected an entirely local
report path.

## Decision drivers

- Keep synthetic incident context on the local workstation.
- Reuse the accepted Ollama/Spring AI boundary.
- Preserve strict application-owned schema, citation, lifecycle, and review
  controls.
- Keep automated verification deterministic and provider-free.

## Considered options

### Option A: Local `qwen3:8b-q4_K_M`

- Advantages: already installed, no hosted-data transfer, sufficient structured
  output when constrained by the report context.
- Disadvantages: generation takes roughly one to two minutes on the current
  workstation and requires an explicit local model preflight.

### Option B: Hosted chat model

- Advantages: potentially lower latency and stronger instruction following.
- Disadvantages: introduces credentials, cost, network dependency, and a new
  data-transfer boundary.

## Decision

Local report generation uses Ollama `qwen3:8b-q4_K_M`. Each request uses
temperature zero, at most 1,536 output tokens, disabled thinking, no tools, and
the application-owned `report-v1` schema. The provider schema is narrowed per
request to the exact persisted evidence and approved-knowledge identifiers;
the independent parser and semantic validator remain authoritative.

The launcher verifies Ollama plus both `qwen3:8b-q4_K_M` and
`nomic-embed-text` before service startup and never pulls models. Automated
tests disable live AI providers.

This supersedes ADR-0007 and ADR-0010 only where they deferred the live report
model. Their embedding, retrieval, offline-test, and human-review decisions
remain accepted.

## Rationale

Native structured output alone does not distinguish one UUID role from another.
Narrowing the provider schema to the exact report context prevents the local 8B
model from mixing evidence and knowledge identifiers, while strict application
validation still fails closed rather than repairing or inventing citations.

## Consequences

### Positive

- The one-click local S001 workflow reaches a reviewable, attributable report.
- No cloud provider, credentials, or fallback is required.
- Model, prompt, constrained-schema, evidence, and retrieval provenance remain
  auditable.

### Negative or accepted tradeoffs

- Current local latency is about 76 seconds for the accepted S001 report.
- Ollama and both pinned model tags must already be installed.
- One successful scenario is not a broad report-quality benchmark.

## Validation or revisit trigger

Revisit when broader scenario evaluation shows unacceptable validity or report
quality, local latency exceeds the two-minute attempt deadline, the pinned
model changes, or a production-provider decision is accepted.
