# Independent lifecycle risk inventory

Version: `synten-payment-domain/v2`
Inputs: [profile](README.md), [lifecycle](lifecycle.md) and
[source requirements](evidence-requirements.md). No scenario/oracle inputs.

Rows derive from state transitions, asynchronous boundaries, dependencies and
authority controls. They are coverage requirements for E2, not selected document
membership, incident fixtures, answer keys or guaranteed diagnoses. A topic may
cover several risks; risks may require several guidance topics. Include risks
that never become demo incidents. E2 must review distinguishing checks, negative
checks, alternative explanations, conditional recovery and escalation for each.

## Lifecycle and asynchronous boundaries

| Risk | Mechanism / possible effect | Distinguishing evidence and alternatives | Guidance applicability / accountable owner |
|---|---|---|---|
| R01 | Explicit rejection versus local validation/transport failure; wrong explanation | E02/E03 separate local rejection from issuer reason; E01 alone cannot identify locked card or funds | Authorization family; shared evidence policy / Authorization Owner |
| R02 | Lost or late authorization response; uncertain hold and unsafe repeat | E02/E03/E05/E10 compare correlated result, hold and deadline; timeout may coexist with approval | Authorization timeout; shared retry policy / Gateway Owner |
| R03 | Key reuse with changed payload or new key for same intent; repeated operation | E02 plus relevant E03/E04/E06 compare keys, payloads and downstream IDs; repeated delivery differs from execution | All request stages; shared identity/idempotency policy / Lifecycle Owner |
| R04 | Two holds displayed as duplicate payments; misleading symptom | E02/E03/E04/E05 distinguish attempts, consumed units and active holds; projection alone is insufficient | Authorization/capture and duplicate category; shared evidence policy / Lifecycle Owner |
| R05 | Capture requested before approval or after confirmed expiry; invalid stage relationship | E03/E04/E05/E10 establish authorization amount/time and capture result; missing confirmation leaves unknown | Capture family; lifecycle precondition policy / Capture Owner |
| R06 | Capture acknowledgement lost; unsafe recapture after local error | E02/E04 compare submission with downstream confirmation; transport failure may mask success | Capture family; retry policy / Capture Owner |
| R07 | Partial captures plus pending requests exceed headroom; apparent amount breach | E03/E04 compare confirmed and pending linked units; stale snapshots may explain disagreement | Capture family; shared amount policy / Capture Owner |
| R08 | Cancellation/reversal mistaken for refund; hold remains or incorrect operation proposed | E03/E04/E05 distinguish consumed from unconsumed authorization; release pending differs from failure | Reversal/hold category and capture; shared action authority / Gateway Owner |
| R09 | Expiry assumed from local clock; stale hold view | E05/E10/E11 compare explicit expiry, effective rule and clock uncertainty; deadline alone proves no release | Hold category; timing/evidence policies / Gateway Owner |
| R10 | Refund submission confused with completion; unsupported customer-facing conclusion | E04/E06/E10 compare capture linkage, final refund outcome and expectation; pending differs from failed | Refund family; confirmation/escalation policy / Refund Owner |
| R11 | Repeated or partial refunds exceed captured units; uncertain remaining headroom | E02/E04/E06 compare distinct confirmed/pending operation IDs and amounts; event replay is not another refund | Refund family; amount/idempotency policies / Refund Owner |
| R12 | Settlement deadline or membership misapplied; apparent delay | E07/E10 verify batch closure, cutoff and receipt; not-yet-due and missing receipt are alternatives | Settlement family; cutoff/escalation policy / Settlement Owner |
| R13 | Settlement receipt conflicts with local status or batch totals | E04/E06/E07/E09 compare exact membership and complete snapshots; receipt cannot explain omitted operations | Settlement/reconciliation; shared conflict policy / Settlement Owner |
| R14 | Lost, duplicated or reordered notification; stale recipient view | E08 plus responsible stage source compare event/operation IDs and acknowledgement; delivery says nothing about payment success | Webhook family; delivery/replay policy / Delivery Owner |
| R15 | Retry exhaustion confused with final downstream payment failure | E08/E10 distinguish effective retry budget from stage confirmation; missing acknowledgement may mask receipt | Webhook family; shared retry/evidence policies / Delivery Owner |
| R16 | Reconciliation compares different cutoffs, units or incomplete inputs; false mismatch | E07/E09/E10 establish compatible membership, amounts and coverage; incomplete run is not confirmed discrepancy | Reconciliation family; comparison/completeness policy / Reconciliation Owner |
| R17 | Genuine comparable mismatch with several possible mechanisms | E02/E04/E06/E07/E09 trace operation lineage; duplicates, omissions and delayed projections require separate evidence | Reconciliation/capture/refund; shared conflict policy / Reconciliation Owner |

## Cross-stage controls and dependencies

| Risk | Mechanism / possible effect | Distinguishing evidence and alternatives | Guidance applicability / accountable owner |
|---|---|---|---|
| R18 | Stale route/configuration or dependency failure; multiple affected stages | E01/E10/E11 compare effective change/window, route and health; correlation is not proof of causation | All affected families; change/rollback policy / Platform Owner |
| R19 | Clock skew or event ordering error; false overdue or invalid apparent transition | E10/E11 and stage journal versions distinguish received order from source order; preserve clock uncertainty | All stages; time/provenance policy / Platform Owner |
| R20 | Missing/partial/retention-limited sources; unsupported conclusion | E01-E11 coverage and attempted scopes distinguish complete empty from missing; request exact absent source | All families; evidence sufficiency policy / Payment Operations Analyst |
| R21 | Contradictory authoritative records; invented final outcome | Relevant E02-E09 source identities, versions and correlations; do not pick latest timestamp automatically | All stages; conflict/escalation policy / Lifecycle Owner |
| R22 | Foreign tenant/operation correlation; false evidence binding | All source tenant IDs and linkage; mismatch must fail safely rather than return another entity | All families; isolation/provenance policy / Platform Security |
| R23 | Credential/permission failure; unsafe bypass or secret disclosure | E01/E11 bounded failure metadata and effective access context; neither proves a payment result | All affected families; access/secrets policy / Platform Security |
| R24 | Model or operator treats report approval as execution authority | Application decision/audit record plus separate human procedure; evidence/guidance cannot authorize action | All families; human authority/change policy / Incident Commander |

## Coverage review and unresolved outcomes

E2 must account for every R01-R24 row in a versioned inventory using these IDs,
with explicit shared/family applicability and source prerequisites. A coverage
gap is recorded before corpus freeze; do not close it by tailoring an incident.
Do not claim all possible payment scenarios are covered. V2 deliberately excludes
real money, fees/FX, disputes, split tender and real customer/account state.

Straightforward guidance can use explicit authoritative results. Ambiguous cases
need competing hypotheses and discriminating evidence. Compound failures need
cross-stage lineage; missing/conflicting cases need source recovery or escalation.
The library must provide direction without fabricating certainty. Current E01-only
observations cannot satisfy most distinguishing checks; that is an explicit
capability gap for E4, not a reason to invent source observations or weaken Q6.
