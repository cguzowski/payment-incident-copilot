# Agent context map

This directory contains shared, version-controlled context for both humans and
coding agents. It is not a transcript archive and must not contain secrets.

## Always read

1. `PROJECT.md` — why the product exists and what the MVP includes.
2. `CONSTRAINTS.md` — boundaries that implementation must respect.
3. `STATUS.md` — current state, next milestone, and known blockers.
4. `tasks/current.md` — the one active implementation outcome.

For executable changes, also read `QUALITY.md` and the nearest service-specific
`AGENTS.md`.

## Read when relevant

- `ARCHITECTURE.md` — cross-service ownership and data flow.
- `DOMAIN.md` — shared payment-operations vocabulary.
- `decisions/` — accepted decisions affected by the task.

## Canonical ownership

Keep each fact in one place:

| Topic | Canonical file |
|---|---|
| Product goal and MVP scope | `PROJECT.md` |
| Non-negotiable boundaries | `CONSTRAINTS.md` |
| Current facts and blockers | `STATUS.md` |
| Active acceptance criteria and evidence | `tasks/current.md` |
| System boundaries and flow | `ARCHITECTURE.md` |
| Domain vocabulary | `DOMAIN.md` |
| Test and verification policy | `QUALITY.md` |
| Consequential decisions | `decisions/` |

## Maintenance rules

- Keep durable instructions in `AGENTS.md`, not in task files.
- Link to canonical context instead of copying it into multiple files.
- Keep only one `tasks/current.md` active at a time.
- After all criteria and evidence are complete, archive the task as
  `tasks/completed/YYYY-MM-DD-short-name.md`; create the directory when first
  needed.
- Record consequential decisions as ADRs instead of silently rewriting history.
- Update documentation in the same change as the behavior it describes.
- Delete or correct stale context immediately.
- Never paste full AI conversations into the repository.
