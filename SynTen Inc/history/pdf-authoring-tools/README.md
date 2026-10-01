# Retired PDF-authoring tools

The demo consumes committed PDFs and their frozen metadata. It does not run
Python or regenerate documents during startup, preparation, tests or packaging.
Completed authoring tools were retired from the active tree on 2026-10-01.

[2026-10-01.zip](2026-10-01.zip) retains all 13 original Python files, byte for
byte, with repository-relative paths. [manifest.json](manifest.json) records
each file's size/SHA-256 and the archive SHA-256. It covers authorization corpus
v1/v2 and independent payment-library v1 builders, renderers, validators and
their tests. Frozen sources, PDFs, manifests, inventories, authoring contracts
and review records remain in their original locations with unchanged bytes.

Historical authoring instructions still name the original script paths. For
reproduction, use a separate checkout containing the corresponding frozen
assets, verify the archive hash, then restore those paths there:

```powershell
$archive = "SynTen Inc/history/pdf-authoring-tools/2026-10-01.zip"
$manifest = Get-Content "SynTen Inc/history/pdf-authoring-tools/manifest.json" -Raw | ConvertFrom-Json
if ((Get-FileHash $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $manifest.archiveSha256) {
    throw "Authoring archive hash mismatch"
}
Expand-Archive -LiteralPath $archive -DestinationPath .
foreach ($file in $manifest.files) {
    if ((Get-FileHash -LiteralPath $file.path -Algorithm SHA256).Hash.ToLowerInvariant() -ne $file.sha256) {
        throw "Authoring source hash mismatch: $($file.path)"
    }
}
```

Run the original package's authoring commands only in that reproduction checkout.
They still require compatible Python packages, ReportLab fonts and Poppler as
documented in the original records. The frozen payment manifest pins ReportLab
4.4.9 and exact tool/font hashes; matching files alone do not prove a compatible
rendering environment. Restore original frozen control-file bytes too; do not
rewrite a manifest to make a changed package pass. Application Java integrity,
PDF parsing, retrieval and startup readiness checks remain active.
