$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$modulePath = Join-Path $PSScriptRoot 'SynTenRetrievalEvaluationV1.psm1'
if (-not (Test-Path -LiteralPath $modulePath -PathType Leaf)) {
    throw "Evaluation module is missing: $modulePath"
}
Import-Module $modulePath -Force

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$failures = [System.Collections.Generic.List[string]]::new()

function Invoke-EvaluationTest {
    param(
        [Parameter(Mandatory)] [string] $Name,
        [Parameter(Mandatory)] [scriptblock] $Test
    )

    try {
        & $Test
        Write-Host "PASS $Name"
    }
    catch {
        $failures.Add("$Name`: $($_.Exception.Message)")
        Write-Host "FAIL $Name"
    }
}

function Assert-True {
    param(
        [Parameter(Mandatory)] [bool] $Condition,
        [Parameter(Mandatory)] [string] $Message
    )
    if (-not $Condition) {
        throw $Message
    }
}

function Assert-Throws {
    param(
        [Parameter(Mandatory)] [scriptblock] $Action,
        [Parameter(Mandatory)] [string] $MessagePattern
    )
    try {
        & $Action
    }
    catch {
        if ($_.Exception.Message -notlike $MessagePattern) {
            throw "Expected '$MessagePattern' but got '$($_.Exception.Message)'."
        }
        return
    }
    throw "Expected '$MessagePattern' but no error was raised."
}

Invoke-EvaluationTest 'buildsTheExact37VariantSeedPlanFromReviewedSources' {
    $plan = New-SynTenEvaluationSeedPlan `
        -CasesPath (Join-Path $repositoryRoot 'SynTen Inc\evaluation\retrieval-cases.md') `
        -ScenarioCatalogPath (Join-Path $repositoryRoot 'syntheticIncidentGenerator\src\main\resources\scenarios\catalog.json') `
        -ValidationManifestPath (Join-Path $repositoryRoot 'SynTen Inc\corpus\validation-manifest.json') `
        -DetectedAt ([DateTimeOffset]::Parse('2026-09-01T12:00:00Z')) `
        -RunToken '1234567890ab'

    Assert-True ($plan.EvaluationVersion -eq 'synten-retrieval-eval/v1') 'Evaluation version differs.'
    Assert-True ($plan.CorpusVersion -eq 'synten-auth-knowledge/v1') 'Corpus version differs.'
    Assert-True ($plan.Variants.Count -eq 37) 'Seed plan must contain 36 scenarios plus KQ-023.'
    Assert-True (($plan.Variants | Where-Object ScenarioCode -match '^S\d{3}$').Count -eq 37) 'Every variant needs an opaque scenario code.'
    Assert-True (($plan.Variants | Select-Object -ExpandProperty ScenarioReference -Unique).Count -eq 37) 'Scenario references must be unique.'
    Assert-True ($plan.Variants[0].CaseId -eq 'KQ-001' -and $plan.Variants[0].VariantId -eq 'S001') 'First variant differs.'
    Assert-True ($plan.Variants[-1].CaseId -eq 'KQ-023' -and $plan.Variants[-1].ScenarioCode -eq 'S999') 'Exclusion probe differs.'
    Assert-True ($plan.Variants[-1].Description -match 'legacy gateway recovery') 'Exclusion probe lacks reviewed terminology.'
    Assert-True ($plan.Variants[-1].ExpectedEvidenceStatus -eq 'NOT_FOUND') 'Exclusion probe must expect honest no-match evidence.'
}

Invoke-EvaluationTest 'buildsOnlyValidVersionedOpaqueScenarioReferences' {
    $reference = New-SynTenAlertReference `
        -ScenarioCode 'S203' `
        -DetectedAt ([DateTimeOffset]::FromUnixTimeSeconds(1788167730)) `
        -Token '1234567890ab'
    Assert-True ($reference -eq 'sig-v1-S203-1788167730-1234567890ab') 'Opaque reference differs.'
    Assert-Throws {
        New-SynTenAlertReference -ScenarioCode 'S20X' -DetectedAt ([DateTimeOffset]::UtcNow) -Token '1234567890ab'
    } '*invalid scenario code*'
}

Invoke-EvaluationTest 'acceptsAnExactIncidentInvestigationAndEvidenceReadback' {
    $variant = [pscustomobject]@{
        CaseId = 'KQ-001'
        VariantId = 'S001'
        ScenarioReference = 'sig-v1-S001-1788167730-1234567890ab'
        Title = 'Authorization decline rate above threshold'
        Description = 'Synthetic card authorization declines reached 31%.'
        Severity = 'CRITICAL'
        ExpectedEvidenceStatus = 'AVAILABLE'
        ExpectedServiceName = 'payment-authorization'
        ExpectedErrors = @([pscustomobject]@{ errorCode = 'GATEWAY_TIMEOUT'; count = 47 })
    }
    $incident = [pscustomobject]@{
        incidentId = '11111111-1111-4111-8111-111111111111'
        tenantId = '8b860d80-d17f-4e6b-8c48-af35f26a4d61'
        externalAlertId = $variant.ScenarioReference
        title = $variant.Title
        description = $variant.Description
        severity = $variant.Severity
    }
    $investigation = [pscustomobject]@{
        investigationId = '22222222-2222-4222-8222-222222222222'
        incidentId = $incident.incidentId
    }
    $evidence = [pscustomobject]@{
        evidenceId = '33333333-3333-4333-8333-333333333333'
        status = 'AVAILABLE'
        content = [pscustomobject]@{
            serviceName = 'payment-authorization'
            errors = @([pscustomobject]@{ errorCode = 'GATEWAY_TIMEOUT'; count = 47 })
        }
    }

    $result = Assert-SynTenSeedReadback `
        -Variant $variant `
        -TenantId '8b860d80-d17f-4e6b-8c48-af35f26a4d61' `
        -Incident $incident `
        -Investigation $investigation `
        -Evidence $evidence

    Assert-True ($result.InvestigationId -eq $investigation.investigationId) 'Validated mapping differs.'
}

Invoke-EvaluationTest 'rejectsAnyPersistedReadbackMismatch' {
    $variant = [pscustomobject]@{
        ScenarioReference = 'sig-v1-S001-1788167730-1234567890ab'
        Title = 'Expected title'
        Description = 'Expected description'
        Severity = 'HIGH'
        ExpectedEvidenceStatus = 'AVAILABLE'
        ExpectedServiceName = 'payment-authorization'
        ExpectedErrors = @([pscustomobject]@{ errorCode = 'GATEWAY_TIMEOUT'; count = 47 })
    }
    $incident = [pscustomobject]@{
        incidentId = '11111111-1111-4111-8111-111111111111'
        tenantId = '8b860d80-d17f-4e6b-8c48-af35f26a4d61'
        externalAlertId = $variant.ScenarioReference
        title = 'Changed title'
        description = $variant.Description
        severity = $variant.Severity
    }
    $investigation = [pscustomobject]@{
        investigationId = '22222222-2222-4222-8222-222222222222'
        incidentId = $incident.incidentId
    }
    $evidence = [pscustomobject]@{
        evidenceId = '33333333-3333-4333-8333-333333333333'
        status = 'AVAILABLE'
        content = [pscustomobject]@{
            serviceName = 'payment-authorization'
            errors = @([pscustomobject]@{ errorCode = 'GATEWAY_TIMEOUT'; count = 47 })
        }
    }
    Assert-Throws {
        Assert-SynTenSeedReadback `
            -Variant $variant `
            -TenantId '8b860d80-d17f-4e6b-8c48-af35f26a4d61' `
            -Incident $incident `
            -Investigation $investigation `
            -Evidence $evidence
    } '*title mismatch*'
}

Invoke-EvaluationTest 'selectsEvidenceFromInvokeRestMethodArrayResponse' {
    $expected = [pscustomobject]@{
        evidenceId = '33333333-3333-4333-8333-333333333333'
        status = 'AVAILABLE'
    }
    $other = [pscustomobject]@{
        evidenceId = '44444444-4444-4444-8444-444444444444'
        status = 'UNAVAILABLE'
    }
    $invokeRestMethodResponse = [object[]] @($other, $expected)

    $selected = Select-SynTenEvidenceReadback `
        -HistoryResponse $invokeRestMethodResponse `
        -EvidenceId $expected.evidenceId

    Assert-True ($selected.evidenceId -eq $expected.evidenceId) 'Selected evidence differs.'
    Assert-Throws {
        Select-SynTenEvidenceReadback `
            -HistoryResponse $invokeRestMethodResponse `
            -EvidenceId '55555555-5555-4555-8555-555555555555'
    } '*missing or duplicated*'
}

Invoke-EvaluationTest 'writesOneAtomicManifestAndRefusesOverwrite' {
    $temporaryRoot = Join-Path ([IO.Path]::GetTempPath()) ("synten-eval-test-" + [Guid]::NewGuid().ToString('N'))
    $null = New-Item -ItemType Directory -Path $temporaryRoot
    try {
        $target = Join-Path $temporaryRoot 'seed.json'
        Write-SynTenEvaluationSeedManifest -Path $target -Manifest ([ordered]@{
            schemaVersion = 'synten-retrieval-eval-seed/v1'
            mappings = @()
        })
        Assert-True (Test-Path -LiteralPath $target -PathType Leaf) 'Seed manifest was not written.'
        Assert-True (-not (Test-Path -LiteralPath "$target.tmp")) 'Partial seed manifest remains.'
        Assert-Throws {
            Write-SynTenEvaluationSeedManifest -Path $target -Manifest @{ schemaVersion = 'changed' }
        } '*already exists*'
    }
    finally {
        Remove-Item -LiteralPath $temporaryRoot -Recurse -Force
    }
}

Invoke-EvaluationTest 'runnerUsesOnlyProductHttpBoundariesForSeeding' {
    $runnerPath = Join-Path $PSScriptRoot 'run-synten-retrieval-evaluation-v1.ps1'
    $runner = Get-Content -LiteralPath $runnerPath -Raw
    foreach ($route in @('/api/alerts', '/investigations', '/evidence-collections', '/actuator/health')) {
        Assert-True ($runner.Contains($route)) "Runner is missing route $route."
    }
    foreach ($forbidden in @('Invoke-Sqlcmd', 'INSERT INTO', 'DELETE FROM', 'DROP DATABASE', 'psql ')) {
        Assert-True (-not $runner.Contains($forbidden)) "Runner contains forbidden direct database operation '$forbidden'."
    }
}

if ($failures.Count -gt 0) {
    throw ($failures -join [Environment]::NewLine)
}

Write-Host 'All SynTen retrieval-evaluation runner tests passed.'
