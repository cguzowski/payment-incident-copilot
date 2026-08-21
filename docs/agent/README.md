# Agent context map

This directory contains shared, version-controlled context for both humans and
coding agents. It is not a transcript archive and must not contain secrets.

## Reading order

1. `PROJECT.md` — why the product exists and what the MVP includes.
2. `CONSTRAINTS.md` — boundaries that implementation must respect.
3. `STATUS.md` — current state, next milestone, and known blockers.
4. `tasks/current.md` — the one active implementation outcome.
5. `ARCHITECTURE.md` — components, ownership, and end-to-end flow.
6. `DOMAIN.md` — shared payment-operations vocabulary.
7. `QUALITY.md` — validation commands and definition of done.

## Maintenance rules

- Keep durable instructions in `AGENTS.md`, not in task files.
- Keep only one `tasks/current.md` active at a time.
- Move completed task briefs into `tasks/completed/` when useful.
- Record consequential decisions as ADRs instead of silently rewriting history.
- Update documentation in the same change as the behavior it describes.
- Delete or correct stale context immediately.
- Never paste full AI conversations into the repository.
