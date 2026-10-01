# Independent operational PDF authoring standard

Version: `synten-independent-authoring/v1`
Applies to: `synten-payment-knowledge/v1`
Classification: Internal - Synthetic Demo
Date: 2026-10-01

The [domain/v2](../../domain/v2/README.md), lifecycle, 24-risk inventory and evidence
requirements are the new-content authorities. Historical authorization PDFs remain
an identified baseline, not independently authored material. Do not use scenario
catalogs/fixtures, oracle answers, retrieval labels or expected report outputs to
author this library. No document must select a target incident or expected answer.

## Membership and control

[inventory.json](inventory.json) owns exactly 16 additions: 11 runbooks and 5 shared
policies. Stable opaque document ID, key, type, title, version 1.0.0, owner,
APPROVED status, effective date 2026-10-01, tenant, classification, stages/families,
risk IDs, source requirements and related keys are required. Synthetic knowledge
approval names a fictional role; it is neither authentication nor a report decision.
E3 must decide how stage/shared-family metadata becomes eligible runtime retrieval.

Each inventory row has one JSON-front-matter Markdown authority under `sources/`
and one text PDF under `pdfs/`. The first page displays title, purpose, responsibility
and document control; the second presents the procedure or mandatory controls; the
third presents response/governance, validation, source limits and revision history.
V1 uses three editorial pages. The absolute accepted limit remains 15 inclusive.
Three complete pages are preferred here over filler to meet historical page targets.

## Operational content

Runbooks must identify use/non-use, responsible owners, distinguishing and negative
checks, alternative explanations, missing/stale/conflicting evidence, conditional
human recovery, validation and rollback boundaries, escalation package and related
policies. Keep topic-specific procedures; do not repeat a generic diagnostic script.

Policies must define durable mandatory controls, accountable roles, exceptions,
non-compliance, record retention and review triggers. They link to runbooks rather
than copying their diagnostic sequences. No exception may fabricate evidence,
relax Q6, authorize model actions or use real payment data.

Only E01 aggregate errors are available through current MCP. E02-E11 are future
read-only requirements, never claimed existing tools. A correlated explicit source
result can support a bounded conclusion. Ambiguous cases must name the missing
discriminating source, and unresolved cases remain valid. No universal coverage,
live-model quality, account balance or payment-processing claim is permitted.

## Visual and extraction contract

A4 portrait; at least 16 mm side margins; embedded Vera sans-serif fonts; 10 pt
body with 14.4 pt leading and 9 pt table text. Restrained navy/teal headings and
high-contrast neutral tables distinguish procedure from control content. Headers
identify key/type/version. Every footer displays synthetic classification, exact
document ID/version and Page x of y. Tables repeat headers and wrap cells safely.
All metadata and substantive source text must survive text extraction.

Every PDF must be readable, unencrypted and text based. Reject malformed/scanned
or empty pages, extra/missing members, metadata disagreement, unknown risks/sources/
related keys, unsafe paths, duplicate identities and content outside page bounds.
Do not modify a generated PDF directly: edit its source, rebuild and revalidate.

## Freeze and review

Build from the inventory and maintained Markdown only. Deterministic ReportLab
generation must produce identical bytes on repeated builds in the pinned environment.
Record generator/library/font/input versions and hashes, exact source/PDF hashes,
page counts and extraction results. Freeze after editorial and all-page visual review.
The CLI refuses to overwrite the manifest; changes require a successor version.

Render every page with Poppler and inspect every cover, densest procedure/control
page and final governance/revision page at readable resolution. Record coverage,
defects and corrections in review.md. Any visual defect requires source/layout
correction, rebuild and reinspection before freeze. Automated bounds checks do
not establish visual correctness.

Frozen provenance also identifies the domain-input hashes and the historical
baseline's existing manifest hash separately. Historical PDFs are not copied or
relabeled independent, and historical membership/exclusion tests remain unchanged.
