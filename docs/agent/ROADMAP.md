# Product roadmap

Status: Active
Last reviewed: 2026-10-01
Owner: Christopher Guzowski

## Purpose

Preserve the completed single-tenant authorization workflow and expand familiar
payment problems with independent knowledge and incident design. The
[expansion plan](PAYMENT_EXPANSION_PLAN.md) owns gates and decisions.
This file orders outcomes; only an
owner-activated [task contract](tasks/current.md) authorizes implementation.

## Completed milestones

| Milestone | Outcome |
|---|---|
| E1 / B01-B06 | Shared local/CI verification and explicit codebase boundaries |
| P1-P4 | Approved knowledge, advisory reports, human decisions, and audit workflow |
| K1-K3 | Synthetic profile, 30-document PDF corpus, and page-aware catalog |
| K4-K5 | Live embeddings, fixed retrieval evaluation, and operator retrieval proof |
| R1 | Generated incidents connected to their matching MCP evidence |
| R2 | Local Qwen S001 report validated and persisted for review |
| Q1 | Oracle isolated from evaluated inputs with terminal reveal and corpus v2 |
| U1 | Report generation releases obsolete work and recovers its button state after terminal outcomes |
| U2 | Populated investigation histories and individual approved sources use accessible native disclosures |
| U3 | Resolvable reviewer identifiers open exact rendered records and immutable PDFs |
| Q2 | Fixed corpus-v2 retrieval benchmark passes at 22/22 primary, 20/22 policy and 20/21 ordering; full repository gate passed |
| Q3 | Versioned offline report grader retains reproducible bounded correctness, citation, unsupported-claim, latency, and failure metrics for all 36 oracle scenarios |
| Q4 | Live S012 Qwen report explicitly rejected by the owner; exact report binding, terminal queue state, unchanged report, and eight-event audit timeline verified |
| Q5 | All 36 corpus-v2 live reports and reproducible grades retained; citation IDs valid, but insufficient-evidence and confidence gaps remain |
| Q6 | Degraded/empty evidence requires insufficient-evidence/LOW/null reports; independent validation, full gate and fresh S111/S211 live proof pass |
| ADR-0018 expansion | Six additional families have deterministic triage, guidance, human decision and audit coverage; live report quality is unmeasured |
| Expansion E1 | Versioned payment domain, lifecycle, 24 independent risks and source requirements; documentation/static gate |

K4/K5 completion did not pass the benchmark. See
[STATUS.md](STATUS.md) for current evidence limitations and
[SynTen Inc](../../SynTen%20Inc/README.md) for recorded results.

## Active and ordered future outcomes

| Order | Outcome | Completion boundary |
|---|---|---|
| E2 — Next | Independent PDF library | Risk-derived guidance, validated PDFs and frozen hashes before incident creation. |
| E3 | Catalog and retrieval | Versioned accepted catalog, PDF-only eligibility and applicable shared/family policies. |
| E4 | Multi-family evidence and workflow | Synthetic payment/refund records and timeline; independent builds and regressions. |
| E5 | Independent demo incidents | Cases from system behavior after corpus freeze, without target runbooks. |
| E6 | Held-out evaluation | Grounded conclusions and useful direction measured with retained provenance. |
| D1 — Deferred | AWS deployment | Select services, tooling, networking, IAM, cost, teardown, and any Bedrock profile in an ADR before implementation. |
| D2 — Deferred | Authentication | Select identity and authorization and enforce tenant/operator access at every public boundary. |

Q1 gates answer-key reveal on an `APPROVED` or `REJECTED` human decision,
separates `scenario-oracle/v1` from observable scenario resources, and binds the
unchanged retrieval labels to corpus v2, which excludes oracle answers but uses
observable scenario inputs. It is not independently authored. The exact v1 corpus
is preserved as a hash-verifiable historical archive.

## Sequencing rules

- Q2 completed the [documented sequence](tasks/completed/2026-09-30-resolve-retrieval-quality-disposition-q2.md):
  retained a new baseline, diagnosed per-variant losses, evaluated runbook
  ranking separately from policy relationships, and verified the combined
  change against the fixed benchmark and full repository gate.
  [ADR-0015](decisions/ADR-0015-source-derived-retrieval-signals-and-relationships.md)
  records the implementation.
- Q3 completed `synten-report-eval/v1` as a deterministic offline post-run
  grader. ADR-0016 records its bounded metric semantics and oracle isolation;
  the retained fixture proves evaluator behavior, not live-model quality. Q4
  completed one live human-decision and audit proof. Q5 now retains all 36 live
  reports and reproducible grades. Confidence and insufficient-evidence failures
  led to Q6's completed degraded-evidence remediation. Fresh S111/S211 reports
  pass the LOW/null contract; confidence gaps on sufficient evidence remain.
  No quality threshold was added and the other 34 scenarios were not rerun under v5.
- Treat corpus v2 and its labels as fixed historical inputs. New corpus authoring
  excludes scenario inputs; new scenarios must not target documents. Complete E2
  before E5. Corpus changes require
  an approved version task; keep source/PDF hashes and prior evidence.
- Keep generation, ingestion, embedding, and evaluation separately verifiable.
- Version parser, chunker, query, ranking, and model changes explicitly.
- Keep tests independent of live AI providers. Record live evaluation separately.
- Preserve exact PostgreSQL hybrid retrieval until measurements justify change.
- Keep human decisions mandatory and prohibit model-executed recommendations.
- Revisit ordering when measured retrieval/report quality, latency, or corpus
  scale invalidates the current approach.
