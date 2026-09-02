# Task: Define the SynTen Inc PDF knowledge corpus

Status: Completed
Created: 2026-08-31
Owner: Christopher Guzowski

## Goal

Create an approved, implementation-ready contract for a substantial synthetic
PDF corpus of SynTen Inc runbooks and policies.

## User story

As a payment operations analyst, I want a representative body of approved
SynTen Inc operational knowledge, so later retrieval and report evaluations
exercise realistic document selection instead of two hand-authored fixtures.

## Context

The initial end-to-end vertical slice is complete. Its knowledge path uses two
repository-owned Markdown documents and deterministic model boundaries. The
next phase expands source realism before changing ingestion, chunking,
embedding, vector search, or report generation.

SynTen Inc is the display name of the existing synthetic tenant
`8b860d80-d17f-4e6b-8c48-af35f26a4d61`; it is not a second tenant. All
tenant-specific assets for this phase belong under `SynTen Inc/`. This task
defines the corpus that the following task will generate as PDFs.

## Chosen contract

- The corpus belongs only to SynTen Inc and contains synthetic information.
- The corpus remains within `AUTHORIZATION_DECLINE_RATE_SPIKE` and includes both
  runbooks and policies.
- A version-controlled inventory under `SynTen Inc/` is the authority for the
  exact document set, stable identifiers, types, versions, applicability,
  approval/effective status, filenames, and intended retrieval role.
- The corpus is large enough to exercise relevant selection, overlapping
  terminology, weak matches, and metadata exclusions; this task records the
  exact count and distribution before PDF generation begins.
- Future PDFs are human-readable, text-based source artifacts. They are not
  pre-chunked model context and must not contain real payment or company data.
- Every PDF must resemble a document a real payment-operations organization
  could use, with credible structure, document control, ownership, procedures,
  tables, cautions, escalation paths, and revision history appropriate to its
  type. No PDF may exceed 15 pages, including its cover, document-control pages,
  appendices, and revision history.
- PDF generation is the next task. PDF extraction/chunking, embedding/indexing,
  retrieval evaluation, and live report generation remain later, separately
  verified tasks.

## In scope

- Define the minimal fictional SynTen Inc company and payment-operations
  profile needed to author coherent documents.
- Define the exact corpus inventory and coverage map for runbooks and policies.
- Reuse the existing tenant UUID and the knowledge metadata required by
  ADR-0002 and ADR-0007.
- Define content rules that keep observed facts, operational guidance,
  escalation criteria, evidence limitations, and human authority distinct.
- Define the PDF authoring and static-validation contract, including stable
  filenames, readable text, page numbering, version labeling, and a one-to-one
  inventory-to-file check.
- Define the realism review and hard 15-page validation required for every PDF.
- Define retrieval-evaluation questions and expected eligible source documents
  that later phases can use to measure the pipeline.

## Out of scope

- Generating the PDF files.
- Selecting or implementing a PDF extraction library.
- Changing the current Markdown parser or chunker.
- Calling an embedding or chat model, writing vectors, or changing retrieval.
- Adding a second tenant, another incident family, authentication, or AWS
  deployment.

## Constraints

- Use synthetic content only; do not adapt confidential or proprietary company
  material.
- Keep every tenant-specific profile, inventory, source, generated artifact,
  validator, and evaluation fixture under `SynTen Inc/`.
- Preserve the single-tenant and single-incident-family product boundary.
- Keep the corpus internally coherent while retaining deliberate retrieval
  challenge cases as explicitly labeled test design, not accidental conflict.
- Preserve document/version provenance and the responsible-AI guardrails in
  `CONSTRAINTS.md`.
- Treat 15 pages as a hard inclusive maximum for each generated PDF, not a
  target that may be exceeded for appendices or front matter.
- Do not revise ADR-0002's chunking decision in this task; the PDF ingestion
  task requires a new accepted ADR first.

## Acceptance criteria

- [x] SynTen Inc has one concise fictional company and payment-operations
      profile mapped to the existing synthetic tenant UUID.
- [x] A version-controlled inventory records the exact target count and every
      planned PDF's stable ID, filename, type, title, version, incident family,
      applicability, approval/effective status, and retrieval role.
- [x] A coverage map shows why each document exists and prevents repetitive
      filler from being counted as corpus depth.
- [x] Authoring rules prohibit real or sensitive data and define how runbooks,
      policies, evidence limits, escalation steps, and human decisions are
      represented consistently.
- [x] The PDF artifact contract is specific enough to validate file count,
      inventory membership, metadata/version labels, text extraction, page
      numbering, rendering, and absence of encrypted or scanned-only files.
- [x] The authoring and review contract requires operationally credible,
      real-life document structure and automatically rejects any PDF over 15
      pages, counting all front matter and appendices.
- [x] Retrieval-evaluation cases identify expected eligible sources and
      meaningful weak or excluded matches without depending on model output.
- [x] The subsequent PDF-generation task can be activated with no unresolved
      material corpus-design decision.
- [x] Markdown formatting and repository diff checks pass.

## Test plan

- Inventory completeness -> static one-to-one document ID and filename review.
- Coverage and evaluation mappings -> manual review against the current
  incident family, MCP evidence vocabulary, and ADR-0002 metadata filters.
- Synthetic-data and PDF rules -> sensitive-pattern review and checklist.
- Realism and page limit -> per-document editorial checklist plus automated PDF
  page-count assertion of 1-15 inclusive.
- Documentation integrity -> `git diff --check` and Markdown reference review.

## Expected approach

1. Inventory the existing incident, evidence, knowledge, retrieval, and report
   vocabulary that the corpus must support.
2. Define the smallest coherent SynTen Inc profile needed by the documents.
3. Select and record the exact runbook/policy distribution and coverage map.
4. Define PDF authoring, metadata, validation, and retrieval-evaluation rules.
5. Review the contract for repetition, real-data risk, unresolved decisions,
   and compatibility with accepted ADRs.

## Likely files or components

- `SynTen Inc/README.md`
- `SynTen Inc/profile.md`
- `SynTen Inc/corpus/inventory.md`
- `SynTen Inc/corpus/authoring-standard.md`
- `SynTen Inc/evaluation/retrieval-cases.md`

## Validation commands

```powershell
git diff --check
rg -n "SynTen Inc|AUTHORIZATION_DECLINE_RATE_SPIKE|RUNBOOK|POLICY" "SynTen Inc" docs/agent
```

## Decisions needed

None before starting. Selecting the exact corpus size, taxonomy, company
profile, and PDF authoring standard is the observable outcome of this task and
must be completed before PDF generation is activated.

## Progress notes

- 2026-08-31: The owner declared the initial end-to-end vertical slice complete
  and selected SynTen Inc, a substantial PDF runbook/policy corpus, and later
  live chunking, embedding, vectorization, retrieval, and report evaluation as
  the next direction.
- 2026-08-31: Preserved the completed queue-refresh task and activated this
  corpus-definition contract. SynTen Inc reuses the existing synthetic tenant
  UUID so the phase does not introduce an unintended second tenant.
- 2026-08-31: The owner selected the repository-level `SynTen Inc/` directory
  as the home for all tenant-specific assets.
- 2026-08-31: Before implementation began, the owner required realistic
  real-life runbook/policy presentation and a hard maximum of 15 pages per PDF.
  The contract was updated with those requirements and implementation began.
- 2026-08-31: Defined a 30-PDF corpus with 22 runbooks and 8 policies, including
  27 approved versions and 3 superseded hard negatives. The coverage map spans
  all 36 synthetic generator scenarios.
- 2026-08-31: Added the fictional operating profile, realistic controlled-
  document standard, reproducible source/PDF contract, hard 15-page validator,
  and 23 human-labeled retrieval cases covering all 49 scenario error codes.
- 2026-08-31: Mechanical inventory, metadata, page-budget, scenario, error-code,
  evaluation-reference, sensitive-pattern, whitespace, and repository checks
  passed. K1 is complete and K2 can be activated without a corpus-design gap.

## Completion evidence

- Profile: `SynTen Inc/profile.md` defines the fictional company, platform,
  roles, evidence model, authority boundaries, and controlled vocabulary.
- Inventory: 30 distinct document versions validated—22 runbooks, 8 policies,
  27 approved versions, and 3 superseded versions—with unique keys, filenames,
  and document-ID/version identities.
- Coverage: all 36 scenarios in the generator catalog map to approved runbook
  guidance; all 49 distinct scenario error codes appear in the fixed evaluation
  oracle.
- Evaluation: 23 cases validated with existing approved primary/supporting
  document references and global exclusion rules for superseded versions.
- PDF contract: every document has an editorial target at or below 12 pages and
  an automated hard acceptance range of 1-15 inclusive, plus full render and
  visual-review requirements.
- Static verification: `./verify.ps1 -Scope Repository`, `git diff --check`,
  tenant-file trailing-whitespace review, and sensitive-pattern review passed.

## Remaining limitations

No source documents or PDFs have been generated yet; that is K2. PDF extraction,
chunking, embedding, vectorization, and live report generation remain K3-K5.
Ollama plus the pinned local models remain external prerequisites for K4-K5.

