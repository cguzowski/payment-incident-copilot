# ADR-0027: Bounded report fact and source validation

Status: Accepted
Date: 2026-10-04
Decision owner: Christopher Guzowski

## Context

O4 targets altered observed counts, omitted exact codes and source requests
that confuse authorization records with capture acknowledgements. Existing
schema and citation membership checks cannot detect these defects. The owner
approved bounded validation with MALFORMED rejection, no rewrite and no retry.

## Decision

report-prompt/v9 keeps report-v1, the model, deadline, provider options and
two-observation/300-character bounds. Each new observation must exactly copy
one supplied tuple: sourceEventId=...; observedAt=...; errorCode=...; count=....
Use the snapshot's Instant string and count without aggregation or paraphrase.
The tuple must cite only applicableAttemptId, which owns these observations;
the latest degraded attempt is not the source of historical facts. Reject
invented, changed or duplicated tuples. Empty snapshots allow no observations.
Selection remains bounded to two events; this does not guarantee full coverage.
Counts absent from provider evidence are rejected at its existing normalization
boundary and are never synthesized by this validator.

The provider's per-context observation schema enumerates exact eligible strings
and their citation owner, but parsing independently enforces the same rule.
No output is repaired or replaced. Existing generation handling records a
terminal MALFORMED attempt without transitioning the incident or retrying.
Checks apply only to new generation, not historical reads or decisions.

For inferences, probable cause, recommendation and contradictions, explicit
standalone uppercase E followed by two digits must appear in the claim's cited
raw knowledge passages. Check four explicit stage-role patterns: authorization
followed by approv/response/confirm, capture or refund followed by
acknowledg/confirm, and settlement followed by receipt/confirm. Match whole stage
words and noun prefixes within 60 characters, case-insensitively. Require an
explicit source-role pairing in those passages. Pairing starts at E## and stops
at the next E##, period,
semicolon or sentence end. Collapse PDF whitespace before deriving pairings.
Every requested pairing must be supported. An identifier elsewhere in the
claim does not establish the requested role. The mapping is derived from the
cited text, never from a scenario code or a hard-coded E04 lookup. Cited
guidance assigning E04 to capture acknowledgements therefore requires that
pairing; uncited guidance cannot supply it.

## Limits and consequences

Observation prose becomes deliberately restricted. Unsupported prose or a
tuple exceeding the existing length bound fails closed; strictness may increase
MALFORMED outcomes. Repeated codes retain individual event/time/count tuples;
there is no count summation or claim of independent corroboration.

Source checks cover explicit identifiers and the four stated stage-role patterns.
Other stage-role meanings, paraphrases, pronouns, negation, hypothetical prose,
owner attribution and semantic applicability are not generally verified.
Summary, confidence rationale and gaps remain prose; exact observation checks
do not prove the rest of the report. Source membership alone is not role
entailment. Stage pattern checks conservatively reject even hypothetical
stage-role prose without a supported explicit pairing. Claims
without these patterns retain existing structural checks and human review.
Ambiguous source lists or missing mapping should remain explicit gaps rather
than fabricated mappings. This is not a general semantic checker or a report
quality guarantee. No corpus, retrieval, comparator or historical change.

Source data containing template tokens or dollar/backslash sequences is inserted
once as literal data; replacements never run against the supplied snapshot.

## Validation

Red/green parser tests exercise exact counts, wrong-code counts, missing and
fabricated tuples, timestamps, repeated signals, incorrect historical citations,
paraphrases and appended guidance. Source tests cover wrong/missing identifiers,
uncited passages, source-role confusion, PDF wrapping and text-derived mappings.
PostgreSQL HTTP regressions check retained MALFORMED outcomes, tenant isolation,
unchanged evidence, open incidents and one model call. The active O4 task records
focused/full verification and separate live fact/source review with failures.
