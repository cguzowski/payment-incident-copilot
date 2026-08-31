# SynTen Inc

SynTen Inc is the fictional company name for the project's existing synthetic
tenant:

- Tenant ID: `8b860d80-d17f-4e6b-8c48-af35f26a4d61`
- Current incident family: `AUTHORIZATION_DECLINE_RATE_SPIKE`
- Data classification: synthetic demonstration data only

This directory is the canonical home for every SynTen Inc-specific profile,
source document, generated PDF, corpus manifest, validation asset, and
retrieval-evaluation fixture. Shared product, architecture, quality, and status
documentation remains under `docs/agent/` and links here when tenant-specific
detail is required.

## Layout

```text
SynTen Inc/
├── AGENTS.md
├── README.md
├── profile.md
├── corpus/
│   ├── inventory.md
│   ├── authoring-standard.md
│   ├── sources/
│   ├── pdfs/
│   ├── validation/
│   └── validation-manifest.json
└── evaluation/
    └── retrieval-cases.md
```

The K1 corpus contract is now defined in:

- `profile.md` — fictional company, system, role, and authority context;
- `corpus/inventory.md` — the exact 30-document corpus and scenario coverage;
- `corpus/authoring-standard.md` — realistic PDF composition, hard 15-page
  limit, generation, validation, and visual-QA requirements; and
- `evaluation/retrieval-cases.md` — 23 human-labeled retrieval cases covering
  all 36 generator scenarios.

K2 generated 30 maintained Markdown sources and 30 deterministic PDFs: 22
runbooks and 8 policies, comprising 27 approved and 3 superseded versions. The
corpus contains 112 pages in total; every PDF is 3-4 pages, text-extractable,
unencrypted, visually reviewed page by page, and below the hard 15-page limit.
`corpus/validation-manifest.json` records the exact source/PDF hashes, page
counts, extraction results, required error codes, status, and validation
outcome for each version. Shared task status remains authoritative in
`docs/agent/tasks/current.md`.

## Guardrails

- Treat SynTen Inc as fictional; do not incorporate real customer, payment,
  employee, merchant, or confidential company data.
- Keep the corpus within the current incident family until the roadmap
  explicitly authorizes expansion.
- Keep stable document IDs, versions, approval/effective status, hashes, and
  source provenance reviewable.
- Generated PDFs are source artifacts, not authoritative incident evidence and
  not preassembled model context.
- Each PDF must use credible real-world runbook or policy structure and must not
  exceed 15 pages, including front matter and appendices.
- Do not commit model binaries, credentials, local database state, or extracted
  third-party material here.
