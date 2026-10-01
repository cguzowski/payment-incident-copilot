# SynTen independent payment knowledge v1

Status: Frozen after E2 editorial and all-page visual review
Corpus: `synten-payment-knowledge/v1`
Domain: `synten-payment-domain/v2`
Classification: Internal - Synthetic Demo

This package contains 16 new PDFs and their editable sources: 11 diagnostic
runbooks and 5 shared policies, 48 pages total. Every document is three pages,
within the inclusive 15-page limit. Topics derive from the independent domain
and risk inventory, not selected incidents or expected answers. See the
[inventory](inventory.md), [authoring standard](authoring-standard.md),
[review evidence](review.md) and [freeze manifest](freeze-manifest.json).

The 30 [historical authorization PDFs](../../corpus/inventory.md) remain separate
and unchanged. Their manifest hash is retained as a baseline reference; they are
not relabeled independent or copied into this package. The fictional approval
metadata in these documents is knowledge control, not a human incident decision.

## Coverage and source limits

The library covers all 24 domain risks through rejection/uncertain outcome,
duplicates, capture, release, refund, settlement, delivery, reconciliation,
dependencies and incomplete/conflicting-source guidance. Policies cover evidence,
retry/amounts, time/cutoffs, tenant/security and human authority. Risk coverage is
bounded editorial coverage, not proof that every incident is solvable or that
retrieval/model output is correct. Shared metadata is not current shared-policy
retrieval implementation.

Only E01 aggregate service errors are exposed by current MCP. E02-E11 are future
read-only source requirements. Each document states what cannot be established
without them and preserves uncertainty, Q6 LOW/null and human-only operational
authority. No live provider or held-out evaluation was used for authoring.

## Reproduce and verify

Use Python with ReportLab, pdfplumber and pypdf. The bundled Codex runtime provides
these packages; the exact ReportLab/font versions and hashes are in the manifest.
Run from the repository root:

```powershell
python -m unittest discover -s "SynTen Inc/payment-knowledge/v1/tools" -v
python "SynTen Inc/payment-knowledge/v1/tools/library.py" verify
python "SynTen Inc/payment-knowledge/v1/tools/library.py" build
python "SynTen Inc/payment-knowledge/v1/tools/library.py" validate
python "SynTen Inc/payment-knowledge/v1/tools/render_library.py"
```

Poppler `pdftoppm` must be on PATH for rendering, or pass `--pdftoppm <path>`.
Renders go to ignored `tmp/e2-pdf-qa/`. The generator reads only inventory and
sources, uses embedded ReportLab Vera fonts and fixed PDF metadata, and produced
byte-identical PDFs on repeated builds. `verify` checks source/PDF/package/input
hashes plus provenance; an incompatible font/library version fails rather than
silently claiming reproduction. Build verifies an existing freeze before writing.

Do not edit a frozen source or overwrite the manifest. A change needs a successor
version, revalidation and review. The original freeze command refuses overwrite:
`python "SynTen Inc/payment-knowledge/v1/tools/library.py" freeze`.

## Adoption boundary

These PDFs are not imported into the application by E2. E3 must define accepted
catalogs, explicit preparation/readiness, PDF-only eligibility and stage/family/
shared-policy relationships while retaining historical citations and exclusions.
E4 adds richer read-only evidence. E5 creates independent incidents only after
the corpus freeze; historical cases remain regressions.
