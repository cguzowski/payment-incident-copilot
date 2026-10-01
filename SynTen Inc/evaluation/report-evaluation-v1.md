# SynTen Inc report evaluation v1

Status: Approved evaluation design
Evaluation version: `synten-report-eval/v1`
Observable scenarios: 36
Oracle version: `scenario-oracle/v1`

## Purpose and boundary

This offline contract grades structured synthetic report attempts after model
generation. It loads the observable catalog and sealed oracle only inside the
repository evaluation runner. Report generation, evidence collection,
retrieval, prompts, application runtimes, and the operator console cannot load
this contract or its oracle data.

The grader is deterministic and network-free. It reports bounded checks and
does not claim that token coverage or valid citation identifiers establish
natural-language entailment. There is no Q3 pass threshold.

## Input contract

An input has schema `synten-report-eval-input/v1` and contains:

- a non-empty run UUID and round-trip ISO-8601 creation timestamp;
- exact lowercase SHA-256 values for the observable catalog and oracle;
- oracle version `scenario-oracle/v1`; and
- exactly one result for each of the 36 reviewed scenario codes.

Every result retains its scenario and attempt identifiers, terminal status,
request/completion timestamps, model ID, prompt/schema versions and hashes,
eligible evidence and knowledge identifiers, and the structured `report-v1`
document when status is AVAILABLE. UNAVAILABLE, TIMED_OUT, and MALFORMED results
must not contain a report. Missing, duplicate, unknown, version-mismatched,
malformed, or internally inconsistent input fails before grading.

## Metrics

Correctness is reported as separate counts for terminal availability, exact
oracle disposition, exact oracle confidence, coverage of machine-signal tokens
in `requiredEvidence`, and null probable cause/recommendation when the oracle
requires `INSUFFICIENT_EVIDENCE`.

Citation integrity reports claim citation presence, recommendation knowledge-
citation presence, total/valid references, and exact unknown evidence or
knowledge identifiers against the retained eligible sets.

Bounded unsupported-claim indicators count unknown source identifiers, claim
machine-signal tokens absent from the observable scenario, and any probable
cause or recommendation asserted for an oracle insufficient-evidence scenario.
The detector does not grade arbitrary prose entailment.

Latency includes every terminal result and records count, minimum, median,
nearest-rank p95, and maximum milliseconds. Terminal outcome counts remain
separate for AVAILABLE, UNAVAILABLE, TIMED_OUT, and MALFORMED.

## Reproduction

Grade a retained input:

```powershell
./scripts/evaluation/run-synten-report-evaluation-v1.ps1 `
  -InputPath <input.json> `
  -OutputPath <new-result.json>
```

Generate the deterministic contract fixture:

```powershell
./scripts/evaluation/run-synten-report-evaluation-v1.ps1 `
  -SyntheticFixture `
  -OutputPath <new-result.json>
```

Output publication is atomic and refuses overwrite. The result retains the
evaluator, input, observable catalog, and oracle SHA-256 values plus all
per-scenario data required to inspect the reported metrics.

## Retained fixture result

`results/q3-report-grader-fixture-v1.json` is a deterministic evaluator fixture,
not a live-model quality result. It contains all 36 scenarios and deliberately
exercises 33 AVAILABLE, one UNAVAILABLE, one TIMED_OUT, and one MALFORMED
terminal result. It also contains one unknown evidence reference, one unknown
knowledge reference, one foreign machine signal, and two forbidden
insufficient-evidence assertions.

Artifact SHA-256:
`a023fa81eac34732a619cd76850db936dac2f48358fa55afa9263a1846e93555`.

## Live Q5 result and CLI compatibility

Q5 retains a complete live input and reproducible grade for all 36 scenarios.
See [measured results and artifacts](q5-live-results.md); citation validity does
not erase the insufficient-evidence and confidence failures.

The CLI preserves JSON timestamp strings by selecting `ConvertFrom-Json
-DateKind String` where supported. Earlier PowerShell versions without DateKind
retain their default string parsing. The evaluator still rejects malformed
round-trip ISO-8601 timestamps. Executable CLI regressions on PowerShell 7.6.5
compare the exact grade bytes (including input hash and latency) with direct
module invocation and verify invalid timestamps publish no output.

Q5's historical CLI failure and direct-module workaround remain recorded in
its reproduction evidence; retained input and result artifacts are unchanged.
