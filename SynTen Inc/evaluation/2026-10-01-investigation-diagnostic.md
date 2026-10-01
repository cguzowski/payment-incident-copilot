# Five fresh investigation comparisons

Executed 2026-10-01, 21:34-21:49 Europe/Warsaw, against the existing local demo.
Five fresh instances of existing observable scenarios completed alert intake,
investigation, evidence collection, PDF retrieval, report generation, terminal
decision, answer-key reveal and automatic comparison. These are regression
samples, not newly authored independent scenarios or a full benchmark.

The original reconciliation case S306 timed out twice at 120 seconds and remains
INVESTIGATING without a report or decision. Capture S302 was added to obtain five
complete comparisons. Both reconciliation failures remain in the results; they
are not excluded from the reliability assessment. Seven report calls produced
five AVAILABLE reports and two TIMED_OUT attempts across six investigations.

## Results under the unchanged rubric

All comparisons returned AVAILABLE using comparison-rubric/v1 and
comparison-text-prompt/v1. No production code, prompt, oracle, frozen corpus or
evaluation rule was changed. Oracle access occurred only through the product's
post-decision reveal endpoint, after each compared report had been generated.

| Case | Scenario | Report / band | Disposition | Confidence | Cause | Recommendation |
|---|---|---:|---:|---:|---:|---:|
| S005 | Issuer do-not-honor surge | 78 / OK | 100 | 100 | 70 | 40 |
| S301 | Authorization timeout | 45 / BAD | 100 | 0 | 40 | 40 |
| S302 | Capture upstream unavailable | 48 / BAD | 100 | 0 | 50 | 40 |
| S303 | Refund dependency timeout | 63 / OK | 100 | 0 | 70 | 80 |
| S313 | Partial refund evidence | 100 / GOOD | 100 | 100 | 100 | 100 |

Mean report score: 66.8; the four non-degraded reports average 58.5. Exact
disposition matches: 5/5. Exact confidence matches: 2/5. The three failures
selected HIGH rather than expected MEDIUM. These small-sample measurements are
diagnostic, not promotion criteria. Text scores remain advisory model judgments.

The owner explicitly rejected S005/S301/S303 and approved S313 after seeing
the report findings. S302 received a diagnostic rejection after the owner asked
to focus on report quality rather than human responses. All decision scores are
100, but they are not evidence of investigation quality or a human-review
benchmark. No AI approval was made.

## Observed problems and optimization priorities

1. **Calibrate confidence from evidence strength.** Multiple error categories
   in one aggregate source plus a runbook are not independent corroboration.
   S301/S302/S303 choose HIGH for hypotheses with unobserved mechanisms. The
   generation prompt constrains degraded cases to LOW but gives no criteria for
   HIGH versus MEDIUM. Define criteria from source authority and corroboration,
   preserving the existing LOW/null contract; do not hard-code oracle labels.

2. **Select operational passages, not metadata or generic governance.** S005
   spends four of seven excerpt slots on metadata-only cover pages, including
   all three selected policies. Its recommendation cites a generic prerequisites
   section. Related-policy expansion can add a score of 2.5 while ordinary RRF
   contributions are around 0.03; document diversity then limits additional
   passages from the relevant runbook. Test exclusion/downranking of metadata-only
   passages and bounded selection of diagnostic plus response passages from the
   strongest runbook. Preserve eligibility filters, frozen bytes and citations.
   The other cases retrieve relevant stage guidance, so retrieval alone cannot
   explain every miss.

3. **Anchor causes and recommendations to the observed failure mechanism.** The
   timeout and refund reports introduce configuration explanations without any
   configuration evidence. Capture turns possible local exceptions into its
   probable-cause narrative. Ask generation to distinguish the observed stage
   failure from speculative deeper mechanisms. Recommendations should name the
   responsible owner, exact missing records, uncertain final outcome and safe
   retry prerequisites where supported by retrieved guidance. The existing
   300-character instruction leaves room for these details; vague advice is not
   required by the length budget. Preserve human authority and no automatic retry.

4. **Protect observed facts and source mapping.** S301 says one late response
   although persisted GATEWAY_RESPONSE_LATE count is eight. The evidence adapter
   passes counts unchanged. S005 omits the exact code and count. Capture requests
   E02/E03/E10 but omits E04, the capture acknowledgement required by its cited
   runbook. Explore structured observation rendering or bounded code/count/source
   validation alongside improved generation instructions. Valid citation IDs
   alone did not prevent these errors. This is an additional report-quality check,
   not a change to comparison scoring.

5. **Measure context and latency before changing limits.** The loaded report
   model had context_length 4096; selected knowledge alone ranged from 4,025 to
   11,524 characters across the five completed cases, before serialized metadata,
   instructions and schema overhead. Truncation is a hypothesis, not a verified
   cause. Measure actual prompt tokens and retained input before testing an
   explicit context budget or smaller, more useful passages. Successful report
   latency was 65.835-107.200 seconds, median 92.314; reconciliation failed twice.
   Increasing timeouts alone would not fix factual or reasoning defects.

A useful next experiment changes one generation/retrieval behavior at a time,
uses deterministic failing regressions before production edits, and evaluates
fresh attempts plus unseen cases using the exact existing comparison. Keep this
baseline immutable. Do not inject answer keys into prompts, author guidance to
these answers or retry until scores improve. No optimization benefit has yet
been measured, and no production optimization is implemented by this assessment.

## Retained evidence and verification

The ignored [diagnostic directory](../../tmp/investigation-diagnostic-2026-10-01/)
contains inputs, model digests, evidence, PDF excerpts, reports, histories,
timelines, owner decisions, answer keys, comparison responses, summary JSON and
the run/verification scripts. comparison-summary.json records the SHA-256 and
absolute path of each native comparison artifact under the generator's ignored
tenant-scoped artifact directory. Back up those local directories for retention.

Generation: qwen3:8b-q4_K_M, report-prompt/v5, report-v1; embedding:
nomic-embed-text, 768 dimensions; retrieval: postgres-pdf-family-related/v5.
The retained chat digest is
500a1f067a9f782620b40bee6f7b0c89e17ae61f686b92c24933e4ca4b2b8b41.

The diagnostic verify.ps1 executed and checked five AVAILABLE reports, exact
evidence/retrieval binding, 43/43 citation references, S313's LOW/null/gap
contract, two retained reconciliation timeouts and unchanged hashes for all 15
protected comparison/evaluator/oracle files. Every comparison selected the exact
decision-bound report; five unique native artifacts were present and hashed.
Report histories stayed byte-equivalent after comparison (canonical JSON).
The diagnostic used product HTTP routes and existing services, not new runtime
code or database writes outside those routes. Full application suites were not
rerun for this assessment; the separate cleanup verification remains pending.

./verify.ps1 -Scope Repository and git diff --check passed after the report and
status/progress updates. Git diff --exit-code also confirms unchanged comparison
implementation/resources, additive oracle and offline evaluator. The changed
tracked files for this assessment are this report, docs/agent/STATUS.md and the
permitted progress notes in docs/agent/tasks/current.md; prior cleanup edits are
preserved. Production/model improvements remain proposals requiring a separate
implementation and live evaluation.
