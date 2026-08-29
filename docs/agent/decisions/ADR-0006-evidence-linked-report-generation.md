# ADR-0006: Evidence-linked structured report generation

Status: Accepted
Date: 2026-08-29
Decision owner: Christopher Guzowski

## Context

The copilot now has immutable tenant-scoped incident, evidence, and approved-
knowledge retrieval inputs. The next slice must turn them into a reviewable AI-
assisted report without allowing model output to become unsourced fact, silently
refreshing context, reaching through feature storage, or implying human approval.

The design must also select a Bedrock chat model and decide how schema
conformance is enforced. The configured source region is `eu-central-1`, and
the owner selected the global Nova 2 Lite inference profile. Its current model
card supports Bedrock Converse but does not list Bedrock native structured
outputs as supported.

## Decision drivers

- Every conclusion traceable to exact persisted evidence
- Clear separation of observation, inference, approved guidance, and decision
- Reproducible input, prompt, schema, model, and generation metadata
- Strict application-owned parsing, schema, semantic, and citation validation
- Tenant-safe feature ownership and immutable attempt history
- Bounded MVP cost and complexity
- Explicitly accepted global inference routing for synthetic-only inputs

## Considered options

### Prompt-only JSON through a general chat model

- Advantages: Broad model compatibility and minimal provider-specific options.
- Disadvantages: JSON and schema compliance remain best-effort, increasing
  parser retries and the chance of malformed or partially validated output.

### Bedrock Converse native structured output with an eligible Claude model

- Advantages: Provider-constrained JSON Schema and Spring AI 2.0 support.
- Disadvantages: Higher cost than the selected Nova 2 Lite default and a model
  choice the owner did not select.

### Nova 2 Lite with prompt-guided JSON and application validation

- Advantages: Cost-efficient active Amazon model, Bedrock Converse support,
  owner-selected global inference profile, and a configurable model boundary.
- Disadvantages: The current model card does not list Bedrock native structured
  outputs as supported. Prompt conformance is best-effort, and global inference
  does not provide geography-bound data residency.

### Nova 2 Lite through strict tool use

- Advantages: Tool input could carry structured fields.
- Disadvantages: The model card lists client-side tool calling but does not
  establish strict tool-schema enforcement for this model. Tool calling is not
  otherwise needed for report generation and would add an unnecessary control
  path.

## Decision

Create a `report` feature inside the existing copilot API. It depends only on
published tenant-scoped report snapshots from `incident`, `evidence`, and
`knowledge.retrieval`. Its persistence adapter owns only report-generation
attempts, validated report content, and normalized source references; it does
not query another feature's tables.

Use the newest terminal evidence attempt plus the newest earlier applicable
`AVAILABLE` or `PARTIAL` evidence attempt. Use only the newest terminal
knowledge-retrieval attempt. Preserve their exact identifiers and status in the
report attempt. Do not rerun MCP collection or retrieval, and do not silently
substitute older knowledge after a failed retry.

Use an application-owned `report-v1` JSON Schema with typed, bounded claims. It
separates summary, observations, inferences, probable cause, confidence,
recommendation, contradictions, and evidence gaps. Every conclusion cites at
least one exact evidence identifier; a recommendation also cites an exact
selected approved-knowledge chunk. The application validates conditional rules
and resolves provenance from its persisted inputs rather than accepting
model-provided excerpts or metadata.

Use Spring AI 2.0's Bedrock Converse integration with the configurable default
`${BEDROCK_CHAT_MODEL:global.amazon.nova-2-lite-v1:0}`. Global cross-Region
inference is accepted for this synthetic-only MVP; it does not make a future
data-residency claim.

Do not request Bedrock native structured output for Nova 2 Lite. Render the
immutable application-owned `report-v1` JSON Schema into the versioned prompt,
require one JSON object with no preamble, and use deterministic bounded
settings with temperature `0`, no model tool calls, and no persisted reasoning
trace. Perform exactly one model call per report attempt. Strictly parse the
result as JSON and independently validate the JSON Schema, conditional report
rules, source membership, tenant ownership, and citation semantics. A parse,
schema, or semantic failure records `MALFORMED`; only an explicit operator retry
may make another model call.

An operator explicitly starts generation. Persist `STARTED` before model I/O,
perform the call outside a transaction, and append a safe terminal result. A
validated report and the application-owned transition to `AWAITING_REVIEW`
commit atomically. Model content never selects a workflow state or executes a
recommendation.

Treat an explicit `INSUFFICIENT_EVIDENCE` result as a reviewable report: it has
low confidence, preserves gaps and degraded statuses, and contains neither a
probable cause nor a recommendation. This still moves to `AWAITING_REVIEW`
because the operator must review the copilot's documented inability to conclude;
it is not a successful root-cause assertion.

## Rationale

This design makes the AI step inspectable instead of treating a JSON response
as trustworthy. Nova 2 Lite receives the exact application schema and greedy
generation instructions, while strict application validation remains the
authority for syntax, shape, tenant ownership, source membership, conditional
rules, and citation semantics. Recording malformed output without an automatic
repair loop keeps the number and provenance of model calls unambiguous.

Snapshot ports preserve B01-B06 ownership and make a report reproducible from
the exact state the operator selected. Explicit `INSUFFICIENT_EVIDENCE` avoids
incentivizing fabrication and gives the next human-decision slice one coherent
review state.

Nova 2 Lite is selected because it is an active cost-efficient Amazon model and
the owner prefers its global inference profile for this synthetic-only slice.
The environment expression keeps the exact deployed model configurable while
making the selected default reproducible.

## Consequences

### Positive

- Every displayed conclusion has deterministic, tenant-safe source provenance.
- Model, prompt, schema, input snapshot, operator, and timing metadata remain
  available for audit and later evaluation.
- Unavailable or contradictory inputs can reduce confidence or produce an
  explicit no-conclusion report without blocking human review.
- The report feature cannot couple itself to foreign persistence records.
- Retries and interrupted calls remain append-only and reviewable.

### Negative or accepted tradeoffs

- Global inference may route outside Europe and is acceptable only because the
  MVP inputs are synthetic; future real or residency-constrained data requires
  a new decision.
- Nova 2 Lite does not provide Bedrock native structured-output enforcement, so
  malformed model responses remain an expected, tested terminal outcome.
- The application owns strict JSON, schema, semantic, and citation validation.
- The first report schema is deliberately specific to one incident family and
  evidence type; it is not a generic report engine.
- A successful report prevents regeneration until a later decision workflow,
  keeping one unambiguous review candidate at the cost of less experimentation.

## Validation or revisit trigger

Revisit the model when authorized access, measured report quality, malformed
rate, latency, lifecycle, cost, or routing makes Nova 2 Lite unsuitable, or when
native structured output becomes available and materially improves reliability.
Revisit global routing before any non-synthetic or residency-constrained data is
allowed. Revisit the input and claim schema when a repeatable report-quality
evaluation shows that the current evidence tool or retrieval context cannot
support the selected incident family. Revisit feature/deployment boundaries
only when measured ownership or scaling needs justify more than the existing
copilot API deployable.

## Provider references reviewed

- [Amazon Bedrock Nova 2 Lite model card](https://docs.aws.amazon.com/bedrock/latest/userguide/model-card-amazon-nova-2-lite.html)
- [Amazon Nova structured-output guidance](https://docs.aws.amazon.com/nova/latest/userguide/prompting-structured-output.html)
- [Spring AI Bedrock Converse](https://docs.spring.io/spring-ai/reference/api/chat/bedrock-converse.html)
