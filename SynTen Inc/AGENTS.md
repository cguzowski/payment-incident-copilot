# SynTen Inc agent instructions

These instructions apply to all work under this directory and add to the
repository-root `AGENTS.md`.

## Required context

Before planning or changing SynTen Inc assets, read:

1. `README.md` in this directory.
2. `../docs/agent/tasks/current.md`.
3. Any existing profile, inventory, authoring standard, and evaluation contract
   directly relevant to the task.

## Location and ownership

- Keep all SynTen Inc-specific profiles, inventories, source text, PDFs,
  generation inputs, validation assets, and retrieval-evaluation fixtures in
  this directory.
- Keep shared application source code in its existing service directory and
  shared project facts or decisions in `docs/agent/`; link to tenant assets
  instead of duplicating them.
- The corpus inventory is authoritative for generated document membership and
  metadata once it exists.

## Synthetic-content guardrails

- Use only fictional people, systems, incidents, identifiers, operational
  history, runbooks, and policies.
- Never copy or lightly disguise confidential, proprietary, or sensitive
  material from a real organization.
- Preserve the distinction between fictional observed facts, operational
  guidance, AI inference, and actions reserved for human approval.
- Keep every document within the authorized tenant and incident-family scope.

## Artifact rules

- Preserve editable source inputs and reproducible generation metadata for each
  PDF; do not make an opaque binary the only maintainable source.
- Generated PDFs must be text-based, readable, unencrypted, and traceable to a
  stable document ID and version in the inventory.
- Generated PDFs must resemble credible real-world payment-operations runbooks
  or policies. Every PDF is limited to 15 pages inclusive of its cover,
  document-control pages, appendices, and revision history.
- Do not hand-edit generated PDFs. Change the maintained source and regenerate
  and revalidate the artifact.
- Do not commit model weights, embedding caches, database files, credentials,
  or temporary render output.
