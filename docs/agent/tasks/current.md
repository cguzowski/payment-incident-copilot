# Task: Refresh scope and independent payment-expansion plan

Status: Complete — documentation review and Repository verification passed
Created: 2026-10-01
Owner: Christopher Guzowski

## Goal

Make maintained Markdown distinguish implemented behavior from the approved
multi-family, independent-PDF expansion and define a clear sequence.

## User story

As the owner, I want stale guidance corrected and a clear planned path so future
work builds broad operational knowledge before independent demo incidents.

## Chosen contract

The owner authorized documentation cleanup and planning only. The canonical
PAYMENT_EXPANSION_PLAN.md owns phases, gates, verification and open decisions.
Archive completed Q6 before replacing this task, correcting only its relocated
relative link. Preserve historical
ADRs, source/PDF bytes, manifests, evaluation labels and measured results.

## In scope

Project scope, constraints, roadmap, status, README corrections, baseline corpus
notices, domain terminology and the expansion plan.

## Out of scope

Executable changes, PDF generation, incident creation, database/runtime changes,
deployment and new architecture/API contract selection.

## Constraints

Label future behavior honestly. Retain synthetic-only and human-review boundaries.
No universal coverage claim or scenario-to-runbook design. Existing v2 contracts
remain reproducibility authorities for the baseline only.

## Acceptance criteria

- [x] Guidance distinguishes current single-family behavior from multi-family
      direction and identifies E1 as the next implementation objective.
- [x] Plan freezes independent risk-derived PDFs before independent scenarios,
      including clear, ambiguous, compound and unresolved cases.
- [x] Stale README claims and roadmap links are corrected; historical Q6 and
      versioned corpus/evaluation evidence are preserved.
- [x] Changed Markdown links and diff are reviewed; git diff --check and
      ./verify.ps1 -Scope Repository pass.

## Test plan

Documentation-only: consistency/link review maps to criteria one and two.
Archived Q6 byte comparison and diff scope map to criterion three. Static link
checks and Repository verification map to criterion four. No artificial tests
or full application rerun are required.

## Progress notes

- 2026-10-01: Clean initial Git status. Reviewed canonical guidance and found
  stale benchmark/reveal claims, single-family restrictions and scenario-informed
  authoring requirements. Archived completed Q6 unchanged.

## Completion evidence

- 2026-10-01: ./verify.ps1 -Scope Repository passed verification-system,
  knowledge-preparation, AI-prerequisite, retrieval/report-evaluation runner
  tests, Compose validation and diff checks.
- Reviewed changed guidance and the six-phase plan against owner direction.
  Local Markdown file links pass static existence checks. Q6 archive matched the
  original before correcting its single relative link for the new directory.
- Final diff contains documentation only. No versioned knowledge sources, PDFs,
  manifests, evaluator labels, executable files or generated output changed.

## Remaining limitations

New families, evidence tools, independent PDFs, PDF-only retrieval and held-out
incidents remain unimplemented. Phase-specific decisions are listed in the plan.

## Decisions needed

None for documentation; implementation phases require separate task contracts.
