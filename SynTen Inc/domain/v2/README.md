# SynTen payment domain v2

Status: E1 authoring contract
Version: `synten-payment-domain/v2`
Date: 2026-10-01
Classification: Internal - Synthetic Demo
Tenant: `8b860d80-d17f-4e6b-8c48-af35f26a4d61`

This successor defines fictional operating semantics for independent knowledge
authoring. It does not replace the [historical profile](../../profile.md), modify
runtime states or authorize a payment engine. All records, actors, amounts and
dependencies are synthetic. No real money or card/account data is used.

Read this profile, [lifecycle](lifecycle.md), [risk inventory](risk-inventory.md)
and [evidence requirements](evidence-requirements.md) together. E2 derives PDF
topics from these authorities and reviews coverage before freezing the library.
E5 creates incidents after that freeze without target documents. Changes to this
domain require an explicit version review; historical corpus bytes stay intact.

## Scope and independence

The implemented baseline has seven families: `AUTHORIZATION_DECLINE_RATE_SPIKE`,
`AUTHORIZATION_TIMEOUT_SPIKE`, `CAPTURE_FAILURE_SPIKE`, `REFUND_FAILURE_SPIKE`,
`SETTLEMENT_DELAY`, `WEBHOOK_DELIVERY_FAILURE`, `RECONCILIATION_MISMATCH`.
[ADR-0018](../../../docs/agent/decisions/ADR-0018-additive-incident-families.md)
owns that implementation. Existing scenarios/guidance are regression assets,
not independent held-out work.

The successor covers the lifecycle from a synthetic intent through authorization,
capture, release, refund and settlement, with notification and reconciliation as
independent projections. Familiar investigation categories include explicit card
rejections, uncertain outcomes, suspected duplicates, refund problems and lingering
holds. These categories are not selected new API enums. Policies also cover risks
with no eventual incident: authority, data freshness, event order and safe recovery.

Authoring inputs are owner requirements, existing architectural boundaries and
the domain rules here. No scenario catalogs, fixtures, oracle files, retrieval
labels or expected model outputs were read to construct this domain/risk inventory.
Existing family names indicate compatibility scope, not target causes or answers.
No risk row selects a document ID, incident code or expected report disposition.

## Fictional operating responsibilities

| Capability / owner role | Responsibility | Authority limit |
|---|---|---|
| Payment Operations Analyst | Compare records, preserve gaps and request review | Report decision does not authorize payment actions |
| Payment Lifecycle Owner | Own intent/request records and stage relationships | Local intent does not prove a downstream outcome |
| Authorization / Gateway Owners | Interpret explicit simulated issuer responses and routing | Transport failures do not establish rejection reasons |
| Capture Owner | Own capture requests and downstream acknowledgements | Accepted submission is not settlement |
| Refund Owner | Own linked refund operations and their confirmations | Pending refund is not completed return |
| Settlement Owner | Own synthetic batch checkpoints and receipt records | No claim about real bank transfers |
| Delivery Owner | Own notification attempts and recipient acknowledgements | Delivery cannot change payment truth |
| Reconciliation Owner | Compare bounded source snapshots and explain differences | A mismatch cannot authorize balance correction |
| Platform / Security Owners | Assess deadlines, connectivity, configuration and access | Recovery requires separate human change authorization |
| Knowledge Approver / Incident Commander | Approve guidance / coordinate unresolved investigations | Neither may manufacture missing source evidence |

These are vocabulary and source responsibilities within the synthetic generator
boundary, not additional services to deploy. Current MCP exposes aggregate service
errors only. All item-level sources below are requirements for later design.

## Guidance applicability

Family guidance explains authorization, capture, refund, settlement, delivery or
reconciliation mechanisms. Shared policies govern evidence sufficiency, identity,
retry/idempotency, amount invariants, reconciliation cutoffs, security, escalation
and human authority across relevant stages. A family match does not make every
shared policy relevant; E3 must define explicit applicability and retrieval tests.

The present family filter and Markdown/PDF eligibility remain unchanged. E2's
inventory must state supported stages and risk IDs for each topic, including
general diagnostic guidance for unfamiliar combinations. Useful direction may
mean requesting a discriminating source rather than naming a cause or remediation.

## Boundaries carried forward

Tenant identity accompanies every record and source lookup. Identifiers are opaque.
Guidance is not observed evidence; alert text and hidden truth are not source
authority. Report observations, inference, limitations and citations remain distinct.
Q6 degraded/empty evidence continues to require LOW confidence and null cause and
recommendation; this domain does not relax the report contract. No report approval
executes an action. New tools, runtime schemas, timing configurations, accepted
PDF manifests and evaluation thresholds are decisions for later phase contracts.
