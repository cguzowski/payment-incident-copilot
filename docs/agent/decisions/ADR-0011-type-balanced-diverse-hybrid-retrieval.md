# ADR-0011: Type-balanced and document-diverse hybrid retrieval

Status: Accepted  
Date: 2026-09-01  
Decision owner: Christopher Guzowski

## Context

The retained K4 evaluation used `knowledge-query/v1` and
`postgres-hybrid-rrf/v1`. Generic incident-family/title terms caused broad
RB-001 and PL-001 matches to dominate a single global 20-deep list per
modality. The four-runbook/three-policy selector could then spend several slots
on chunks from the same document version. K4 selected RB-001 four times and
PL-001 twice for S001 instead of selecting its labeled primary RB-002.

K5 must improve that measured behavior without changing the corpus, labels,
eligibility filters, embedding tuples, RRF constant, similarity threshold, or
final type allocation.

## Decision

Use `knowledge-query/v2` for live product retrieval. When applicable evidence
exists, derive the query from the normalized incident description, explicit
evidence status, service, and ordered error-code counts. Omit the repeated
incident-family and generic alert-title boilerplate. When evidence is not
applicable or was not collected, preserve normalized title, description, and
status without inventing observations. Keep the query bounded to 2,000
characters and retain contributing evidence IDs.

Use `postgres-hybrid-rrf/v2`. Keep exact PostgreSQL full-text and cosine-vector
ranking, candidate depth 20, RRF `k=60`, positive lexical rank, cosine threshold
`0.55`, and existing deterministic tie-break fields. Apply candidate depth
independently to RUNBOOK and POLICY inside each lexical and vector modality;
rank positions are therefore type-local. Four disjoint 20-deep lists make the
maximum fused candidate union 80.

Select final context in two stable passes. The first pass takes the best-ranked
chunk from distinct document versions while respecting the four-runbook and
three-policy capacities. The second pass fills remaining capacity from the
original fused order. Candidate and selected positions remain persisted for
audit.

## Rationale

The final context already reserves capacity by document type, so applying the
same balance before fusion prevents one type from starving the other. A
distinct-document first pass uses the small context budget for broader source
coverage while retaining ranked repeats when the eligible corpus cannot fill a
type with distinct versions. Removing generic repeated query terms lets exact
observed service/error signals carry more of the lexical and semantic query.

## Consequences

### Positive

- S001 selects RB-002 first and the real operator action displays its cited PDF
  excerpt instead of a no-match state.
- The fixed evaluation improves primary-runbook coverage from 9/22 to 19/22
  and supporting-policy coverage from 1/20 to 12/20.
- Tenant, approval, effective-time, model/dimension, and superseded-source
  filters remain unchanged; the live K5 result contains zero ineligible
  candidates.
- Candidate and artifact bounds remain explicit and tested.

### Negative or accepted tradeoffs

- A complete 37-variant audit artifact can now contain up to 2,960 candidate
  records, so its serialized bound increases from 2 MB to 4 MB.
- Type-local ranks are not directly comparable as a single global modality
  position; the persisted document type and fused score must be read with them.
- The fixed evaluation still fails its original quality thresholds: 19/22
  primary-runbook cases, 12/20 required supporting-policy cases, and 16/21
  primary-over-weak cases versus 19 required.

## Validation or revisit trigger

Revisit when a separately approved evaluation changes the type allocation,
corpus scale makes the 80-candidate exact search material, or measured policy
recall justifies a different query/ranking change. Do not weaken eligibility or
hard-code scenario/document mappings to improve a score.
