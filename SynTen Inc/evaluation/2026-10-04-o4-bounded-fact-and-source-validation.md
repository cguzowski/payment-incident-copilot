# O4 bounded fact/source validation — 2026-10-04

Application validation is implemented and verified. Live diagnostic quality is
not established: six of seven fresh first attempts timed out; only unavailable
S211 returned a report. After explicit owner authorization, its fixed diagnostic
rejection unlocked the unchanged comparison: report 100/GOOD, decision 0/BAD.
No generation retry or judge call occurred. The score does not resolve the
separate manual summary/gap findings below.

## Boundary and provenance

The isolated API uses port 8091 and a new database,
payment_copilot_o4_eval_20261004, copied from the prepared reliability catalog:
1,562 knowledge chunks and initially zero incidents. The separate generator
uses port 8092. Existing main/O3/reliability APIs and databases were not changed.
Generation uses observable inputs and supplied approved passages, never oracle
content. The original diagnostic cases and two additional cases are retained.

- Provider: Ollama 0.35.1. Model: qwen3:8b-q4_K_M, digest
  500a1f067a9f782620b40bee6f7b0c89e17ae61f686b92c24933e4ca4b2b8b41.
- Prompt: report-prompt/v9; schema: context-constrained report-v1.
  Prompt template SHA-256:
  dabbb0a2e0082e29e813861a1c53b7acd5bc48af3a5dd880c14f617cd3f308d3.
- Retrieval: postgres-pdf-family-related/v5, nomic-embed-text / 768;
  frozen corpus and ranking are unchanged.
- report-model-settings/v2: 8,192 context tokens, stream=true, truncate=false,
  shift=false; temperature zero, disabled thinking, no tools, 1,536 output
  tokens and the unchanged two-minute deadline.
- All seven exact prompt/schema hashes and settings match persisted attempts.
  Reconstructed prompts, schemas and hashes are retained per case in tmp/o4/.
- API artifact SHA-256:
  89ee4a8d10bb1874df185d5b7dec5a39ab3a58e7d8060ef88cdfada82188ac36.
- Generator artifact SHA-256:
  a9bcb75c3493426160f1804752b3cd224003c3dcf9fbe6c05fa7ee4aa9fe2d3f.

## Fresh first attempts

Times use persisted timestamps. Provider token counts come from sequential
Ollama slot logs. All calls use the full context; slot releases show truncated=0.
Every timeout cancels the provider task and releases its slot. No incomplete
output is accepted as a report. Local throughput remains roughly 4-6 tokens/sec.

| Case | Status | Seconds | Prompt characters | Provider input tokens | Fact/source review | Comparison |
|---|---|---:|---:|---:|---|---|
| S005 | TIMED_OUT | 120.019 | 13,690 | 3,455 | No report | Unscored |
| S301 | TIMED_OUT | 120.007 | 20,949 | 4,834 | No report | Unscored |
| S302 | TIMED_OUT | 120.014 | 21,592 | 4,829 | No report | Unscored |
| S303 | TIMED_OUT | 120.013 | 20,859 | 4,748 | No report | Unscored |
| S002 | TIMED_OUT | 120.019 | 14,622 | 3,645 | No report | Unscored |
| S313 | TIMED_OUT | 120.013 | 21,056 | 4,859 | No report | Unscored |
| S211 | AVAILABLE | 60.350 | 14,492 | 3,478 | LOW/null; empty observations; prose limits below | 100/GOOD report; 0/BAD fixed decision |

The immutable five-case baseline has report scores 78/45/48/63/100 for
S005/S301/S302/S303/S313. Fresh O3 returned four reports and three timeouts;
reliability's S301/S302/S303 all timed out. This run is not a controlled throughput
benchmark; sampling failures and prompt changes prevent an overall improvement
claim. Historical keys/artifacts and all current comparison formulas remain
unchanged. Do not assign invented scores to failed generation attempts.

## Separate manual fact/source review

S211's unavailable source has no applicable event snapshot. The returned
observations, inferences and contradictions are empty; probable cause and
recommendation are null, confidence is LOW and both evidence references name
the exact unavailable attempt. No observed code/count or E## request is invented.
There is no live sufficient-evidence count preservation or source-role request
to evaluate. The six failed attempts remain explicit unmeasured results.

Manual limitations: the summary states that authorization declines rose, but
its unavailable evidence citation cannot confirm that alert-described trend.
The first gap requests missing capture/refund/settlement source-role guidance
unrelated to this authorization incident. Confidence prose copies internal
input-field names. These fields are outside the bounded validator; citation
membership and null cause/recommendation do not establish general support.
Any degraded-case comparison must be interpreted alongside these findings.
Detailed review is retained in tmp/o4/review.md with exact API snapshots.

## Owner-authorized post-decision comparison

The owner explicitly authorized the diagnostic REJECTED decision for only S211
attempt 3b6fee3d-7f32-41e1-9dab-18c67de93982 in the isolated O4 database.
Decision a4663378-4375-4c1d-9cd3-6513e06fe0e5 was persisted before revealing
the answer key. Comparison dcb62224-7ea7-4361-97cf-d1f273b22fe7 is AVAILABLE,
using unchanged comparison-rubric/v2, comparison-text-prompt/v1 and
scenario-oracle/v1. It completed in 0.036 seconds with modelId=not-used and
zero judge calls.

Disposition, confidence, cause and recommendation each score 100; report score
is 100/GOOD. Expected/original confidence is LOW under confidence-evidence/v1.
Cause/recommendation scores check correctly withheld null fields; they do not
measure diagnostic prose. Expected APPROVED versus actual fixed REJECTED yields
decision score 0/BAD. This owner-authorized evaluation decision is not an
analyst-performance benchmark. Six timed-out cases remain open and unrevealed.

The native artifact preserves the exact report prose, tenant, evidence and
decision bindings. SHA-256:
db8b0ee6730c304809aa9668a83494aabdf646e6252745d314129616fb5282b5.
comparison-verification.log and comparison-summary.json retain the binding,
call-count and hash checks. All seven new report histories remain unchanged.

## Verification and preservation

- Red parser tests: 14 executed, 13 intended failures against the previous parser.
  PDF wrapping reproduces one source-map failure; three role swaps and literal
  template-token preservation fail before their fixes. Intermediate unsuccessful
  test runs remain in tmp/o4/ rather than being counted as successful evidence.
- Final focused report/persistence suite: 75 tests, zero failures/errors/skips.
- Full ./verify.ps1: 401 API / 9 MCP / 82 generator / 103 console, zero
  failures/errors/skips, eight nested Node UI cases, formatting, builds,
  repository scripts, Compose and diff checks. First full verification fails
  on a console-held esbuild file lock. Verified console commands are restored
  after temporary shutdown; both loopback listeners return HTTP 200.
- All 373 protected corpus/baseline/comparator/oracle/evaluator hashes match.
  Seven original O3 and three reliability report histories remain unchanged.
  The completed reliability archive equals its previous current-task Git blob
  80dc135dbdc6d305b46478242256415773afcbfa. O3 remains unfinished.
- There is exactly one attempt per new investigation with exact evidence and
  retrieval bindings. Failed attempts leave incidents open; S211 has only the
  owner-authorized diagnostic rejection. Post-comparison checks confirm all ten
  original histories and 373 protected hashes remain unchanged.

Artifacts are ignored local files under tmp/o4/: all inputs/histories, exact
prompt/schema reconstruction, persisted provenance, model/provider/runtime
snapshots, first-attempt outcomes, manual review, decision/reveal/comparison
artifacts, binding verification and red/green/full logs.
They require backup for durable retention. The main demo API was not restarted;
only the isolated runtime uses this build. No broader semantic-quality claim.
