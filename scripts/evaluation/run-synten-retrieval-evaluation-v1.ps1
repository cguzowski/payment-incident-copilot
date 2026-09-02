[CmdletBinding()]
param(
    [Uri] $CopilotApiBaseUrl = 'http://127.0.0.1:8080',
    [Uri] $GeneratorMcpBaseUrl = 'http://127.0.0.1:8082',
    [string] $TenantId = '8b860d80-d17f-4e6b-8c48-af35f26a4d61',
    [string] $OperatorId = '7b636625-53d1-46f7-92a9-9c8c27a243d1',
    [string] $EvaluationDatabaseName = $env:SYNTEN_EVALUATION_DATABASE_NAME,
    [string] $OutputPath,
    [switch] $PlanOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$modulePath = Join-Path $PSScriptRoot 'SynTenRetrievalEvaluationV1.psm1'
Import-Module $modulePath -Force

function Assert-HttpUri {
    param([Parameter(Mandatory)] [Uri] $Uri, [Parameter(Mandatory)] [string] $Name)
    if (-not $Uri.IsAbsoluteUri -or $Uri.Scheme -notin @('http', 'https')) {
        throw "$Name must be an absolute HTTP or HTTPS URI."
    }
}

function Join-ApiUri {
    param([Parameter(Mandatory)] [Uri] $BaseUrl, [Parameter(Mandatory)] [string] $Path)
    return $BaseUrl.AbsoluteUri.TrimEnd('/') + $Path
}

function Assert-Healthy {
    param([Parameter(Mandatory)] [Uri] $BaseUrl, [Parameter(Mandatory)] [string] $Name)
    $health = Invoke-RestMethod `
        -Method Get `
        -Uri (Join-ApiUri -BaseUrl $BaseUrl -Path '/actuator/health') `
        -TimeoutSec 5
    if ($health.status -ne 'UP') {
        throw "$Name health is not UP."
    }
}

function Assert-DedicatedEvaluationDatabase {
    if ([string]::IsNullOrWhiteSpace($EvaluationDatabaseName) -or
        $EvaluationDatabaseName -notmatch '^payment_copilot_k4_eval(?:_[a-z0-9_]+)?$') {
        throw 'SYNTEN_EVALUATION_DATABASE_NAME must name a dedicated payment_copilot_k4_eval database.'
    }
    $jdbcUrl = [Environment]::GetEnvironmentVariable('SPRING_DATASOURCE_URL', 'Process')
    if ([string]::IsNullOrWhiteSpace($jdbcUrl) -or
        $jdbcUrl -notmatch '^jdbc:postgresql://[^/]+/(?<Database>[A-Za-z0-9_]+)(?:\?.*)?$' -or
        $matches.Database -cne $EvaluationDatabaseName) {
        throw 'SPRING_DATASOURCE_URL must point to the named dedicated K4 evaluation database used by the API.'
    }
}

function Get-SafeOutputPath {
    param([Parameter(Mandatory)] [string] $RunId)
    $candidate = if ([string]::IsNullOrWhiteSpace($OutputPath)) {
        Join-Path ([IO.Path]::GetTempPath()) "synten-retrieval-eval-v1\$RunId-seed.json"
    }
    else {
        $OutputPath
    }
    $absolute = [IO.Path]::GetFullPath($candidate)
    $repositoryPrefix = $repositoryRoot.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
    if ($absolute.StartsWith($repositoryPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'The temporary evaluation seed manifest must be written outside the repository.'
    }
    return $absolute
}

try {
    Assert-HttpUri -Uri $CopilotApiBaseUrl -Name 'Copilot API base URL'
    Assert-HttpUri -Uri $GeneratorMcpBaseUrl -Name 'Generator MCP base URL'
    if ($TenantId -ne '8b860d80-d17f-4e6b-8c48-af35f26a4d61') {
        throw 'K4 evaluation is restricted to the configured synthetic SynTen tenant.'
    }
    $runId = [Guid]::NewGuid().ToString('N')
    $runToken = $runId.Substring(0, 12)
    $detectedAt = [DateTimeOffset]::UtcNow.AddMinutes(-5)
    $plan = New-SynTenEvaluationSeedPlan `
        -CasesPath (Join-Path $repositoryRoot 'SynTen Inc\evaluation\retrieval-cases.md') `
        -ScenarioCatalogPath (Join-Path $repositoryRoot 'syntheticIncidentGenerator\src\main\resources\scenarios\catalog.json') `
        -ValidationManifestPath (Join-Path $repositoryRoot 'SynTen Inc\corpus\validation-manifest.json') `
        -DetectedAt $detectedAt `
        -RunToken $runToken

    if ($PlanOnly) {
        $plan | ConvertTo-Json -Depth 8
        exit 0
    }

    Assert-DedicatedEvaluationDatabase
    Assert-Healthy -BaseUrl $CopilotApiBaseUrl -Name 'Copilot API'
    Assert-Healthy -BaseUrl $GeneratorMcpBaseUrl -Name 'Standalone generator MCP endpoint'
    $headers = @{
        'X-Synthetic-Tenant-Id' = $TenantId
        'X-Synthetic-Operator-Id' = $OperatorId
    }
    $mappings = [System.Collections.Generic.List[object]]::new()
    foreach ($variant in $plan.Variants) {
        Write-Host "Seeding $($variant.CaseId)/$($variant.VariantId)..."
        $alertBody = [ordered]@{
            externalAlertId = $variant.ScenarioReference
            severity = $variant.Severity
            detectedAt = $variant.DetectedAt
            title = $variant.Title
            description = $variant.Description
        } | ConvertTo-Json
        $createdIncident = Invoke-RestMethod `
            -Method Post `
            -Uri (Join-ApiUri -BaseUrl $CopilotApiBaseUrl -Path '/api/alerts') `
            -Headers $headers `
            -ContentType 'application/json' `
            -Body $alertBody `
            -TimeoutSec 15
        if ([string] $createdIncident.tenantId -ne $TenantId) {
            throw "Created incident tenant mismatch for $($variant.CaseId)/$($variant.VariantId)."
        }
        $investigation = Invoke-RestMethod `
            -Method Post `
            -Uri (Join-ApiUri -BaseUrl $CopilotApiBaseUrl -Path "/api/incidents/$($createdIncident.incidentId)/investigations") `
            -Headers $headers `
            -TimeoutSec 15
        $collectedEvidence = Invoke-RestMethod `
            -Method Post `
            -Uri (Join-ApiUri -BaseUrl $CopilotApiBaseUrl -Path "/api/investigations/$($investigation.investigationId)/evidence-collections") `
            -Headers $headers `
            -TimeoutSec 30

        $incidentReadback = Invoke-RestMethod `
            -Method Get `
            -Uri (Join-ApiUri -BaseUrl $CopilotApiBaseUrl -Path "/api/incidents/$($createdIncident.incidentId)") `
            -Headers $headers `
            -TimeoutSec 15
        $incidentReadback | Add-Member -NotePropertyName tenantId -NotePropertyValue $createdIncident.tenantId
        $investigationReadback = Invoke-RestMethod `
            -Method Get `
            -Uri (Join-ApiUri -BaseUrl $CopilotApiBaseUrl -Path "/api/investigations/$($investigation.investigationId)") `
            -Headers $headers `
            -TimeoutSec 15
        $evidenceHistory = Invoke-RestMethod `
            -Method Get `
            -Uri (Join-ApiUri -BaseUrl $CopilotApiBaseUrl -Path "/api/investigations/$($investigation.investigationId)/evidence-collections") `
            -Headers $headers `
            -TimeoutSec 15
        $evidenceReadback = Select-SynTenEvidenceReadback `
            -HistoryResponse $evidenceHistory `
            -EvidenceId $collectedEvidence.evidenceId
        $mappings.Add((Assert-SynTenSeedReadback `
            -Variant $variant `
            -TenantId $TenantId `
            -Incident $incidentReadback `
            -Investigation $investigationReadback `
            -Evidence $evidenceReadback))
    }

    $manifest = [ordered]@{
        schemaVersion = 'synten-retrieval-eval-seed/v1'
        evaluationVersion = $plan.EvaluationVersion
        corpusVersion = $plan.CorpusVersion
        runId = $runId
        createdAt = [DateTimeOffset]::UtcNow.ToString('O')
        evaluatedAt = $detectedAt.ToUniversalTime().ToString('O')
        tenantId = $TenantId
        evaluationDatabaseName = $EvaluationDatabaseName
        mappings = $mappings.ToArray()
    }
    $manifestPath = Write-SynTenEvaluationSeedManifest -Path (Get-SafeOutputPath -RunId $runId) -Manifest $manifest
    Write-Host "Seeded and verified $($mappings.Count) evaluation variants."
    Write-Host "Temporary seed manifest: $manifestPath"
}
catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
