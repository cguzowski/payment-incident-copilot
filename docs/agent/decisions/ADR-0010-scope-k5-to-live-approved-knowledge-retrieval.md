# ADR-0010: Scope K5 to live approved-knowledge retrieval

Status: Accepted  
Date: 2026-09-01  
Decision owner: Christopher Guzowski

## Context

K4 populated all 705 stable PDF chunks with live normalized
`nomic-embed-text` vectors and retained a factual fixed-threshold FAIL artifact.
The next product risk is whether the real operator action can retrieve and show
eligible approved PDF guidance. `nomic-embed-text` is an embedding model and
cannot generate report text, so treating it as a chat-model replacement would
create an invalid provider contract.

## Decision drivers

- Prove the user-visible approved-knowledge path before expanding live report
  generation.
- Keep one downloaded local model and the verified 768-dimensional index
  contract for K5.
- Preserve K4's artifact, labels, eligibility, provenance, and audit evidence.
- Keep automated verification network-free and live work explicit.

## Considered options

### Option A: Use live `nomic-embed-text` retrieval and defer live reports

- Advantages: uses the verified embedding contract, targets the observed
  no-match product outcome, and does not misuse an embedding-only model.
- Disadvantages: live report generation remains unproved.

### Option B: Select a separate live chat model during K5

- Advantages: could exercise retrieval and report generation together.
- Disadvantages: adds a second model and a separate quality decision before the
  approved-knowledge UI path itself is proven.

## Decision

K5 uses Ollama `nomic-embed-text`, normalized to 768 dimensions, for query and
catalog embeddings. Live K5 runs disable chat-model startup. The required
end-to-end outcome is that, for an accepted repeatable synthetic investigation,
clicking **Retrieve approved knowledge** returns and displays at least one
eligible approved PDF excerpt with its immutable citation instead of the
**No eligible approved knowledge matched this investigation.** state.

K5 may diagnose and improve retrieval only under its own accepted task contract.
It must retain the K4 FAIL artifact and may not silently weaken labels,
eligibility, provenance, tenant isolation, or human-review boundaries. Live
chat/report-model selection and report-quality evaluation are deferred to a
later decision.

This decision supersedes ADR-0007 only for the future K5 live chat-model scope.
ADR-0007's `nomic-embed-text` embedding, pgvector, Spring AI, offline-test, and
historical-vector compatibility decisions remain accepted. Existing report
schema, persistence, deterministic tests, and human-review controls remain
unchanged.

## Rationale

The operator-visible empty result is the immediate product failure, and K4 has
already produced the ranks and diagnostics needed to investigate it. Separating
retrieval proof from chat-model selection keeps K5 measurable and avoids
claiming that an embedding model can generate reports.

## Consequences

### Positive

- K5 has one observable product acceptance outcome and one live model.
- Retrieval changes remain comparable with the retained K4 baseline.
- The existing report feature remains deterministic and review-controlled.

### Negative or accepted tradeoffs

- K5 will not prove live report generation.
- A later report milestone must select and evaluate a compatible chat model.
- Passing one operator scenario will not by itself erase the broader K4
  threshold misses; K5 must record both UI evidence and evaluation results.

## Validation or revisit trigger

Revisit this decision after K5 proves the operator retrieval outcome and records
the post-change fixed evaluation, or earlier if an accepted product requirement
explicitly requires live report generation in the same milestone.
