# Agent context map

This directory holds maintained engineering context, not conversation transcripts.

## Required reading

Follow root [AGENTS.md](../../AGENTS.md) for required context, service-specific
rules, task locking, and verification. Do not duplicate those instructions in
task bodies.

## Canonical ownership

| Topic | Authority |
|---|---|
| Product goal and scope | [PROJECT.md](PROJECT.md) |
| Non-negotiable boundaries | [CONSTRAINTS.md](CONSTRAINTS.md) |
| Current state, limitations, latest verification | [STATUS.md](STATUS.md) |
| Latest authorized task and its evidence | [tasks/current.md](tasks/current.md) |
| Ordered future outcomes | [ROADMAP.md](ROADMAP.md) |
| System ownership and data flow | [ARCHITECTURE.md](ARCHITECTURE.md) |
| Shared vocabulary | [DOMAIN.md](DOMAIN.md) |
| Verification policy and commands | [QUALITY.md](QUALITY.md) |
| Architectural decisions | [decisions](decisions) |
| Tenant corpus and retrieval evaluation | [SynTen Inc](../../SynTen%20Inc/README.md) |

## Task and decision lifecycle

- Keep one authorized contract in `tasks/current.md`. Its Status makes clear
  whether work is active or complete; a completed task may remain there until
  another is activated.
- Before replacement, preserve the completed contract and evidence in
  `tasks/completed/YYYY-MM-DD-short-name.md`.
- A future proposal, if needed, belongs in `tasks/proposed/` and does not
  authorize implementation. Remove superseded proposals once the accepted
  decision and completion evidence are recorded elsewhere.
- Preserve historical task acceptance wording. Add dated corrections or
  disposition notes rather than implying an unrun check passed.
- Preserve ADR rationale. Mark partial supersession and link to the successor
  so historical choices cannot be mistaken for active defaults.

## Maintenance

Link to canonical facts rather than duplicating milestone lists. Update current
guidance with behavior changes; archive evidence instead of growing STATUS
into a diary. Retain intentionally versioned corpus sources and compatibility
fixtures even when they describe older guidance.
