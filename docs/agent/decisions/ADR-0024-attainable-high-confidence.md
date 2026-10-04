# ADR-0024: Attainable HIGH with conservative confidence selection

Status: Accepted
Date: 2026-10-04
Decision owner: Christopher Guzowski
Supersedes: ADR-0023

## Context

The owner clarified that the product should most commonly produce MEDIUM,
sometimes LOW for weak investigation support, and rarely HIGH despite missing
independent confirmation. The absolute aggregate-only ceiling exceeded that
intent. Exact post-decision confidence matching should therefore stay unchanged.

## Decision

report-prompt/v7 restores LOW/MEDIUM/HIGH provider eligibility and removes the
parser's unconditional HIGH rejection. Confidence concerns the narrow observed
mechanism rather than a confirmed deeper root cause or final payment outcome.
MEDIUM is the normal choice with material uncertainty. HIGH is exceptional:
substantial, direct, consistent incident-window observations, applicable guidance,
and no material contradiction or similarly supported alternative. Independent
confirmation strengthens confidence but is not mandatory. Counts, severity,
repeated categories/history and generic knowledge alone do not justify HIGH.
LOW remains appropriate for weak, ambiguous or conflicting support.

Degraded/empty evidence still independently requires insufficient-evidence,
LOW, null cause/recommendation and explicit gaps. All structural/citation
validation and human decision gates remain. Prompt selection is advisory;
no semantic support checker or scenario-specific confidence rules are added.

## Consequences

The model may select any structurally valid level for AVAILABLE evidence.
Prompt criteria cannot guarantee a frequency or prove an exceptional HIGH is
supported. Measure live outcomes separately; never force MEDIUM or tune to keys.
Versioned metadata and historical v5/v6 reports/comparisons remain unchanged.
comparison-rubric/v1, judge prompt, keys, weights and thresholds remain exact.
The known prompt/context truncation defect is a separate unresolved concern.

## Verification

The current task records failing/green confidence eligibility and HTTP persistence
regressions, six degraded input cases, full verification and live diagnostic limits.
