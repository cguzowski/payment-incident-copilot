# O1 aggregate-evidence confidence calibration

Executed 2026-10-04 against an isolated copy of the local synthetic database,
with a newly built API on port 8083 and the unchanged generator on port 8084.
Seven fresh first attempts cover the October 1 diagnostic cases and additional
authorization-reset and webhook cases. Model calls run sequentially; answer keys
remain concealed until a diagnostic final rejection after report generation.
Failed reports remain unfinalized and unrevealed. No AI approval is made.

## Change and verification

report-prompt/v6 limits confidence to LOW/MEDIUM for the current single aggregate
operational source and explains that counts, categories, historical snapshots
and approved guidance are not independent causal confirmation. Degraded/empty
evidence retains INSUFFICIENT_EVIDENCE/LOW/null/gaps. The parser independently
rejects HIGH through the existing one-call MALFORMED path. The base report-v1
schema and historical HIGH records remain readable. See
[ADR-0023](../../docs/agent/decisions/ADR-0023-aggregate-evidence-confidence-ceiling.md).

The red phase reproduced three prompt/parser failures and a PostgreSQL HTTP
failure (expected MALFORMED, actual AVAILABLE). The green phase passed 24 focused
tests. Full ./verify.ps1 passed 367 API, 9 MCP, 74 generator and 103 console tests,
zero failures/errors/skips, plus seven nested Node cases, formatting, builds,
Compose and repository checks. The first full run stopped at formatting;
spotless:apply corrected the changed Java files and the rerun passed.

## Live results

Seven report calls produced three AVAILABLE and four TIMED_OUT attempts, with
no retries or MALFORMED attempts. All available reports completed final diagnostic
rejection, answer-key reveal and AVAILABLE comparison. Failed attempts remain
INVESTIGATING without a report, decision or reveal.

| Case | Report status | Confidence | Report / band | Disposition score | Confidence score | Cause | Recommendation |
|---|---|---|---:|---:|---:|---:|---:|
| S005 | AVAILABLE | MEDIUM | 88 / GOOD | 100 | 100 | 90 | 60 |
| S301 | TIMED_OUT | — | — | — | — | — | — |
| S302 | TIMED_OUT | — | — | — | — | — | — |
| S303 | TIMED_OUT | — | — | — | — | — | — |
| S313 | AVAILABLE | LOW | 100 / GOOD | 100 | 100 | 100 | 100 |
| S002 | AVAILABLE | MEDIUM | 64 / OK | 100 | 0 | 85 | 70 |
| S305 | TIMED_OUT | — | — | — | — | — | — |

Exact disposition matches are 3/3 scored reports; confidence matches are 2/3.
Mean score is 84 for completed reports only; this survivor mean is not comparable
to the October 1 five-case mean of 66.8. On the two completed shared cases,
S005 rose from 78 to 88 and S313 stayed 100; both already matched confidence
before O1. The three original HIGH/MEDIUM misses timed out, so their confidence
improvement remains unmeasured. All four timeouts occurred at approximately
120 seconds. Successful generation took 109.3-115.2 seconds. This run completed
3/7 calls versus the earlier diagnostic's 5/7 across a different case mix; the
small uncontrolled comparison does not establish the cause of slower generation.

S002's unchanged key expects HIGH from an upstream-reset cluster. The new
aggregate-only ceiling permits no HIGH report and therefore cannot match that
label under the present evidence contract. This is a measured compatibility
limitation, not grounds to modify the key or scoring rules. No overall answer-key
performance improvement is established by O1.

Manual support review also found defects that the unchanged text judge overlooks:
S005 attributes the issuer anomaly to synthetic data processing without support,
despite a cause score of 90. S313 preserves LOW/null/gaps but renders approved
guidance as observed incident facts with operational-evidence citations; several
fields end mid-sentence. S002's rationale acknowledges missing confirmation while
its probable cause asserts network/service disruption and its gaps discuss
real-world impact, which is not the relevant confirmation for this synthetic
incident. Valid citation membership does not prove these claims. O2-O4 remain
necessary; this task does not repair those outputs or alter the judge.

All final decisions are diagnostic REJECTED, not report-quality labels. Decision
scores are retained in the native artifacts and are excluded from quality claims.
The verification script checked seven single-attempt histories, exact evidence
and retrieval bindings, 24/24 citation references, S313's LOW/null/gap contract,
three decision-bound comparison artifacts and unchanged report histories after
comparison. All 15 protected evaluator/oracle/comparison hashes match both the
start of this run and the October 1 baseline.

## Provenance and scope

The database copy payment_copilot_o1_eval_20261004 retains 30 authorization PDF
versions / 705 chunks and 16 independent payment PDF versions / 65 chunks.
Embedding is nomic-embed-text / 768; retrieval is
postgres-pdf-family-related/v5. Generation uses qwen3:8b-q4_K_M, temperature 0,
1,536 output tokens and the unchanged 120-second timeout. Ollama reports a
4,096-token loaded chat context; actual input truncation has not been established.
The chat digest remains
500a1f067a9f782620b40bee6f7b0c89e17ae61f686b92c24933e4ca4b2b8b41.
Persisted prompt hash is
ccd960f635a433ed623a30bf234feaaba629478191e76b2fdc08491ee0b6b9d2;
per-context schema hashes are retained separately for every attempt.

comparison-rubric/v1, comparison-text-prompt/v1, answer keys, offline evaluator,
corpus, retrieval, model and limits are unchanged. No score threshold is added.
Human-decision scores are retained for auditability but are not the target of
this evaluation. The small diagnostic sample does not establish general
calibration, semantic support or a promotion decision. O2-O4 remain queued.

The ignored [run directory](../../tmp/o1-confidence-2026-10-04/) retains inputs,
reports, failure attempts, retrieved passages, histories, timelines, comparison
responses and native artifacts, scripts, database provenance, model digests and
runtime JAR hashes. These local artifacts require backup for durable retention.
The main demo runtime has not been replaced by the evaluation runtime.
Temporary evaluation services were stopped after comparison and verification;
the database copy is retained. Final ./verify.ps1 -Scope Repository and
git diff --check passed. Protected generator/comparison/evaluator/corpus and base
schema paths have no diff. O1 changes only report generation/validation, their
tests and the related task, status, roadmap, architecture, ADR and result records.
