# ADR-0025: Evidence-based confidence expectations after investigation

Status: Accepted
Date: 2026-10-04
Decision owner: Christopher Guzowski
Amends: ADR-0021 and ADR-0024 (post-decision confidence expectations only)

## Context

The owner explicitly requested confidence expectations calibrated to supplied
information and selected explicit evidence rules. Two v7 terminal reports chose
MEDIUM against original HIGH keys: rate limiting without traffic shape and DNS
policy blocking/resolution failure without affected-path linkage.

## Decision

Use confidence-evidence/v1 in the generator's gated post-decision comparison.
Resolve exactly the report-bound latest evidence ID. Missing/duplicate bindings
leave comparison unscored; earlier applicable history never rescues degraded
latest evidence. No report confidence, rationale, human response, severity,
scenario identifier or original expected confidence is used by the classifier.

LOW follows degraded/empty/unsupported-schema evidence, invalid windows,
timestamps/counts, missing required machine signals, unclassified signals,
symptom-only support or competing direct diagnoses. Recognized mechanism
signals with unresolved causal/scope context expect MEDIUM. Required machine
signals are extracted from the original key's required-evidence text; requirements
without machine signals cannot qualify for HIGH. This bounded extraction does
not establish that the key's natural-language conditions are fulfilled.

HIGH is attainable through three explicit direct signatures: TLS_CERTIFICATE_EXPIRED
with TLS_HANDSHAKE_FAILED, HSM_QUORUM_LOST with HSM_SIGNING_TIMEOUT, and
HSM_FIRMWARE_PROTOCOL_MISMATCH with HSM_SIGNING_FAILED. The required and observed
signal sets must equal the pair, with one positive aggregate row per signal,
matching counts and diagnosis no later than failure inside one valid service
window. Additional signals, repeated rows, unequal counts or reversed ordering
prevent HIGH. The diagnostic code names an explicit failure mechanism; counts
only check internal consistency. No independent confirmation is required.
This supports a narrow mechanism, not a deeper cause or final payment outcome.

comparison-rubric/v2 changes only the expected-confidence input. Exact matching
remains 0/100; disposition, text judge/prompt, rounded equal-weight mean, bands
and expected-decision formula remain unchanged. Aggregate and decision scores
can consequently change when confidence now matches. The grade exposes original
and calibrated expectations, confidence-rule version and rationale. Artifacts
retain these with the frozen key, evidence/report IDs and existing hashes.
UI displays the calibrated expected level and explains the original key.

## Consequences

Original oracle JSON, offline grading and historical comparison/report/decision
artifacts remain immutable. Recomparison creates a new uniquely identified
artifact under the existing terminal gate; historical v1 display falls back to
its original key. Neither report generation nor evidence schemas change.

These explicit rules are deliberately conservative and bounded. Other strong
signatures remain MEDIUM until separately reviewed. Approved knowledge is not
causal confirmation and is not evaluated here; recommendation grounding retains
its existing independent text score. This is not probability calibration,
general entailment, proof of causation or a guarantee of a confidence distribution.

## Verification

The active task records regression red/green, deterministic rules and UI tests,
exact retained-input replay and the full repository completion gate.
