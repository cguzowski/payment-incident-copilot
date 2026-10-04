# ADR-0023: Bound confidence for aggregate-only report evidence

Status: Superseded by [ADR-0024](ADR-0024-attainable-high-confidence.md)
Date: 2026-10-04
Decision owner: Christopher Guzowski

## Context

The owner activated O1 after the five-case diagnostic found HIGH confidence
based on aggregate service errors and approved guidance without independent
causal confirmation. ReportEvidenceSnapshot represents a single service-error
source. Error categories, large counts and historical attempt IDs do not make
that source independent. Approved knowledge is guidance, not incident evidence.

## Decision

report-prompt/v6 defines confidence in the probable cause rather than error
severity or volume. MEDIUM requires observations and guidance supporting a
bounded cause/advisory step while confirmation remains missing. LOW remains
available for weak, ambiguous or conflicting support; lack of a supported cause
and step still requires INSUFFICIENT_EVIDENCE under the existing contract.
Rationale instructions require observed support and missing independent
operational confirmation, without inventing additional missing sources.

The per-context provider schema allows LOW/MEDIUM for nonempty AVAILABLE
aggregate evidence. ADR-0017's degraded/empty LOW/null/gap constraints remain.
Independent report parsing rejects HIGH even if the provider ignores its schema.
Violations follow the existing MALFORMED path: one call, no repair, no automatic
retry and no review-state transition. Exact prompt/schema hashes remain stored.

The base report-v1 schema and historical HIGH reports remain unchanged and
readable. No oracle, scenario identifier, expected confidence label or judge
result enters confidence selection. Comparison rubric, judge prompt, corpus,
retrieval, model and output/time/context budgets remain unchanged.

## Consequences

The ceiling follows the current evidence contract; it does not force MEDIUM or
prove that every MEDIUM cause is supported. Rationale prose is still generated,
and bounded validation does not establish its semantic correctness. HIGH can
return only through a separately designed input/validation contract representing
independent, claim-specific operational corroboration. Adding more error codes
or historical snapshots alone must not relax the ceiling.

Provider constraints guide valid generation; independent rejection prevents
quietly rewriting the model's confidence. This may increase MALFORMED failures,
which must be retained and measured in live evaluation rather than retried away.

## Verification

O1's current task retains red/green prompt/parser tests, PostgreSQL HTTP failure
and historical readback tests, full-gate evidence and fresh live comparisons.
