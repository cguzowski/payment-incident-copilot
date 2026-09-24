# Task: Consolidate and refresh repository Markdown

Status: Complete
Created: 2026-09-24
Owner: Christopher Guzowski

## Goal

Make current documentation concise, factual, and consistent while preserving
historical acceptance evidence and versioned knowledge inputs.

## User story

As the repository owner, I want obsolete guidance removed and remaining
Markdown updated, so humans and agents can find the current facts without
reading duplicate histories or contradictory instructions.

## Chosen contract

- The owner authorized Markdown cleanup and deletion of the temporary audit.
- Current guidance reflects inspected code and clearly labels historical proof.
- Preserve completed task contracts, ADR history, and frozen corpus source
  bytes; document unresolved product limitations without changing behavior.
- Archive the previous completed task before replacing this file.

## In scope

- Consolidate project, status, roadmap, setup, and service guidance.
- Remove the superseded verification proposal and missing-image reference.
- Correct factual wording, provenance availability, and supersession notices.
- Run documentation checks and repository verification.

## Out of scope

- Executable changes, dependency upgrades, corpus regeneration, or reindexing.
- New live-model evaluation, authentication, deployment, or grading behavior.

## Acceptance criteria

- [x] Current Markdown reflects inspected runtime and verification behavior.
- [x] Repeated current-state histories are consolidated into canonical files.
- [x] Historical task contracts and versioned knowledge inputs are preserved.
- [x] Local Markdown links and repository verification pass.
- [x] The temporary markdown-audit.md is deleted after its findings are handled
      through corrections or explicit limitations in canonical documentation.

## Test plan

- Check local Markdown links, retired references, and final diff scope.
- Verify archived R2 contract sections and frozen corpus bytes are preserved.
- Run ./verify.ps1 -Scope Repository; no production behavior changes require
  the full application test suites.

## Progress notes

- 2026-09-24: Inspected current instructions, application settings, retrieval
  thresholds, startup scripts, verification code, and tracked evidence.
- 2026-09-24: Consolidated current guidance, removed the superseded E1 proposal
  (implemented under B01-B06), archived R2, and corrected setup, provider,
  lifecycle, benchmark, and evidence-availability descriptions. Product issues
  remain explicit in STATUS rather than being silently fixed by changing docs.

## Completion evidence

- `./verify.ps1 -Scope Repository` passed verification-system, local knowledge
  preparation, AI prerequisite, evaluation-runner, Compose, and diff checks.
- Local Markdown destinations resolved across all 92 remaining files.
- All 30 maintained source/PDF pairs matched their manifest SHA-256 values.
- Inventory, evaluation-label, and threshold tables matched HEAD exactly.
- Archived R2 Goal, User story, Chosen contract, In scope, Out of scope,
  Constraints, Acceptance criteria, Test plan, and Decisions needed sections
  matched the original. A factual token-cap note was added to its evidence.
- Final diff review found only Markdown changes; no source/PDF bytes, benchmark
  labels, application behavior, or dependencies changed.
- Deleted the temporary markdown-audit.md and verified its absence.
- Full application suites and live-model workflows were not rerun for this
  documentation-only change.

## Remaining limitations

Product limitations remain tracked in [STATUS.md](../STATUS.md).

## Decisions needed

None.
