# ADR-0017: Require insufficient-evidence reports for degraded observations

Status: Accepted
Date: 2026-09-30
Decision owner: Christopher Guzowski

## Context

Q5 measured schema-valid PROPOSED reports for partial S111 and unavailable S211.
Both asserted causes and recommendations at MEDIUM confidence. Citation membership
and model-selected disposition did not enforce current evidence sufficiency.
The owner activated Q6 to correct these failures.

## Decision

Use observable persisted report context, never scenario identifiers or oracle
labels, to require INSUFFICIENT_EVIDENCE when latestStatus is not AVAILABLE or
the evidence snapshot contains no observations. This is a conservative necessary
condition for a proposed conclusion, not a general proof of sufficient evidence.

The context-constrained provider schema requires INSUFFICIENT_EVIDENCE, LOW,
null probableCause/recommendation and at least one evidence gap. report-prompt/v5
explains the same rule and prohibits relocating unsupported conclusions into
other prose fields. The base report-v1 schema remains unchanged; exact constrained
schema and prompt hashes remain persisted for every attempt.

Independent application parsing verifies the context rule in addition to existing
schema, LOW/null, citation and conditional checks. Invalid output remains MALFORMED
with one model call, no repair, no automatic retry and no incident transition.
Compliant reports enter AWAITING_REVIEW under the existing atomic persistence rule.

Earlier applicable observations after a degraded retry remain immutable and
eligible for citations but cannot restore the latest evidence status. No historical
report is rewritten. AVAILABLE evidence with observations retains the existing
PROPOSED and INSUFFICIENT_EVIDENCE contracts.

## Alternatives and tradeoffs

Prompt-only remediation would still trust model-selected sufficiency. Output
repair would disguise the actual model response. Provider constraints plus
independent fail-closed validation make the safety rule inspectable and testable.

All partial evidence is handled conservatively because the current snapshot lacks
a separate measured sufficiency assessment. This may withhold useful conclusions
from some partial inputs; a finer policy needs separately authorized evidence and
validation contracts. The check cannot detect unsupported causes hidden in arbitrary
natural language or establish semantic entailment for AVAILABLE reports.

## Verification

Parameterized deterministic prompt/parser and PostgreSQL HTTP tests cover degraded
statuses, empty observations, compliant reports, invalid output, source membership,
one-call history and unchanged sufficient-evidence behavior. Live S111/S211 checks
and the repository gate are recorded in the Q6 task and retained evaluation results.
