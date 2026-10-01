# Independent library inventory

Version: synten-payment-knowledge/v1. Exact metadata authority: [inventory.json](inventory.json).

## Documents

| Key | Type / title | Stages | Risks | Evidence requirements | Related |
|---|---|---|---|---|---|
| RB-101 | RUNBOOK / [Authorization rejection: establish the source](pdfs/rb-101-authorization-rejection-establish-the-source.pdf) | authorization | R01, R18, R20 | E01, E02, E03, E10, E11 | PL-101, RB-102, RB-110 |
| RB-102 | RUNBOOK / [Uncertain authorization outcomes](pdfs/rb-102-uncertain-authorization-outcomes.pdf) | authorization, release | R02, R03, R09, R19 | E02, E03, E05, E10, E11 | RB-103, RB-105, PL-102, PL-103 |
| RB-103 | RUNBOOK / [Suspected duplicates and replayed events](pdfs/rb-103-suspected-duplicates-and-replayed-events.pdf) | authorization, capture, refund, delivery | R03, R04, R11, R14 | E02, E03, E04, E05, E06, E08 | PL-101, PL-102, RB-105, RB-108 |
| RB-104 | RUNBOOK / [Capture acknowledgement and amount checks](pdfs/rb-104-capture-acknowledgement-and-amount-checks.pdf) | authorization, capture | R05, R06, R07, R17 | E02, E03, E04, E05, E09, E10 | PL-102, RB-103, RB-109 |
| RB-105 | RUNBOOK / [Authorization holds, release and reversal](pdfs/rb-105-authorization-holds-release-and-reversal.pdf) | authorization, release, capture | R04, R08, R09, R19 | E02, E03, E04, E05, E10, E11 | RB-102, RB-106, PL-102, PL-103 |
| RB-106 | RUNBOOK / [Refund progress and partial amount exposure](pdfs/rb-106-refund-progress-and-partial-amount-exposure.pdf) | capture, refund | R10, R11, R03, R17 | E02, E04, E06, E09, E10 | PL-102, PL-103, RB-103, RB-109 |
| RB-107 | RUNBOOK / [Settlement checkpoints and receipt gaps](pdfs/rb-107-settlement-checkpoints-and-receipt-gaps.pdf) | capture, refund, settlement, reconciliation | R12, R13, R16, R19 | E04, E06, E07, E09, E10, E11 | PL-103, RB-109, RB-111 |
| RB-108 | RUNBOOK / [Webhook delivery and recipient acknowledgement](pdfs/rb-108-webhook-delivery-and-recipient-acknowledgement.pdf) | delivery, cross-stage | R14, R15, R03, R19 | E02, E08, E10, E11 | PL-102, PL-103, RB-103, RB-110 |
| RB-109 | RUNBOOK / [Reconciliation without invented corrections](pdfs/rb-109-reconciliation-without-invented-corrections.pdf) | capture, refund, settlement, reconciliation | R07, R11, R13, R16, R17, R21 | E02, E04, E06, E07, E09, E10 | PL-101, PL-102, PL-103, RB-111 |
| RB-110 | RUNBOOK / [Dependency, configuration and event-order investigation](pdfs/rb-110-dependency-configuration-and-event-order-investigation.pdf) | cross-stage | R18, R19, R23, R02, R14 | E01, E10, E11 | PL-101, PL-103, PL-104, PL-105 |
| RB-111 | RUNBOOK / [Incomplete evidence and conflicting final records](pdfs/rb-111-incomplete-evidence-and-conflicting-final-records.pdf) | cross-stage | R20, R21, R22, R01, R17 | E01, E02, E03, E04, E05, E06, E07, E08, E09, E10, E11 | PL-101, PL-104, PL-105 |
| PL-101 | POLICY / [Evidence sufficiency and conflicting-source policy](pdfs/pl-101-evidence-sufficiency-and-conflicting-source-policy.pdf) | cross-stage | R01, R20, R21, R22 | E01, E02, E03, E04, E05, E06, E07, E08, E09, E10, E11 | RB-111, PL-103, PL-104, PL-105 |
| PL-102 | POLICY / [Idempotency, retry and amount safety policy](pdfs/pl-102-idempotency-retry-and-amount-safety-policy.pdf) | authorization, capture, release, refund, delivery | R02, R03, R04, R05, R06, R07, R08, R10, R11, R15 | E02, E03, E04, E05, E06, E08, E10 | RB-103, RB-104, RB-105, RB-106, RB-108, PL-105 |
| PL-103 | POLICY / [Time, freshness and comparable-cutoff policy](pdfs/pl-103-time-freshness-and-comparable-cutoff-policy.pdf) | cross-stage | R09, R12, R13, R14, R15, R16, R19, R20 | E07, E08, E09, E10, E11 | RB-107, RB-109, RB-110, PL-101 |
| PL-104 | POLICY / [Tenant isolation and safe operational records](pdfs/pl-104-tenant-isolation-and-safe-operational-records.pdf) | cross-stage | R22, R23, R20 | E01, E02, E10, E11 | PL-101, PL-105, RB-110, RB-111 |
| PL-105 | POLICY / [Human authority, escalation and controlled recovery](pdfs/pl-105-human-authority-escalation-and-controlled-recovery.pdf) | cross-stage | R24, R18, R21, R23 | E01, E10, E11 | PL-101, PL-102, PL-104, RB-110, RB-111 |

Family applicability is explicit in inventory.json. Shared policies retain cross-stage scope;
these relationships are authoring metadata, not implemented retrieval rules.

## Risk coverage

| Domain risk | Guidance keys |
|---|---|
| R01 | RB-101, RB-111, PL-101 |
| R02 | RB-102, RB-110, PL-102 |
| R03 | RB-102, RB-103, RB-106, RB-108, PL-102 |
| R04 | RB-103, RB-105, PL-102 |
| R05 | RB-104, PL-102 |
| R06 | RB-104, PL-102 |
| R07 | RB-104, RB-109, PL-102 |
| R08 | RB-105, PL-102 |
| R09 | RB-102, RB-105, PL-103 |
| R10 | RB-106, PL-102 |
| R11 | RB-103, RB-106, RB-109, PL-102 |
| R12 | RB-107, PL-103 |
| R13 | RB-107, RB-109, PL-103 |
| R14 | RB-103, RB-108, RB-110, PL-103 |
| R15 | RB-108, PL-102, PL-103 |
| R16 | RB-107, RB-109, PL-103 |
| R17 | RB-104, RB-106, RB-109, RB-111 |
| R18 | RB-101, RB-110, PL-105 |
| R19 | RB-102, RB-105, RB-107, RB-108, RB-110, PL-103 |
| R20 | RB-101, RB-111, PL-101, PL-103, PL-104 |
| R21 | RB-109, RB-111, PL-101, PL-105 |
| R22 | RB-111, PL-101, PL-104 |
| R23 | RB-110, PL-104, PL-105 |
| R24 | PL-105 |

All 24 risks have diagnostic/control coverage. No incident mappings or answer keys are included.
The 30 historical authorization PDFs remain separate and retain their original manifest.
