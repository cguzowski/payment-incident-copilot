# O3 grounded-recommendation evaluation

Executed 2026-10-04 against a new isolated database containing only the existing
prepared knowledge catalog (1,562 historical/active chunks). The API uses port
8085 and the unchanged generator uses port 8086. Seven fresh first attempts
cover the five diagnostic regressions plus connection-reset and unavailable
authorization evidence. No retries, new scenarios or corpus changes.

## Implementation and verification

report-prompt/v8 requests observed mechanisms rather than unverified deeper
causes, guidance-supported owners/records and applicable retry safeguards,
unknown final outcomes and explicit missing guidance. It retains v7 confidence
instructions, structural parsing, citations and mandatory human review. There
is no semantic prose checker or automatic output repair.

The instruction regressions first failed in tmp/o3/red.log. The Docker-enabled
red-provenance run executed 28 tests with 13 expected instruction/version
failures, zero errors/skips. Green passed all 28 focused prompt/parser and
PostgreSQL HTTP tests. Full ./verify.ps1 passed 371 API, 9 MCP, 82 generator and
103 console tests, zero failures/errors/skips, eight nested Node UI cases,
formatting, builds, Compose and diff checks. The first full run stopped on the
running console's esbuild.exe lock; the console was stopped, the gate rerun and
the console restored on port 4200 (HTTP 200). Logs remain under tmp/o3/.

## Fresh outcomes

| Case | Report outcome | Confidence | Seconds | Valid citation references |
|---|---|---|---:|---:|
| S005 | AVAILABLE | MEDIUM | 112.961 | 9 |
| S301 | TIMED_OUT | — | 120.016 | 0 |
| S302 | TIMED_OUT | — | 120.010 | 0 |
| S303 | TIMED_OUT | — | 120.014 | 0 |
| S313 | AVAILABLE | LOW | 96.551 | 5 |
| S002 | AVAILABLE | MEDIUM | 94.889 | 9 |
| S211 | AVAILABLE | LOW | 40.862 | 2 |

Four of seven calls returned schema-valid reports; three hit the unchanged
120-second limit. Both degraded reports retain INSUFFICIENT_EVIDENCE/LOW/null
cause and recommendation with gaps. All 25 references belong to the exact
persisted evidence/retrieval snapshots. This verifies membership, not support.

After reviewing the prepared request, the owner authorized four fixed diagnostic
rejections on 2026-10-04. Only the four AVAILABLE evaluation reports were rejected;
the three timed-out cases remain INVESTIGATING, without decisions or reveals.
All four post-decision comparisons returned AVAILABLE. The main demo's incidents
and decisions were not changed. No AI approval or operational action was taken.

## Post-decision comparison

All comparisons use unchanged comparison-rubric/v2, confidence-evidence/v1 and
comparison-text-prompt/v1. Exact disposition/confidence matches are 4/4.

| Case | Report / band | Cause | Recommendation | Expected decision | Fixed rejection score | Judge calls |
|---|---|---:|---:|---|---:|---:|
| S005 | 88 / GOOD | 70 | 80 | REJECTED | 100 | 1 |
| S313 | 100 / GOOD | 100 | 100 | APPROVED | 0 | 0 |
| S002 | 90 / GOOD | 80 | 80 | APPROVED | 0 | 1 |
| S211 | 100 / GOOD | 100 | 100 | APPROVED | 0 | 0 |

The two degraded cases earn deterministic 100s for withholding cause and
recommendation; those scores do not assess their observations or gaps. S005 and
S002 each used one Qwen judge call, with no retries, completing in 24.950 and
19.525 seconds. The fixed rejections are owner-authorized evaluation setup,
not a benchmark of human decisions; the three zero decision scores are retained.

S005 recommendation rises from 40 in the October 1 diagnostic and 60 in O1 to
80, while cause remains 70 versus October 1 and falls from O1's 90. S002 cause
falls from O1's 85 to 80 and recommendation rises from 70 to 80. These mixed
advisory scores do not establish reliable grounding improvement: manual review
still finds generic requests and unsupported hypotheses. In particular, the
S005 judge calls the generic recommendation more specific, despite its omission
of the owner/records identified in the support review. Both judgments are retained.

## Manual support review (independent of scores)

- S005: ISSUER_DO_NOT_HONOR_SURGE count 126 is preserved. The cause asserts
  issuer processing/configuration without supporting configuration evidence;
  its cited signal passage explicitly cautions against establishing a root
  cause. The generic recommendation cites evidence prerequisites and omits
  owner/record requests. Four of seven selected passages are metadata-only.
- S002: UPSTREAM_CONNECTION_RESET count 58 is preserved. Cause stays closer to
  the observed mechanism, and network/dependency explanations are tentative
  inferences. Recommendation remains generic and adds routing-configuration
  checks without explaining support. It omits a responsible owner despite an
  available RB-101 owner-role passage. A gap explicitly preserves missing
  retry/recovery guidance; no automatic retry is proposed.
- S313: LOW/null structure is correct, but observations/inference copy guidance
  about Refund/Capture/Reconciliation Owners instead of the supplied error
  records. Summary, rationale and a gap end mid-sentence at the existing schema's
  300-character ceiling; the prompt's stricter under-300 request was not followed.
  Existing structural validation accepted them. Correct null fields do not
  establish factual quality.
- S211: No observations or cause/recommendation are invented. A gap saying no
  guidance is available overstates the retrieval, which returned seven approved
  passages. Unavailable operational evidence still correctly controls disposition.
- S301/S302/S303: No prose returned; mechanism, owner/record and retry safeguards
  cannot be assessed. Their failures remain in the reliability denominator.

No reliable O3 adherence or improvement is established. Prompt regression tests
verify the instructions and persisted contract; they cannot prove model prose.

## Baselines and provenance

The October 1 diagnostic retained cause/recommendation scores S005 70/40,
S301 40/40, S302 50/40, S303 70/80 and S313 100/100 under rubric/v1. O1's later
retained S005 scores were 90/60, S313 100/100 and S002 85/70; its three stage
cases timed out. These bytes remain unchanged. New comparisons use the current
unchanged rubric/v2 and text-prompt/v1. Rubric/v2 changes
only confidence expectations; cause/recommendation judging remains unchanged.
Different generated inputs, retrievals and advisory judging prevent attributing
score differences solely to this prompt change. Baseline values were checked
against their original comparison artifacts, correcting the preliminary S002
transcription without changing any historical result.

Model: qwen3:8b-q4_K_M, temperature 0, maximum output tokens 1,536; retrieval:
postgres-pdf-family-related/v5 with nomic-embed-text / 768. Persisted template
SHA-256: 98d9ce4d3ea5ac8b3c33005adb885605f46fad55e8e7095c531f96b3b8ae1094.
Reconstructed per-case template/schema hashes match all seven persisted attempts.
Prompts contain 12,126–19,891 characters; these are character counts, not measured
token counts. The observed Ollama runtime reports a 4,096-token chat context;
this run does not establish whether particular v8 instructions were truncated.
No context/output/time-budget changes were made.

tmp/o3/ retains requests, responses, histories, exact reconstructed contexts,
prompts/schemas, persisted provenance, generation-summary.json, review.md,
verification logs and both runtime jar hashes. comparison-summary.json retains
each native artifact path/SHA-256 and comparison, report and decision IDs.
comparison-verification.log verifies all seven unchanged report histories, four
exact evidence/decision/comparison bindings and three unfinalized failures.
comparison-hashes.log independently verifies native input/prompt/response hashes
with the unchanged JSON serialization. Hash checks preserve 357 baseline/
corpus files and 16 comparison/oracle/evaluator files. The preceding completed
task's archive matches its original Git blob. Local tmp artifacts are ignored;
durable retention requires backing them up and backing up the isolated database.

## Remaining work

Authorized decisions, comparisons and retained evidence are complete. Semantic
adherence and stage-specific recommendations remain
unproven; timeouts, retrieval usefulness and context budgeting remain limitations.
O3's first two acceptance criteria remain unchecked; O3 is not declared complete.
