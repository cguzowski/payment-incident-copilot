[CmdletBinding()]
param([switch] $CheckOnly, [switch] $PrepareKnowledge, [switch] $NoBrowser)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Import-Module (Join-Path $PSScriptRoot 'Launch.psm1') -Force
$repository = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$releaseRoot = Join-Path $repository '.migration-workspaces/independent-v1'
$settings = @{}
# Read configuration as data; never evaluate dotenv content or print credentials.
$configuration = Join-Path $repository '.env'
if (!(Test-Path -LiteralPath $configuration)) { throw 'Copy .env.example to .env and configure your database first.' }
foreach ($line in Get-Content -LiteralPath $configuration) {
    if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') {
        $settings[$matches[1]] = $matches[2].Trim().Trim('"').Trim("'")
    }
}
# Preserve explicit shell overrides for the same existing application settings.
foreach ($entry in [Environment]::GetEnvironmentVariables().GetEnumerator()) {
    if ($entry.Key -match '^(SPRING_DATASOURCE_|REPORT_|KNOWLEDGE_|OLLAMA_|OPERATIONS_MCP_|SYNTEN_|COPILOT_API_|SYNTHETIC_|COMPARISON_|EVALUATOR_|GENERATOR_BROWSER_)') {
        if (!$settings.ContainsKey($entry.Key)) { $settings[$entry.Key] = $entry.Value }
    }
}
foreach ($key in @('SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD')) {
    if (!$settings.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($settings[$key])) { throw "Required setting $key is missing." }
}
if (!$settings.ContainsKey('OLLAMA_BASE_URL')) { $settings.OLLAMA_BASE_URL = 'http://localhost:11434' }
if (!$settings.ContainsKey('REPORT_CHAT_MODEL')) { $settings.REPORT_CHAT_MODEL = 'qwen3:8b-q4_K_M' }
if (!$settings.ContainsKey('KNOWLEDGE_EMBEDDING_MODEL')) { $settings.KNOWLEDGE_EMBEDDING_MODEL = 'nomic-embed-text' }
if (!$settings.ContainsKey('COMPARISON_ARTIFACT_DIRECTORY')) { $settings.COMPARISON_ARTIFACT_DIRECTORY = Join-Path $repository 'tmp/comparisons' }
elseif (![IO.Path]::IsPathRooted($settings.COMPARISON_ARTIFACT_DIRECTORY)) { $settings.COMPARISON_ARTIFACT_DIRECTORY = [IO.Path]::GetFullPath((Join-Path $repository $settings.COMPARISON_ARTIFACT_DIRECTORY)) }
$java = (Get-Command java.exe -ErrorAction Stop).Source
$pwsh = (Get-Command pwsh.exe -ErrorAction Stop).Source
$node = (Get-Command node.exe -ErrorAction Stop).Source
if ((& $java --version | Out-String) -notmatch '^(openjdk|java) 21[.\s]') { throw 'Java 21 required.' }
Import-Module (Join-Path $repository 'scripts/local/LocalAiPrerequisites.psm1') -Force
Assert-LocalOllamaModels -BaseUrl $settings.OLLAMA_BASE_URL -RequiredModels @($settings.REPORT_CHAT_MODEL,$settings.KNOWLEDGE_EMBEDDING_MODEL)
$jdbc = $settings.SPRING_DATASOURCE_URL -replace '^jdbc:', ''
$database = [Uri] $jdbc
$tcp = [Net.Sockets.TcpClient]::new()
try { $tcp.Connect($database.Host, $(if ($database.Port -gt 0) { $database.Port } else { 5432 })) } finally { $tcp.Dispose() }
if ($CheckOnly) { Write-Host 'Independent startup prerequisites passed.'; return }

$plan = @(Get-IndependentLaunchPlan $releaseRoot)
if ($settings.ContainsKey('EVALUATOR_PORT')) {
    ($plan | Where-Object Name -eq evaluator).Port = [int] $settings.EVALUATOR_PORT
    ($plan | Where-Object Name -eq evaluator).ProbeUrl = "http://127.0.0.1:$($settings.EVALUATOR_PORT)"
    if (!$settings.ContainsKey('EVALUATOR_BROWSER_BASE_URL')) {
        $settings.EVALUATOR_BROWSER_BASE_URL = "http://localhost:$($settings.EVALUATOR_PORT)"
    }
}
foreach ($system in $plan) {
    if (!(Test-Path -LiteralPath $system.Directory)) {
        & (Join-Path $PSScriptRoot 'export.ps1') -Application $system.Name -Destination $system.Directory
    }
    $jar = Join-Path $system.Directory $system.Jar
    if (!(Test-Path -LiteralPath $jar)) {
        Push-Location $system.Directory
        try {
            & ./mvnw.cmd '-DskipTests' package
            if ($LASTEXITCODE -ne 0) { throw "Build failed: $($system.Name)" }
        } finally { Pop-Location }
    }
}
$investigation = $plan | Where-Object Name -eq investigation
$investigationSettings = Get-SystemEnvironment investigation $settings
Import-Module (Join-Path $repository 'scripts/local/LocalKnowledgePreparation.psm1') -Force
$steps = @(Get-LocalKnowledgePreparationPlan -RepositoryRoot $investigation.Directory)
if (!$PrepareKnowledge) { $steps = @($steps | Where-Object Name -in @('readiness','payment-readiness')) }
foreach ($step in $steps) {
    $stepSettings = $investigationSettings.Clone()
    foreach ($entry in $step.EnvironmentVariables.GetEnumerator()) { $stepSettings[$entry.Key] = $entry.Value }
    Start-IsolatedProcess -Executable $pwsh -Arguments @('-NoProfile','-File',(Join-Path $PSScriptRoot 'prepare-step.ps1'),'-Directory',(Join-Path $investigation.Directory 'backend/copilot-api')) -Directory $investigation.Directory -Settings $stepSettings -Wait
}
foreach ($system in $plan) {
    $jar = Join-Path $system.Directory $system.Jar
    $hash = (Get-FileHash $jar).Hash.ToLowerInvariant()
    $baseUrl = $system.ProbeUrl
    $info = $null
    try { $info = Invoke-RestMethod "$baseUrl/actuator/info" -TimeoutSec 2 } catch {}
    if ($null -ne $info) { Assert-SystemIdentity $system.Name $info $hash; continue }
    Write-Host "Starting independent $($system.Name)..."
    $envForSystem = Get-SystemEnvironment $system.Name $settings
    $null = Start-IsolatedProcess -Executable $pwsh -Arguments @('-NoProfile','-File',(Join-Path $PSScriptRoot 'run-service.ps1'),'-Java',$java,'-Jar',$jar,'-Name',$system.Name,'-Hash',$hash) -Directory $system.Directory -Settings $envForSystem -Visible
    $deadline = [DateTime]::UtcNow.AddSeconds(90)
    do {
        try {
            $health = Invoke-RestMethod "$baseUrl/actuator/health" -TimeoutSec 2
            if ($health.status -eq 'UP') { break }
        } catch {}
        Start-Sleep -Milliseconds 500
    } while ([DateTime]::UtcNow -lt $deadline)
    $info = Invoke-RestMethod "$baseUrl/actuator/info" -TimeoutSec 2
    Assert-SystemIdentity $system.Name $info $hash
}
Import-Module (Join-Path $repository 'scripts/local/LocalEvidenceStartup.psm1') -Force
Assert-LocalApiMcpConfiguration -ExpectedBaseUrl 'http://localhost:8082'
$frontend = Join-Path $investigation.Directory 'frontend/operator-console'
if (!(Test-Path (Join-Path $frontend 'node_modules/.package-lock.json'))) {
    Push-Location $frontend
    try { & npm.cmd ci; if ($LASTEXITCODE -ne 0) { throw 'npm ci failed.' } } finally { Pop-Location }
}
$consoleReady = $false
try { $consoleReady = (Invoke-WebRequest 'http://localhost:4200' -TimeoutSec 2).StatusCode -eq 200 } catch {}
if (!$consoleReady) {
    $null = Start-IsolatedProcess -Executable $pwsh -Arguments @('-NoProfile','-File',(Join-Path $PSScriptRoot 'run-console.ps1')) -Directory $frontend -Settings @{} -Visible
    $deadline = [DateTime]::UtcNow.AddSeconds(90)
    do {
        try { $consoleReady = (Invoke-WebRequest 'http://localhost:4200' -TimeoutSec 2).StatusCode -eq 200 } catch {}
        if ($consoleReady) { break }
        Start-Sleep -Milliseconds 500
    } while ([DateTime]::UtcNow -lt $deadline)
    if (!$consoleReady) { throw 'Operator console did not become ready. Review its visible terminal.' }
}
Write-Host 'Generator: http://localhost:8082 | Console: http://localhost:4200 | Evaluator: http://localhost:8083'
if (!$NoBrowser) {
    Start-Process 'http://localhost:4200' -WindowStyle Hidden
    Start-Process 'http://localhost:8082' -WindowStyle Hidden
}
