# ADR-0018: Additive synthetic incident families

Status: Accepted under explicit owner request
Date: 2026-09-30
Decision owner: Christopher Guzowski

## Context and scope decision

The owner requested at least six additional incident forms and full triage like
the completed authorization-decline slice. This explicitly expands the original
single-family MVP after its completion, outside the historical authorization
corpus phase. Six bounded simulated families are feasible without new deployables.

## Decision

Add authorization timeouts, capture failures, refund failures, settlement delays,
webhook delivery failures and reconciliation mismatches. Preserve legacy intake
by defaulting an omitted incidentType to AUTHORIZATION_DECLINE_RATE_SPIKE;
validate explicit values using the application enum. Persist the explicit family
and use existing tenant/family metadata filters for knowledge retrieval. Console
labels derive from each incident rather than a constant.

Keep additional observable fixtures, sealed oracle, versioned Markdown runbooks
and policies under SynTen Inc/multi-incidents/v1. Maven packages only the needed
resources into each independently buildable service. API runtime loads only
approved guidance; API workflow tests package only observable fixtures. Original
scenario/oracle/corpus bytes, hashes and fixed evaluations remain unchanged.
The generator composes the original catalog with twelve additional scenarios:
complete and degraded evidence for each family. The answer-key reveal remains
restricted to a terminal human decision. Existing rarity weighting is retained.

Reuse the immutable MCP v1 getRecentServiceErrors contract. These simulated
services expose stage-specific aggregate errors; no new tools or payment engines
are needed to diagnose those bounded signals. Reuse retrieval, report validation,
review and audit, including the Q6 degraded-evidence rules. Explicit Markdown
knowledge preparation uses the existing ingestion command, preserving source
versions, line locators, embedding metadata and human review.

## Tradeoffs and limits

Service errors do not independently establish transaction outcomes, measured
alert thresholds, balances, settlement deadlines or final delivery state. Approved
runbooks identify the absent confirmation sources and prohibit automatic retries,
replays or balance changes. This is synthetic triage support, not simulation of
payment processing. Markdown sources avoid unnecessary PDF generation while
retaining the existing approved-source contract. The old 36-case benchmarks do
not measure these new families; deterministic workflow tests use provider doubles
and no live-model quality claim is made.

## Verification

Six typed-intake regressions failed before implementation. Catalog and approved
knowledge tests proved missing scenarios/guidance; twelve console regressions
proved incorrect constant labels. Parameterized persisted workflow tests cover
all twelve additional scenarios with family filters, source provenance, report
citations, degraded LOW/null behavior, human decisions and tenant-scoped audit.
The active task records focused/full verification results.
