---
documentId: 8b4f9f78-6d3b-5c22-893a-da234d224f3b
tenantId: 8b860d80-d17f-4e6b-8c48-af35f26a4d61
type: RUNBOOK
title: Reconciliation mismatch Runbook
version: 1.0.0
incidentFamily: RECONCILIATION_MISMATCH
appliesTo: reconciliation-sim
approvalStatus: APPROVED
approvedBy: 7b636625-53d1-46f7-92a9-9c8c27a243d1
approvedAt: 2026-09-30T00:00:00Z
effectiveAt: 2026-09-30T00:00:00Z
---
# Reconciliation mismatch Runbook

SynTen Inc | Internal - Synthetic Demo | Version 1.0.0
All services, events and identifiers are fictional. No money is processed.

## Initial assessment

Confirm incident family `RECONCILIATION_MISMATCH` and source service `reconciliation-sim`. Match the
five-minute MCP observation interval to the alert detection time. Record exact
observation identifiers, error counts, retrieval status, source and timestamps.
The alert is a signal requiring investigation, not verified evidence of a cause.

## Diagnostic signals

`RECONCILIATION_TOTAL_MISMATCH` and `RECONCILIATION_RECORD_MISSING` are relevant `reconciliation-sim` error categories.
Mismatch and missing-record signals support investigating input completeness or inconsistent checkpoints. Aggregate errors do not establish a financial loss or identify the authoritative balance.
Compare their timestamps and counts with the alert. Counts describe errors,
not unique transactions, percentages, balance values or measured latency.
These signals can support a hypothesis about the affected stage; they cannot
independently prove a root cause. Preserve conflicting categories explicitly.

## Safe next checks

Request immutable input snapshots, matched-record counts and authoritative checkpoint totals from the reconciliation owner through a separately
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

Prepare an advisory summary for the reconciliation owner containing the incident ID,
service, observation window, error categories/counts, evidence identifiers and
missing confirming records. Link this report to the Reconciliation mismatch Policy for human
review. The operator may approve or reject the exact report with a reason.
The copilot cannot execute retries, replays, state changes or external messages.

## Revision history

1.0.0, 2026-09-30: Initial synthetic diagnostic guidance for RECONCILIATION_MISMATCH.
