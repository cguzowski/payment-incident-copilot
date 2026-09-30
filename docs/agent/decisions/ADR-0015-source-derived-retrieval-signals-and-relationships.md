# ADR-0015: Source-derived retrieval signals and relationships

Status: Accepted
Date: 2026-09-29
Decision owner: Christopher Guzowski

## Context and authorization

The owner authorized implementation of the Q2 recommended sequence after
reviewing the proposal. The retained baseline and every failed assertion are
documented in [the diagnosis](../../../SynTen%20Inc/evaluation/q2-baseline-diagnosis.md).
Generic prose crowds exact observed signals out of lexical depth, while policy
slots are dominated by generic policies. The earlier diversity and compact-query
experiments were reverted. Corpus bytes, labels, thresholds and eligibility
remain fixed.

## Decision

Keep `knowledge-query/v2`, embeddings, lexical/vector thresholds, depth 20 per
type/modality, RRF k=60 and the four-runbook/three-policy selector allocation.
Version changed ranking as `postgres-hybrid-signals/v3` during the isolated
signal experiment, then `postgres-hybrid-related/v4` for relationships.

Recognize distinct uppercase underscore-delimited machine tokens in the bounded
derived query. Count exact whole-token occurrences in each eligible chunk once
per query token. Order lexical candidates by match count then lexical rank;
the v3 score is match count plus RRF. Its retained run reached 22/22 primary,
12/20 policy and 18/21 ordering cases. Diagnostics showed equally complete
signal matches losing because only the weaker lexical candidate also passed
vector depth. V4 adds `0.5 / lexicalPosition` when an exact lexical signal match
exists, retaining RRF as the remaining contribution. This bounded preference
for lexical evidence cannot outweigh an additional exact signal. No
code-to-document mapping is maintained. With no matching tokens, direct
ordering remains ordinary RRF.

Ingest existing hash-verified source front matter into separate derived
document metadata: document key, related keys and parser version
`document-relationships/v1`. Reimport may populate this metadata on an existing
version only when its immutable source/catalog hashes match. Do not rewrite
chunks, embedding inputs or vectors. Legacy documents without relationships
continue direct retrieval. File references such as `corpus/inventory.md` stay
in immutable source bytes and are excluded from the document-key graph;
no filesystem lookup follows them. Malformed and duplicate references fail
validation. Preserve self references but never expand recursively.

Resolve relationships only within the eligible tenant/family/effective approved
catalog. Ambiguous keys are not traversed. Use at most four distinct ranked
runbooks as anchors, and follow at most two edges: runbook to policy, or runbook
to another runbook to policy. No recursive expansion or relationship fallback
to superseded versions. Rank paths by anchor position, then shortest distance,
then stable source identity. Merge related policy candidates with direct policy
matches while preserving the ranks of existing direct candidates. Expansion
scores all eligible policy chunks against the same query and thresholds;
its full policy modality ranks use chunk ID for deterministic ties. Choose one
best RRF chunk per related policy and at most 20 such policies. Retain these
pre-limit policy ranks for newly admitted chunks. Related-only candidates have
no exact-signal contribution; existing direct candidates retain theirs.
Add `10 / (3 * anchorPosition + hops)` to the candidate's base score; deduplicate
by chunk ID, then use score and the existing deterministic tie fields. Direct
policy matches remain available. Keep the final allocation at four runbooks
and three policies. Expansion cannot bypass positive lexical relevance or the
0.55 vector threshold, including when embeddings are unavailable.

Persist exact-signal counts and the chosen relationship path in selected result
snapshots and evaluation candidates, including anchor/intermediate version IDs,
keys, source hashes, path length and metadata version. Retain old snapshots
without inventing relationship evidence. V10 adds nullable JSONB ranking evidence
to selected snapshots and derived catalog columns. Evaluation candidate records
carry the same additive evidence. The union is bounded at 100 candidates
(80 direct plus 20 related); the artifact byte bound becomes 6 MB. Evaluator
labels, thresholds, assertions and grading logic are unchanged.

## Verification and risks

Use deterministic test-first PostgreSQL and unit regressions for exact token
boundaries, depth, parser validation, idempotent import, ambiguous/missing links,
cross-tenant/family/version exclusions, bounded traversal, lexical fallback and
immutable provenance. Run the fixed live benchmark separately for signals and
combined relationships, then the full repository gate. The retained combined
run `1f66fee3cf194f268287a314651ba13f` passed at 22/22 primary, 20/22 applicable
policy cases (20 required) and 20/21 ordering cases, with zero ineligible candidates and all
special semantics preserved. See the active task for final verification.

A wrong anchor can propagate to a policy. Authored links express association,
not incident truth or permission to operate. Exact signals can occur in several
documents and do not prove root cause. A failing benchmark requires explicit
owner disposition, not weakened labels or eligibility.
