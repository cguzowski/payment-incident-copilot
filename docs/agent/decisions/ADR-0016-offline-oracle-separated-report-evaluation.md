# ADR-0016: Offline oracle-separated report evaluation

Status: Accepted
Date: 2026-09-30
Decision owner: Christopher Guzowski

## Context

The application persists structured, cited report attempts with model, prompt,
schema, source, status, and timing metadata. Q1 removed the scenario answer key
from generation inputs, and ADR-0014 allowed only terminal answer-key reveal at
runtime. The repository still lacked one reproducible way to measure report
correctness, citation integrity, unsupported-claim indicators, latency, and
terminal failures across the complete scenario oracle.

Natural-language semantic grading could use an LLM judge or fuzzy similarity,
but either would add another model dependency and obscure what a score proves.
Exact application schema validation also cannot prove that a cited statement is
true. The first grading contract therefore needs bounded, inspectable semantics
and an explicit limitation rather than an overstated quality score.

## Decision drivers

- Preserve the observable-input/oracle separation established by ADR-0014.
- Make every reported metric reproducible from retained bytes and versions.
- Fail closed on incomplete or internally inconsistent runs.
- Separate measured facts from quality inference.
- Keep automated verification independent of live models, databases, and
  network services.

## Considered options

### LLM-as-judge grading

- Advantages: Can compare paraphrased causes and recommendations semantically.
- Disadvantages: Adds model/version/prompt variance, cost, latency, and another
  output requiring validation; a self-judge can also bias results.

### Fuzzy lexical or embedding similarity

- Advantages: Simple aggregate scores and partial paraphrase tolerance.
- Disadvantages: Thresholds are unmeasured and can turn lexical overlap into a
  misleading correctness claim.

### Exact bounded indicators in an offline evaluator

- Advantages: Deterministic, inspectable, network-free, and precise about what
  each metric establishes.
- Disadvantages: Cannot establish general natural-language entailment or detect
  every unsupported claim.

## Decision

Adopt `synten-report-eval/v1` as a repository-owned offline evaluator. It loads
the observable catalog, the sealed `scenario-oracle/v1`, and a completed
36-scenario run only after report generation. No generator, evidence,
retrieval, prompt, copilot API, or operator-console runtime may load grading
content or gain a new oracle dependency.

The input fails closed unless it has exactly one result for every reviewed
scenario, matching catalog/oracle hashes and versions, valid terminal status
and timing, complete report metadata, and consistent report presence. The
output retains exact input/evaluator/catalog/oracle hashes, per-result
model/prompt/schema metadata, eligible citation sets, structured report content,
per-scenario grades, and aggregates.

Report these dimensions separately:

- correctness: terminal availability, exact disposition, exact confidence,
  required oracle machine-signal coverage, and null cause/recommendation for
  insufficient-evidence scenarios;
- citation integrity: required citation presence and exact membership in the
  retained eligible evidence and knowledge identifier sets;
- bounded unsupported-claim indicators: unknown source identifiers, machine
  signal tokens absent from observable scenario evidence, and cause or
  recommendation assertions when the oracle requires insufficient evidence;
- latency: terminal completion minus request time, including count, minimum,
  median, nearest-rank p95, and maximum; and
- terminal outcome counts for AVAILABLE, UNAVAILABLE, TIMED_OUT, and MALFORMED.

The evaluator does not assign a Q3 pass threshold and must describe that these
checks are not semantic entailment. Live multi-scenario execution remains Q5.

## Rationale

Exact bounded checks are the smallest auditable step from anecdotal report
review to repeatable measurement. Retained hashes and full structured inputs
allow independent reproduction, while explicit metric names prevent citation
membership or token overlap from being presented as proof that prose is true.
Measured live results can later justify a reviewed threshold or a versioned
semantic grader.

## Consequences

### Positive

- A complete run can be compared without exposing oracle content during
  generation.
- Invalid or partial coverage cannot silently produce an aggregate grade.
- Citation, failure, and latency regressions remain independently visible.
- Automated tests require no live AI or infrastructure.

### Negative or accepted tradeoffs

- Correct paraphrases can miss required exact machine signals.
- Unsupported prose without a foreign signal, bad citation, or forbidden
  insufficient-evidence assertion can escape the bounded detector.
- The deterministic fixture proves evaluator behavior, not live-model quality.
- A future semantic judge or promotion threshold requires a new version and
  owner-reviewed decision.

## Validation or revisit trigger

Revisit after Q5 produces broad live reports, when bounded indicators fail to
separate known-good and known-bad reports, or when a stable independently
validated semantic-evaluation method materially improves decision quality.
