# ADR-0020: Accepted payment PDF catalogs and operational eligibility

Status: Accepted under owner-selected E3 adoption
Date: 2026-10-01
Decision owner: Christopher Guzowski

## Context

The owner selected adoption of frozen synten-payment-knowledge/v1 rather than
conversion of twelve historical Markdown documents. The baseline pins 30 PDFs,
705 chunks, a single family per document and a different generated layout.

## Decision

Preserve the baseline plan and fingerprint. Accept the frozen library through
its exact freeze-manifest SHA-256, inventory/source/PDF hashes and separately
pinned 16-document / 65-chunk plan. Do not remove integrity checks to admit
arbitrary catalogs. Frozen source metadata owns stages, families and related keys.
incident_family retains the first declared family for compatibility;
applicable_families owns eligibility. Shared policies have explicit family scope.

PDFs identify the fictional Knowledge Approver role and an effective date.
Map that role to stable synthetic identity b3154ad3-6d18-4fb0-867a-55fe2c676b23
and date-only approval/effectivity to UTC midnight of that date. These ingestion
conventions describe fictional knowledge control, not a real clock time or a
human incident decision.

Use pdfbox-payment-pages/v1 for the new layout, removing only exact generated
header/footer/control lines. Preserve page confinement, deterministic blocks
and the existing 400/600/50 chunking contract. Preserve baseline extraction and IDs.

V11 adds catalog_version and applicable_families. Reimport fills absent derived
metadata but rejects changed bound metadata. Readiness validates applicability
along with exact persisted chunks. Embedding snapshots isolate catalogs; backfill
validates each accepted fingerprint's target count and publishes vectors atomically.
Embedding snapshots inspect only the selected catalog tag. Untagged historical
versions remain auditable and do not block preparation; explicit reimport tags
the exact accepted versions before readiness. Missing tagged versions still fail.

Operational searches default to PDF-only. Both direct and bounded related-policy
searches filter accepted catalog versions, tenant, approval, effectivity and
declared family before ranking. Retain thresholds, context capacities and policy
scoring. Record successor eligibility as postgres-pdf-family-related/v5. Explicit
legacy regression mode preserves postgres-hybrid-related/v4 and Markdown behavior;
historical snapshots and source artifacts remain unchanged.

Preparation imports/embeds baseline, then successor, then checks both catalogs.
Ordinary local startup checks readiness without ingestion/provider calls. Missing
files, drift, absent/mixed/incompatible vectors prevent readiness. Never fall
back to Markdown. Package frozen PDFs for exact hash-based reviewer links.

## Consequences and verification

No new service, parser dependency, PDF rewrite, evidence tool or report change.
Aggregate errors still cannot establish item-level outcomes. Adoption tests are
technical regressions, not independent model-quality or universal-coverage claims.
The active task records deterministic plans, input rejection, import atomicity,
embedding coexistence/idempotency, policy/family eligibility and exclusion checks.
