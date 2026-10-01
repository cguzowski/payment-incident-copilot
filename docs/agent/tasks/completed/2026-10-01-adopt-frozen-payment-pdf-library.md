# Task: E3 — Adopt the frozen payment PDF library

Status: Complete — final full verification and local PDF retrieval checks passed
Created: 2026-10-01
Owner: Christopher Guzowski

## Goal

Adopt synten-payment-knowledge/v1 for operational retrieval with the same exact
PDF citations and explicit embedding preparation as the authorization baseline.

## User story

As an operator investigating any supported incident family, I want approved,
applicable PDF excerpts and shared policies with exact source versions, hashes,
pages and blocks, so I can review the guidance supporting an advisory report.

## Chosen contract

Accept unchanged synten-auth-knowledge/v2 and frozen 16-document
synten-payment-knowledge/v1 through separately pinned plans. Use declared
inventory families for applicability, including shared policies. Retain bounded
source-derived related-policy ranking. New operational searches are PDF-only.
Preserve Markdown inputs and historical snapshots. Explicit preparation validates
all hashes before atomic import, then embeds. Repeated preparation is idempotent.
Missing/changed PDFs or incomplete/failed embeddings prevent readiness; never
fall back to Markdown. Preserve NO_MATCH/UNAVAILABLE semantics.

## In scope

Accepted catalogs, layout-specific extraction, multi-family metadata, PDF-only
eligibility, source packaging, explicit preparation/readiness, tests, ADR and docs.
Local preparation and representative retrieval/source checks.

## Out of scope

Source/PDF rewrites, new fixtures, E4 evidence, report/schema/Q6 changes,
autonomous actions, historical rewrites and held-out quality claims.

## Constraints

Preserve baseline bytes/chunks/fingerprint, tenant/version/approval isolation,
independent deployables and deterministic automated providers. Legacy tests may
explicitly exercise Markdown compatibility; operational default is PDF-only.

## Acceptance criteria

- [x] Both accepted catalogs are pinned, deterministic and hash-validated;
      missing, changed, invalid or unaccepted inputs fail before persistence.
- [x] Successor chunks retain exact PDF/page/block/model provenance, declared
      family applicability and shared-policy relationships; imports are atomic
      and repeatable, preserving historical records.
- [x] Direct and related operational searches exclude Markdown, wrong tenant,
      wrong family, future/superseded and unaccepted sources; all seven families
      have PDF guidance. Historical citations remain readable.
- [x] Explicit preparation covers both catalogs; readiness rejects missing,
      incomplete or failed embeddings without automatic import.
- [x] Focused and full verification pass; local preparation and representative
      retrieval/source checks are recorded separately from model quality.

## Test plan

PaymentPdfCatalogTest: deterministic plan, immutable baseline regression,
source/PDF hash/path/metadata and missing-file rejection.
PaymentPdfCatalogPostgresIntegrationTest: combined atomic/idempotent import,
shared/family policy eligibility, provenance, exclusions, empty catalog,
historical Markdown preservation and embedding readiness.
LocalKnowledgePreparation.Tests.ps1: separate catalog/backfill/readiness steps
and disabled Markdown import. Existing embedding failure and artifact endpoint
tests remain regression coverage. Confirm intended red, then focused suites and
full verify.ps1. Manual local checks use the configured synthetic environment.

## Progress notes

- Owner selected frozen-library adoption (option 1). Existing staged report/CLI
  changes are user-owned and preserved; completed task archived.
- Risks: layout differences, multi-family applicability and catalog coexistence
  in embedding snapshots. Resolve through focused regressions before production.
- Confirmed red: successor loading initially rejected its absent legacy manifest;
  shared policies were absent for six families and Markdown entered PDF-only
  searches. Catalog coexistence and the 65-target backfill failed under the
  original one-catalog/705-target assumptions. All pass after implementation.
- Applicability drift and conflicting readiness/import modes reproduced before
  correction. Legacy wrong-family exclusion caught an unintended compatibility
  change; legacy mode now retains the original single-family filter.
- Full gate passed 361 API, 9 MCP, 52 generator and 103 console tests before the
  final historical-version fix. A generator JAR lock required a scoped stop;
  generator restored from byte-identical runtime copy to avoid future clean locks.
- Local database contained 30 untagged historical PDF versions (705 chunks),
  fourteen Markdown versions (87 chunks) and subsequently imported thirty v2
  PDFs (705 chunks). Backfill failed on the retained untagged versions. A focused
  regression reproduced the failure; scoped catalog snapshots now preserve and
  ignore those historical versions. Five preparation tests passed afterward.
- Frozen library verify and eight tooling tests passed; sources, PDFs, inventory,
  freeze manifest and package-control hashes remain unchanged. Final full gate
  and local preparation/retrieval checks subsequently passed.

## Completion evidence

- Final ./verify.ps1 passed on 2026-10-01: 362 API, 9 operations MCP,
  52 generator and 103 console tests, zero failures/errors/skips; PowerShell
  script suites, Java/Prettier formatting, production builds, Compose and diff
  checks passed. Docker-backed commands required elevation for named-pipe access.
- Focused command ./mvnw.cmd -pl backend/copilot-api
  '-Dtest=PaymentPdfPreparationPostgresIntegrationTest' test passed five cases
  after the final historical-version fix. Earlier focused suites covered the
  four catalog input tests, all seven family searches/exclusions, readiness,
  mode conflicts, strategy provenance, baseline fingerprint and exact packaged
  PDF bytes. Existing embedding/migration/snapshot/report suites passed in the gate.
- Bundled Python: library.py verify and unittest discover -s
  'SynTen Inc/payment-knowledge/v1/tools' -v passed freeze verification and
  eight tooling tests. Temporary fixture access required elevation. No frozen
  library, baseline corpus, domain or historical evaluation bytes changed.
- Executed the six-step Get-LocalKnowledgePreparationPlan against the configured
  synthetic database using the existing launcher functions. Baseline reimport
  skipped 30 identical v2 versions; backfill wrote 705 nomic-embed-text vectors.
  Successor imported 16 versions / 65 chunks and wrote 65 vectors. Both readiness
  commands passed their exact fingerprints/counts. Retained 30 untagged historical
  PDF versions / 705 chunks and fourteen Markdown versions / 87 chunks unchanged.
- Live final checks (all AVAILABLE, seven PDF chunks, nomic-embed-text / 768,
  postgres-pdf-family-related/v5):
  authorization investigation da62c985-a5c2-44c3-ac13-1d8b0bec8a24, retrieval
  a2bdacb2-9ab4-4dcb-a1c7-8eaff3e96665;
  capture 8245ff66-ce77-4ceb-8567-9f44fd1a07cf, retrieval
  9f9eaf77-a166-40ab-aaaa-b3feb21cbebd;
  reconciliation 94deb248-9dea-44e6-bcbb-d22ca6dce5d0, retrieval
  eb6b3e7f-3ca8-46ef-8108-b61f3ade39cc;
  webhook f1c24df6-ec42-4d48-b70e-f58f458fd576, retrieval
  dde80050-2326-452b-ab96-90e4fcad8b83.
  Downloaded first-cited PDF bytes matched each SHA-256, with valid page/block
  locators. All previous retrieval responses were byte-equivalent after JSON
  normalization. Every incident stayed INVESTIGATING; no reports/decisions created.
  An initial authorization retrieval also succeeded before correcting the temporary
  check script's array handling; that attempt remains retained.
- API and generator health, console root and effective API MCP endpoint checks
  passed (HTTP 200; MCP http://localhost:8082). API PID 37328, generator PID 38000,
  console PID 26044 left running. API/generator use identical copies of verified
  JARs under ignored tmp/e3 to avoid Windows clean-build locks.
- Changed application files are confined to knowledge catalog/retrieval, V11,
  PDF resource packaging and launcher preparation. ADR-0020 and current docs
  describe the resulting contract. Prior staged report/CLI work remains untouched.

## Remaining limitations

E4 item-level evidence and independent live-model quality evaluation remain
deferred. Live checks establish retrieval availability and source provenance,
not cause correctness or report quality. Capture S312 evidence remains deliberately
UNAVAILABLE; knowledge availability does not repair or relabel that evidence.
The immutable library README records its E2 adoption boundary; current adoption
instructions live in the root/tenant README and ADR-0020 to preserve the freeze.

## Decisions needed

None: owner selected E3; inventory-declared applicability, separately pinned
accepted catalogs and fail-closed explicit readiness implement the contract.
