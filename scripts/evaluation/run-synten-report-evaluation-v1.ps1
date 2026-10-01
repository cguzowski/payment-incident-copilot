[CmdletBinding(DefaultParameterSetName = 'Input')]
param(
    [Parameter(Mandatory, ParameterSetName = 'Input')]
    [string] $InputPath,

    [Parameter(Mandatory, ParameterSetName = 'Fixture')]
    [switch] $SyntheticFixture,

    [Parameter(Mandatory)]
    [string] $OutputPath,

    [string] $ObservableCatalogPath,
    [string] $OraclePath
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
if ([string]::IsNullOrWhiteSpace($ObservableCatalogPath)) {
    $ObservableCatalogPath = Join-Path $repositoryRoot 'syntheticIncidentGenerator\src\main\resources\scenarios\catalog.json'
}
if ([string]::IsNullOrWhiteSpace($OraclePath)) {
    $OraclePath = Join-Path $repositoryRoot 'syntheticIncidentGenerator\src\main\resources\scenarios\oracle.json'
}

Import-Module (Join-Path $PSScriptRoot 'SynTenReportEvaluationV1.psm1') -Force

$inputRun = if ($SyntheticFixture) {
    New-SynTenReportEvaluationFixtureRun `
        -ObservableCatalogPath $ObservableCatalogPath `
        -OraclePath $OraclePath
}
else {
    if (-not (Test-Path -LiteralPath $InputPath -PathType Leaf)) {
        throw "Report evaluation input is missing: $InputPath"
    }
    # PowerShell 7.5+ coerces ISO timestamps by default; the grader requires strings.
    $jsonOptions = @{}
    if ((Get-Command ConvertFrom-Json).Parameters.ContainsKey('DateKind')) {
        $jsonOptions.DateKind = 'String'
    }
    Get-Content -LiteralPath $InputPath -Raw | ConvertFrom-Json @jsonOptions
}

$evaluation = Invoke-SynTenReportEvaluation `
    -InputRun $inputRun `
    -ObservableCatalogPath $ObservableCatalogPath `
    -OraclePath $OraclePath
$written = Write-SynTenReportEvaluationArtifact -Path $OutputPath -Evaluation $evaluation

Write-Host "Report evaluation artifact: $($written.path)"
Write-Host "SHA-256: $($written.sha256)"
Write-Host "Scenarios: $($evaluation.aggregates.scenarioCount)"
Write-Host "Terminal statuses: AVAILABLE=$($evaluation.aggregates.terminalStatuses.AVAILABLE), UNAVAILABLE=$($evaluation.aggregates.terminalStatuses.UNAVAILABLE), TIMED_OUT=$($evaluation.aggregates.terminalStatuses.TIMED_OUT), MALFORMED=$($evaluation.aggregates.terminalStatuses.MALFORMED)"
Write-Host "Unsupported-claim indicators: $($evaluation.aggregates.unsupportedClaims.totalIndicators)"
