# ADR-0014: Separate evaluation inputs from the scenario oracle

Status: Accepted; offline post-run evaluation extended by ADR-0016
Date: 2026-09-24  
Decision owner: Christopher Guzowski

## Context

The original scenario catalog combined observable fixtures with deterministic
answer keys. Corpus generation copied oracle root-cause prose into retrievable
runbooks, and retrieval evaluation deserialized that truth. Hiding the answer in
the browser therefore did not make evidence, knowledge, or evaluation inputs
independent.

## Decision drivers

- Keep oracle data outside every evaluated input.
- Preserve deterministic post-decision grading.
- Keep the original corpus inspectable without mutating its bytes or hashes.
- Preserve document identity, eligibility, scenario coverage, and fixed
  retrieval labels while versioning changed knowledge artifacts.

## Decision

Maintain `scenarios/catalog.json` as the observable fixture authority and
`scenarios/oracle.json` as `scenario-oracle/v1`. Generation, MCP evidence,
retrieval evaluation, and corpus generation consume only the observable
catalog. Only `AnswerKeyRevealService`, after the terminal-decision check from
ADR-0013, depends on the oracle catalog at runtime. ADR-0016 additionally
permits the repository-owned offline report evaluator to load the sealed oracle
after generation; it remains outside every application runtime and model input.

Preserve `synten-auth-knowledge/v1` byte-for-byte under
`SynTen Inc/corpus/versions/synten-auth-knowledge-v1/`. Publish
`synten-auth-knowledge/v2` with bumped document versions and filenames. Its
runbooks interpret exact signals neutrally from observable evidence and require
independent confirmation; the generator never reads oracle fields. Keep
`synten-retrieval-eval/v1` labels and thresholds unchanged, but bind the
contract explicitly to corpus v2.

## Consequences

### Positive

- Evaluated evidence, retrieval, reports, and human decisions cannot obtain the
  deterministic answer through their configured resources.
- Historical v1 artifacts and recorded hashes remain independently reviewable.
- Corpus changes are explicit in document versions, filenames, and manifest.

### Negative or accepted tradeoffs

- The generator deployment still contains both resources because it owns the
  terminal reveal endpoint; dependency tests enforce which service may load the
  oracle.
- Corpus v2 removes leakage but does not improve or rerun the failed retrieval
  benchmark. That measured outcome remains Q2.
- Neutral signal language is intentionally less prescriptive than a runbook
  derived from a known synthetic cause.

## Validation or revisit trigger

Revisit if the oracle moves to a separate deployment, if retrieval labels
change, or if a future corpus version needs independently authored signal
semantics beyond the current observable evidence contract.
