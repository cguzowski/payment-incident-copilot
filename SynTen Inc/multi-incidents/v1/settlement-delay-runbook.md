---
documentId: 12005746-ceaf-5e4f-b012-d5ad1e974e87
tenantId: 8b860d80-d17f-4e6b-8c48-af35f26a4d61
type: RUNBOOK
title: Settlement delay Runbook
version: 1.0.0
incidentFamily: SETTLEMENT_DELAY
appliesTo: settlement-sim
approvalStatus: APPROVED
approvedBy: 7b636625-53d1-46f7-92a9-9c8c27a243d1
approvedAt: 2026-09-30T00:00:00Z
effectiveAt: 2026-09-30T00:00:00Z
---
# Settlement delay Runbook

SynTen Inc | Internal - Synthetic Demo | Version 1.0.0
All services, events and identifiers are fictional. No money is processed.

## Initial assessment

Confirm incident family `SETTLEMENT_DELAY` and source service `settlement-sim`. Match the
five-minute MCP observation interval to the alert detection time. Record exact
observation identifiers, error counts, retrieval status, source and timestamps.
The alert is a signal requiring investigation, not verified evidence of a cause.

## Diagnostic signals

`SETTLEMENT_BATCH_OVERDUE` and `SETTLEMENT_ACK_MISSING` are relevant `settlement-sim` error categories.
An overdue checkpoint or missing acknowledgement supports investigating settlement processing delay. It does not prove loss of funds or an actual settlement balance.
Compare their timestamps and counts with the alert. Counts describe errors,
not unique transactions, percentages, balance values or measured latency.
These signals can support a hypothesis about the affected stage; they cannot
independently prove a root cause. Preserve conflicting categories explicitly.

## Safe next checks

Request batch checkpoints and upstream settlement acknowledgements from the settlement operations owner through a separately
controlled read-only process. The current MCP tool supplies aggregate errors
only; these confirming records are not currently available in this application.
Record this gap instead of implying that the records were checked. Compare the
bounded error window with subsequent complete windows before drawing conclusions.

## Missing or contradictory evidence

PARTIAL, UNAVAILABLE, TIMED_OUT and empty observations require an insufficient-
evidence report, LOW confidence, null probable cause and recommendation, and
an explicit evidence gap. Retain prior observations as history only. An available
empty window is not proof that the underlying stage succeeded. Contradictory
observations remain visible; do not discard them to select a convenient hypothesis.

## Human review and escalation

Prepare an advisory summary for the settlement operations owner containing the incident ID,
service, observation window, error categories/counts, evidence identifiers and
missing confirming records. Link this report to the Settlement delay Policy for human
review. The operator may approve or reject the exact report with a reason.
The copilot cannot execute retries, replays, state changes or external messages.

## Revision history

1.0.0, 2026-09-30: Initial synthetic diagnostic guidance for SETTLEMENT_DELAY.
