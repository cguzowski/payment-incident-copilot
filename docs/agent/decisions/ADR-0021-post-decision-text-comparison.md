# ADR-0021: Automatic post-decision text comparison

Status: Accepted
Date: 2026-10-01
Decision owner: Christopher Guzowski

## Context

The owner requested automatic comparison beneath the revealed answer key, with
separate colored report and human-decision cards. The owner explicitly selected
a new local LLM prompt for text portions only; all remaining scoring uses a
deterministic rubric. No manual grading is part of this workflow.

## Decision

Extend the standalone generator's post-decision reveal boundary with
`POST /api/generations/{incidentId}/comparison`. The request requires synthetic
operator identity and no body. The service invokes the existing authoritative
terminal reveal gate before reading comparison inputs or invoking any model.
Only the reveal service loads the oracle catalog. Copilot API reads remain
tenant-scoped; the generator checks incident/investigation/decision bindings and
selects exactly the AVAILABLE report referenced by the immutable human decision.
Its exact evidence IDs resolve to retained evidence snapshots. No report,
decision, evidence, retrieval, prompt or corpus input is modified.

`comparison-text-prompt/v1` sends only expected/actual cause and recommendation
text, required evidence prose and the report-bound observed evidence to Ollama.
The trusted instructions and untrusted JSON occupy separate system/user messages.
It never receives actual or expected disposition, confidence, human outcome or
reason, or decision-rule prose. Application parsing accepts exactly four fields:
two integer scores in 0-100 and two bounded explanations. Unknown fields,
duplicates, trailing JSON, incomplete responses and out-of-range values fail.
The evaluator uses temperature 0, no thinking, a JSON schema, 1,024 output tokens
and an 8,192-token context. One automatic retry is allowed. Correct null fields
for INSUFFICIENT_EVIDENCE score 100 deterministically; forbidden assertions score
0, without a model call.

`comparison-rubric/v1` computes exact disposition/confidence matches (0/100),
the rounded equal-weight mean of the four metrics, and bands: GOOD >=80,
OK 50-79, BAD <50. The expected human outcome is APPROVED only when both exact
checks pass and both text scores are >=80; otherwise REJECTED. The recorded
decision match is independently 0/100. Thus a correctly rejected poor report
can have a red report card and a green decision card. A high average does not
by itself justify approval; the per-metric approval rule is displayed.

The UI reveals first, then automatically requests comparison. It shows labeled
colored borders and accessible 0-100 gauges with black diamonds. Comparison
failure leaves the answer key visible and both cards Not scored, with a retry
action. A new generated incident invalidates pending results.

Every attempt retains a unique local tenant-scoped JSON artifact under the
configured artifact directory (default `./tmp/comparisons` relative to generator
working directory). It contains full comparison inputs, report/decision/evidence
IDs, oracle/model/prompt/rubric versions, complete prompt, attempted responses,
timestamps and input/prompt/result SHA-256 hashes. Earlier attempts are never
overwritten. Artifact retention failure prevents returning a successful result.
These ignored local files require ordinary local backup for durable retention;
they are not added to the copilot timeline or database.

## Consequences

This narrowly extends ADR-0014/0016's runtime prohibition only for the owner-
authorized generator post-decision evaluator. The offline evaluator remains
unchanged and does not acquire semantic percentages or new benchmark thresholds.
Oracle dependency tests still permit only the existing loader and reveal service.

Text scores are advisory AI assessments, not semantic proof, and may vary even
at temperature 0. The same local Qwen model is the default for compatibility;
`COMPARISON_CHAT_MODEL` independently configures a different installed judge.
No model is downloaded automatically. The judge cannot approve a report, alter
a human decision, or execute an operational action.

## Verification

Deterministic rubric/service/HTTP/UI tests and the complete repository gate.
Browser layout uses synthetic preview responses; an explicit live smoke check
may compare an already terminal report without creating a new human decision.
