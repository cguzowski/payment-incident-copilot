[CmdletBinding()]
param([Parameter(Mandatory)][ValidateSet('investigation','source','evaluator')][string] $Application,
    [Parameter(Mandatory)][string] $Destination)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Import-Module (Join-Path $PSScriptRoot '../migration/Migration.psm1') -Force
$repository = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$targetRoot = [IO.Path]::GetFullPath($Destination)
if (Test-Path -LiteralPath $targetRoot) { throw 'Destination already exists; never overwrite an independent checkout.' }
$allowlist = Join-Path $PSScriptRoot "allowlists/$Application.json"
# The existing exporter validates every path/link/hash before any copy. Evaluator
# uses the same standalone layout as source; no application code is imported.
$exportKind = if ($Application -eq 'evaluator') { 'source' } else { $Application }
Export-IndependentApplication -RepositoryRoot $repository -Application $exportKind -Destination $targetRoot -AllowlistPath $allowlist
if ($Application -eq 'investigation') {
    # Preserve historical PDF serving in this UX-preserving release.
    $apiPom = Join-Path $targetRoot 'backend/copilot-api/pom.xml'
    Copy-Item -LiteralPath (Join-Path $repository 'backend/copilot-api/pom.xml') -Destination $apiPom
}
$identityPath = Join-Path $targetRoot 'release-identity.json'
$identity = Get-Content $identityPath -Raw | ConvertFrom-Json
$identity.application = $Application
$identity.maturity = 'independent-v1-existing-ux'
$identity | ConvertTo-Json | Set-Content $identityPath -Encoding utf8
Set-Content (Join-Path $targetRoot 'README.md') -Encoding utf8 -Value @"
# Independent $Application

This checkout owns all of its runtime build inputs.
No sibling application, original monorepo or shared compiled application library
is required. Public contracts are pinned in release-identity.json.

Build with ./mvnw.cmd -Dmaven.test.skip=true package. Configure environment variables explicitly
before running the packaged JAR. Investigation also owns frontend/operator-console
(npm ci, npm start); generator/evaluator have no Node runtime dependency.

Generator: port 8082, COPILOT_API_BASE_URL, SYNTHETIC_TENANT_ID,
COPILOT_API_REQUEST_TIMEOUT, EVALUATOR_BROWSER_BASE_URL (browser use only).
Evaluator: port 8083, the same read-only API/tenant settings, OLLAMA_BASE_URL,
COMPARISON_CHAT_MODEL, COMPARISON_TIMEOUT, COMPARISON_ARTIFACT_DIRECTORY and
GENERATOR_BROWSER_ORIGINS. It has no generation/MCP/database component.
Investigation: port 8080, SPRING_DATASOURCE_* and existing REPORT_/KNOWLEDGE_
settings; OPERATIONS_MCP_BASE_URL=http://localhost:8082. Console: port 4200.

Keep existing PDFs, synthetic IDs, reports, decisions and comparison rules.
This release proves application/build separation, not blind benchmark quality
or denial of shared administrator access. Histories are never rewritten.
The original one-click launcher orchestrates these independent artifacts.
"@
Set-Content (Join-Path $targetRoot '.gitignore') -Value "target/`nnode_modules/`ndist/`n.angular/`ncoverage/`nartifacts/`ntmp/`n.env`n.env.*`n*.log" -Encoding utf8
$files = @(Get-ChildItem -LiteralPath $targetRoot -Recurse -File -Force | Where-Object Name -ne 'export-manifest.json' | ForEach-Object {
    [ordered]@{path=[IO.Path]::GetRelativePath($targetRoot,$_.FullName).Replace('\','/');sha256=(Get-FileHash $_.FullName).Hash.ToLowerInvariant()}
})
[ordered]@{application=$Application;version='independent/v1';files=$files} | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $targetRoot 'export-manifest.json') -Encoding utf8
Assert-ExportManifest -Path $targetRoot
Write-Host "Independent $Application exported and verified: $targetRoot"
