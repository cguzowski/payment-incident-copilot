# Task: E1 independent payment domain and risks

Status: Complete - E1 editorial/static review and Repository verification passed
Created: 2026-10-01
Owner: Christopher Guzowski

## Goal

Define the synthetic payment domain, lifecycle risks and evidence requirements
before independent PDF authoring and later independent incident creation.

## User story

As the owner, I want a coherent operational domain and risk inventory independent
of incident answers so the next PDF library supports useful investigation,
including ambiguous and unresolved outcomes.

## Chosen contract

The owner activated E1 after approving the independent-corpus plan. Add a versioned
successor SynTen profile, lifecycle, risk inventory and evidence requirements
under SynTen Inc/domain/v2. Define conceptual synthetic states, source authority,
ownership, timing and amount semantics, and family/shared-policy applicability.
These are authoring contracts, not runtime schemas or new API enums. Reconcile
the plan with the seven-family ADR-0018 baseline. Archive completed retrieval
recovery unchanged. Preserve historical profiles, sources/PDFs, manifests,
scenarios, oracles and evaluations.

## In scope

Domain documentation, risk coverage matrix, evidence requirements, navigation,
status/roadmap updates and static verification.

## Out of scope

PDF/scenario creation, model tuning, executable changes, new MCP/API schemas,
migrations, deployment, report sufficiency changes and production payments.

## Constraints

Synthetic only; no new deployables or operational actions. Do not consume scenario
fixtures, labels, expected reports or oracle answers as authoring inputs. Current
aggregate errors cannot establish individual payment outcomes. Keep Q6 LOW/null
rules and human review. Domain concepts do not claim implemented evidence tools.

## Acceptance criteria

- [x] Successor profile defines scope, conceptual categories, owners and authority
      while preserving seven implemented families and the historical profile.
- [x] Lifecycle defines transitions, uncertainty, duplicates, reversal versus
      refund, amounts/timing and disagreement without inventing final outcomes.
- [x] Independent risk inventory maps lifecycle/dependency/control risks to
      distinguishing evidence and shared/family guidance without target incidents.
- [x] Evidence requirements define provenance, source authority, missing/stale/
      conflicting semantics and current versus future capabilities.
- [x] Links, preservation and consistency checks and Repository gate pass; status
      and roadmap identify E2 next without claiming PDFs/runtime work complete.

## Test plan

Documentation-only: editorial review of profile, lifecycle, risk and evidence
tables maps to criteria 1-4. Local Markdown links, risk/evidence ID cross-references,
historical-file diff checks and ./verify.ps1 -Scope Repository map to criterion 5.
No artificial production tests or live-model run is needed.

## Progress notes

- Reviewed required repository/tenant context and architecture. Previous planning
  changes committed and integrated while preserving newer completed-family facts.
- Initial working tree clean. Owner authorization covers E1; later phases remain
  separate tasks.

## Completion evidence

- ./verify.ps1 -Scope Repository passed on 2026-10-01, including verification,
  preparation/evaluation runner checks, Compose validation and diff checks.
- Static link check passed for every changed/new Markdown file. All 24 unique
  risk IDs and 11 source IDs resolve; each referenced evidence ID is defined.
- Editorial review mapped criteria 1-4 to the profile, transition table, risk
  matrix and source/uncertainty tables. Covered unknown outcomes, explicit issuer
  reasons, partial amounts, duplicate messages versus operations, reversal/refund,
  source disagreement, time/configuration gaps, owners and policy applicability.
- git diff aa4ccef --name-only over historical sources/PDFs/versions/manifest,
  evaluation, multi-incidents and runtime source paths returned no changes.
  Recovery archive text matches 2f74d81:docs/agent/tasks/current.md exactly.
- No production behavior changed; documentation-only static/editorial checks
  satisfy this phase without artificial red/green tests or a live-provider run.
- Planning changes retained as 817578f and integrated as 2f74d81, resolving stale
  single-family facts in favor of the completed seven-family baseline.

## Remaining limitations

The successor domain is an authoring authority only. Payment records, independent
PDF library, PDF-only retrieval and held-out incidents remain future work.

## Decisions needed

None for E1 documentation. Runtime schemas, corpus membership/approval metadata,
PDF eligibility and evaluation thresholds remain later phase decisions.
