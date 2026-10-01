# Task: E2 independent operational PDF library

Status: Complete - independent library frozen; full verification passed
Created: 2026-10-01
Owner: Christopher Guzowski

## Goal

Author, validate and freeze a risk-derived synthetic PDF library before independent
incident creation, preserving the existing authorization corpus.

## User story

As an operator, I want varied approved operational guidance that helps distinguish
causes, effects and safe next evidence requests without containing incident answers.

## Chosen contract

The owner's proceed activates E2 from PAYMENT_EXPANSION_PLAN.md. Create a separate
synten-payment-knowledge/v1 package under SynTen Inc/payment-knowledge/v1 with
16 new documents: 11 diagnostic runbooks and 5 shared policies. Derive topics and
coverage solely from domain/v2's 24 risks and 11 evidence requirements. Use stable
opaque document IDs, version 1.0.0, synthetic APPROVED metadata, owner roles,
effective date 2026-10-01 and explicit stage/family/shared applicability. Preserve
30 original PDFs and their manifests as a separately identified historical baseline;
do not relabel it independent. Freeze exact editable sources, PDFs, inventory,
authoring/generator provenance and review evidence. No runtime import is activated.

## In scope

Successor authoring standard/inventory, source/PDF generation, deterministic asset
validation, extraction, all-page visual/editorial QA, hashes/freeze and navigation.

## Out of scope

Incident/oracle/evaluation creation, application API/MCP/database/retrieval changes,
live embeddings/model runs, PDF-only runtime enforcement or report-safety changes.

## Constraints

No authoring from scenarios, labels, report outputs or oracle. Every PDF <=15 pages
including control/revision material. Only synthetic records and units; guidance is
not proof. E02-E11 remain unavailable current sources. Shared policies do not imply
implemented cross-family retrieval. Preserve Q6 LOW/null and human-only actions.

## Acceptance criteria

- [x] Versioned authoring standard and exact 16-document inventory map every R01-R24
      to useful diagnostic or policy content with scope, sources and related items.
- [x] Editable, independently risk-derived sources and text PDFs contain controlled
      metadata, distinguishing/negative checks, uncertainty and conditional recovery;
      policies define durable controls without copying runbook procedures.
- [x] Deterministic generation and validators verify exact membership/metadata/hashes,
      extraction, 1-15 pages, related references and invalid input rejection.
- [x] Every rendered page passes visual review; all documents pass editorial review
      with source-capability limits and no unresolved layout defects.
- [x] Frozen package retains exact provenance/hashes and review evidence; historical
      assets unchanged; required focused and repository verification pass.

## Test plan

Before tooling implementation, write focused Python tests for missing/extra files,
duplicate metadata, missing risk coverage, unsafe source paths, source mismatch,
PDF limits/text/encryption and altered freeze hashes. Confirm intended red failure,
then focused green. Generate twice and compare PDF bytes. Validate/extract all PDFs
and render every page with Poppler; inspect every cover, procedure/control page and
revision page. Review every source for operational relevance and independence.
Run ./verify.ps1 and static links/diff/preservation checks; live providers not needed.

## Progress notes

- Integrated all current worktree histories after E2 completion. Full combined
  ./verify.ps1 passed (503 Java/Angular tests, no failures/errors/skips), eight
  PDF tooling tests passed and frozen hashes remained unchanged. The independent
  evidence-availability task is preserved in completed/2026-10-01-available-demo-evidence.md.

- Required context and PDF skill read; clean initial working tree.
- Archived completed E1 unchanged. Existing authoring contract remains historical;
  successor intentionally uses independent risks rather than scenario-code coverage.

## Completion evidence

- Red: focused unittest discovery failed with ModuleNotFoundError for missing
  library builder/validator before implementation. Green: all eight focused tests
  passed, including deterministic build, invalid membership/identity/risk/source/
  references/path/metadata, encrypted/empty/malformed/over-limit PDFs and tampering.
- Validated 16 PDFs / 48 pages with full metadata/substantive text extraction;
  each document is three pages (min/max/median 3). Two rebuilds were byte-identical
  and matched the exact PDFs used for the final visual inspection.
- Poppler rendered all 48 pages at 96 dpi. Inspected every cover, procedure/control
  and final revision page at readable resolution. Corrected merged numbered steps
  and added visible tenant metadata before final all-page reinspection. No remaining
  layout defects. Editorial review of all sources and coverage is in review.md.
- Full ./verify.ps1 passed on 2026-10-01: 337 API, 9 MCP, 50 generator and 103
  Angular tests, zero failures/errors/skips; format/build/Compose/repository checks.
  Initial full run correctly rejected skipped PostgreSQL tests while Docker was
  unavailable. Started Docker and reran with Docker access; the complete gate passed.
- Focused commands (bundled Python): -m unittest discover -s
  "SynTen Inc/payment-knowledge/v1/tools" -v; tools/library.py build/validate/verify;
  tools/render_library.py. Actual freeze command ran once after review; a second
  freeze attempt was rejected and left the manifest byte-identical.
- Freeze SHA-256: a1fe13333de61f51a9406827a577f880ca7d88b1bcd2e0a4def438f6b134b421.
  Manifest retains exact source/PDF/package/generator/font/domain-input hashes,
  ReportLab 4.4.9 and UTC freeze timestamp. Input allowlist excludes scenario/
  oracle/evaluation/report artifacts; historical manifest is a hash-only reference.
- Changed/new Markdown links, label/credential pattern checks and git diff --check
  passed. Historical corpus/evaluation/multi-incident/domain/runtime paths have
  no diff against 4d6effd. Completed E1 archive text matches exactly.
- npm ci reported eight existing advisories (four moderate, four high) with no
  lockfile change; the gate has no failing npm-audit step.

## Remaining limitations

E3 must select accepted catalogs, PDF-only eligibility and shared-policy retrieval.
E4 must implement richer read-only evidence. No held-out quality claim is made.

## Decisions needed

None for E2 assets; existing baseline kept separate, new metadata explicitly scoped
for later catalog design. Fictional knowledge approval is not a human report decision.
