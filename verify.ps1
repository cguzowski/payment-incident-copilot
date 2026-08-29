[CmdletBinding()]
param(
    [ValidateSet('All', 'Backend', 'Frontend', 'Repository')]
    [string] $Scope = 'All'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repositoryRoot = $PSScriptRoot
$modulePath = Join-Path $repositoryRoot 'scripts/verification/Verification.psm1'
Import-Module $modulePath -Force

Write-Host "Running $Scope repository verification from $repositoryRoot"
Invoke-VerificationPlan -Scope $Scope -StepRunner {
    param($step)
    Invoke-RepositoryVerificationStep -Step $step -RepositoryRoot $repositoryRoot
}
Write-Host "`n$Scope repository verification passed."
