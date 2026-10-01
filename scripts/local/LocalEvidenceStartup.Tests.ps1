$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Import-Module (Join-Path $PSScriptRoot 'LocalEvidenceStartup.psm1') -Force

function Assert-Throws {
    param([scriptblock] $Action, [string] $Pattern)
    try { & $Action } catch {
        if ($_.Exception.Message -notlike $Pattern) { throw $_ }
        return
    }
    throw "Expected failure: $Pattern"
}

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
foreach ($generator in @($false, $true)) {
    $script:started = $null
    $script:waited = $false
    $expectedDirectory = if ($generator) { 'syntheticIncidentGenerator' } else { 'backend/operations-mcp-server' }
    Start-SelectedMcpProvider -RepositoryRoot $root -UseGeneratorMcp:$generator -HealthUri 'http://localhost/actuator/health' `
        -IsHealthy { param($Uri) $false } `
        -StartProvider { param($Directory) $script:started = $Directory } `
        -WaitForProvider { param($Uri) $script:waited = $true }
    if ($script:started -ne (Join-Path $root $expectedDirectory) -or -not $script:waited) {
        throw 'Wrong provider started or readiness was not awaited.'
    }
    Start-SelectedMcpProvider -RepositoryRoot $root -UseGeneratorMcp:$generator -HealthUri 'http://localhost/actuator/health' `
        -IsHealthy { param($Uri) $true } `
        -StartProvider { throw 'Healthy provider must be reused.' } `
        -WaitForProvider { throw 'Healthy provider must not be awaited.' }
}
Assert-Throws {
    Start-SelectedMcpProvider -RepositoryRoot $root -HealthUri 'http://localhost/actuator/health' `
        -IsHealthy { $false } -StartProvider { } -WaitForProvider { throw 'provider not ready' }
} '*provider not ready*'

Assert-LocalApiMcpConfiguration -ExpectedBaseUrl 'http://localhost:8082/' -InfoRequest {
    param($Uri)
    if ($Uri -ne 'http://localhost:8080/actuator/info') { throw 'Wrong info URI.' }
    @{ operationsMcp = @{ baseUrl = 'http://localhost:8082' } }
}
Assert-Throws {
    Assert-LocalApiMcpConfiguration -ExpectedBaseUrl 'http://localhost:8082' -InfoRequest {
        @{ operationsMcp = @{ baseUrl = 'http://localhost:8081' } }
    }
} '*different evidence provider*restart*'
foreach ($missing in @(@{}, @{ operationsMcp = @{} })) {
    Assert-Throws {
        Assert-LocalApiMcpConfiguration -ExpectedBaseUrl 'http://localhost:8082' -InfoRequest { $missing }
    } '*cannot be verified*restart*'
}
Assert-Throws {
    Assert-LocalApiMcpConfiguration -ExpectedBaseUrl 'http://localhost:8082' -InfoRequest { throw 'HTTP unavailable' }
} '*cannot be verified*restart*'
Write-Host 'Local evidence startup tests passed.'
