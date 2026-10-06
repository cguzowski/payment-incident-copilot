Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Assert-ReleasePath {
    param([Parameter(Mandatory)][string] $Path)
    if ([IO.Path]::IsPathRooted($Path) -or $Path -match '(^|[\\/])(\.|\.\.|tmp|\.git|\.env(?:\.[^/]+)?|AGENTS\.md|CLAUDE\.md)([\\/]|$)' -or $Path -match '\.(pem|key)$' -or $Path -match '[*?:]' -or $Path -match '\\') {
        throw "Unsafe release path: $Path"
    }
}

function Assert-NoReleaseLink {
    param([string] $Root, [string] $Relative)
    $current = $Root
    if ((Get-Item -LiteralPath $Root -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Unsafe release path: linked root' }
    foreach ($part in $Relative.Split('/')) {
        $current = Join-Path $current $part
        if (Test-Path -LiteralPath $current) {
            if ((Get-Item -LiteralPath $current -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) {
                throw "Unsafe release path: linked input $Relative"
            }
        }
    }
}

function Assert-ContractRelease {
    param([Parameter(Mandatory)][string] $Path, [string] $ExpectedVersion = 'v1', [string] $ExpectedManifestHash)
    $manifestPath = Join-Path $Path 'manifest.json'
    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    if ($manifest.release -ne 'investigation-source-contracts' -or $manifest.version -ne $ExpectedVersion) { throw 'Contract version mismatch' }
    if ($ExpectedManifestHash -and (Get-FileHash -LiteralPath $manifestPath).Hash.ToLowerInvariant() -ne $ExpectedManifestHash) { throw 'Contract hash mismatch: manifest pin' }
    $listed = @('manifest.json')
    foreach ($entry in $manifest.files) {
        Assert-ReleasePath $entry.path
        Assert-NoReleaseLink -Root $Path -Relative $entry.path
        $file = Join-Path $Path $entry.path
        if (!(Test-Path -LiteralPath $file -PathType Leaf) -or (Get-FileHash -LiteralPath $file).Hash.ToLowerInvariant() -ne $entry.sha256) { throw "Contract hash mismatch: $($entry.path)" }
        if ($entry.path -in $listed) { throw 'Duplicate contract resource' }
        $listed += $entry.path
    }
    foreach ($file in Get-ChildItem -LiteralPath $Path -File -Recurse -Force) {
        $relative = [IO.Path]::GetRelativePath($Path, $file.FullName).Replace('\', '/')
        if ($relative -notin $listed) { throw "Unlisted contract resource: $relative" }
    }
}

function Export-IndependentApplication {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string] $RepositoryRoot, [Parameter(Mandatory)][ValidateSet('investigation','source')][string] $Application,
        [Parameter(Mandatory)][string] $Destination, [string] $AllowlistPath)
    if (Test-Path -LiteralPath $Destination) { throw "Destination already exists: $Destination" }
    if (!$AllowlistPath) { $AllowlistPath = Join-Path $RepositoryRoot "scripts/migration/allowlists/$Application.json" }
    $allowlist = Get-Content -LiteralPath $AllowlistPath -Raw | ConvertFrom-Json
    $targets = @()
    foreach ($entry in $allowlist.files) {
        Assert-ReleasePath $entry.source
        Assert-ReleasePath $entry.target
        Assert-NoReleaseLink -Root $RepositoryRoot -Relative $entry.source
        $source = Join-Path $RepositoryRoot $entry.source
        if (!(Test-Path -LiteralPath $source -PathType Leaf)) { throw "Missing allowlisted input: $($entry.source)" }
        if ((Get-FileHash -LiteralPath $source).Hash.ToLowerInvariant() -ne $entry.sha256) { throw "Allowlisted input hash mismatch: $($entry.source)" }
        if ($entry.target -in $targets) { throw "Duplicate release target: $($entry.target)" }
        $targets += $entry.target
    }
    $release = Join-Path $RepositoryRoot 'contracts/releases/investigation-source/v1'
    $pin = Get-Content -LiteralPath (Join-Path $RepositoryRoot 'scripts/migration/contract-pin.json') -Raw | ConvertFrom-Json
    Assert-ContractRelease -Path $release -ExpectedVersion $pin.version -ExpectedManifestHash $pin.sha256
    New-Item -ItemType Directory -Path $Destination -Force | Out-Null
    foreach ($entry in $allowlist.files) {
        $target = Join-Path $Destination $entry.target
        New-Item -ItemType Directory -Path (Split-Path $target) -Force | Out-Null
        Copy-Item -LiteralPath (Join-Path $RepositoryRoot $entry.source) -Destination $target
        if ((Get-FileHash -LiteralPath $target).Hash.ToLowerInvariant() -ne $entry.sha256) { throw "Allowlisted input changed during export: $($entry.source)" }
    }
    Copy-Item -LiteralPath $release -Destination (Join-Path $Destination 'public-contracts') -Recurse
    if ($Application -eq 'investigation') {
        $rootPom = Join-Path $Destination 'pom.xml'
        $text = (Get-Content -LiteralPath $rootPom -Raw).Replace('        <module>backend/operations-mcp-server</module>', '')
        Set-Content -LiteralPath $rootPom -Value $text.TrimEnd() -Encoding utf8
        $apiPom = Join-Path $Destination 'backend/copilot-api/pom.xml'
        $text = (Get-Content -LiteralPath $apiPom -Raw).Replace('                    <include>versions/*/pdfs/*.pdf</include>', '')
        Set-Content -LiteralPath $apiPom -Value $text.TrimEnd() -Encoding utf8
    } else {
        $pom = Join-Path $Destination 'pom.xml'
        $text = (Get-Content -LiteralPath $pom -Raw).Replace('${project.basedir}/../SynTen Inc/multi-incidents/v1', '${project.basedir}/development/scenarios/multi-incidents/v1')
        Set-Content -LiteralPath $pom -Value $text.TrimEnd() -Encoding utf8
    }
    [ordered]@{ application=$Application; maturity='M1-development-regression'; contractManifestSha256=$pin.sha256 } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $Destination 'release-identity.json') -Encoding utf8
    Set-Content -LiteralPath (Join-Path $Destination 'README.md') -Value "# $Application development release`n`nRun ./verify.ps1 from this root. M1 proves build isolation only.`nKnown-case resources are developmental. Runtime evaluation/source labels and`nscenario-informed knowledge retire in later phases. No sealed measurement or`nruntime access certification is claimed. Archive-specific regression tests remain`nin the legacy gate. No legacy Git history or private retained artifacts are copied." -Encoding utf8
    Set-Content -LiteralPath (Join-Path $Destination '.gitignore') -Value "target/`nnode_modules/`ndist/`n.angular/`ncoverage/`ntmp/`n.env`n.env.*`n*.log" -Encoding utf8
    $files = @(Get-ChildItem -LiteralPath $Destination -File -Recurse -Force | ForEach-Object {
        [ordered]@{ path=[IO.Path]::GetRelativePath($Destination,$_.FullName).Replace('\','/'); sha256=(Get-FileHash -LiteralPath $_.FullName).Hash.ToLowerInvariant() }
    })
    [ordered]@{ application=$Application; files=$files } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $Destination 'export-manifest.json') -Encoding utf8
    Assert-ExportManifest -Path $Destination
}

function Assert-ExportManifest {
    param([Parameter(Mandatory)][string] $Path)
    $manifest = Get-Content -LiteralPath (Join-Path $Path 'export-manifest.json') -Raw | ConvertFrom-Json
    foreach ($entry in $manifest.files) {
        Assert-ReleasePath $entry.path
        Assert-NoReleaseLink -Root $Path -Relative $entry.path
        $file = Join-Path $Path $entry.path
        if (!(Test-Path -LiteralPath $file -PathType Leaf) -or (Get-FileHash -LiteralPath $file).Hash.ToLowerInvariant() -ne $entry.sha256) { throw "Export hash mismatch: $($entry.path)" }
    }
    $listed = @($manifest.files.path) + @('export-manifest.json')
    foreach ($file in Get-ChildItem -LiteralPath $Path -File -Recurse -Force) {
        $relative = [IO.Path]::GetRelativePath($Path,$file.FullName).Replace('\','/')
        if ($relative -notin $listed) { throw "Unlisted export resource: $relative" }
    }
}
Export-ModuleMember -Function Assert-ReleasePath,Assert-ContractRelease,Export-IndependentApplication,Assert-ExportManifest
