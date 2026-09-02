Set-StrictMode -Version Latest

function Get-VerificationStepNames {
    [CmdletBinding()]
    param(
        [ValidateSet('All', 'Backend', 'Frontend', 'Repository')]
        [string] $Scope = 'All'
    )

    switch ($Scope) {
        'Backend' {
            return @('check-java', 'check-repository-tools', 'maven-verify', 'backend-no-skips')
        }
        'Frontend' {
            return @(
                'check-node',
                'check-npm',
                'frontend-install',
                'frontend-test',
                'frontend-no-skips',
                'frontend-format',
                'frontend-build'
            )
        }
        'Repository' {
            return @('check-repository-tools', 'verification-system-tests', 'compose-config', 'diff-check')
        }
        default {
            return @(
                'check-java',
                'check-node',
                'check-npm',
                'check-repository-tools',
                'verification-system-tests',
                'maven-verify',
                'backend-no-skips',
                'frontend-install',
                'frontend-test',
                'frontend-no-skips',
                'frontend-format',
                'frontend-build',
                'compose-config',
                'diff-check'
            )
        }
    }
}

function Assert-ExactVersion {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string] $Name,
        [Parameter(Mandatory)] [string] $Actual,
        [Parameter(Mandatory)] [string] $Expected
    )

    if ($Actual.Trim() -ne $Expected.Trim()) {
        throw "$Name must be version $Expected but version $Actual is active."
    }
}

function Assert-JUnitXmlHasNoSkippedTests {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string] $XmlContent,
        [Parameter(Mandatory)] [string] $Source
    )

    try {
        [xml] $document = $XmlContent
    }
    catch {
        throw "JUnit report '$Source' is malformed: $($_.Exception.Message)"
    }

    $skippedNodes = @($document.SelectNodes('//skipped'))
    $declaredSkipped = 0
    foreach ($suite in @($document.SelectNodes('//testsuite[@skipped]'))) {
        $value = 0
        if (-not [int]::TryParse($suite.GetAttribute('skipped'), [ref] $value)) {
            throw "JUnit report '$Source' has an invalid skipped count."
        }
        $declaredSkipped += $value
    }

    if ($declaredSkipped -gt 0 -or $skippedNodes.Count -gt 0) {
        throw "JUnit report '$Source' contains skipped tests."
    }
}

function Assert-JUnitReportsHaveNoSkippedTests {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string[]] $ReportPaths,
        [Parameter(Mandatory)] [string] $Description
    )

    if ($ReportPaths.Count -eq 0) {
        throw "No $Description JUnit reports were produced."
    }
    foreach ($reportPath in $ReportPaths) {
        if (-not (Test-Path -LiteralPath $reportPath -PathType Leaf)) {
            throw "Expected $Description JUnit report is missing: $reportPath"
        }
        Assert-JUnitXmlHasNoSkippedTests `
            -XmlContent (Get-Content -Raw -LiteralPath $reportPath) `
            -Source $reportPath
    }
}

function Invoke-VerificationPlan {
    [CmdletBinding()]
    param(
        [ValidateSet('All', 'Backend', 'Frontend', 'Repository')]
        [string] $Scope = 'All',
        [Parameter(Mandatory)] [scriptblock] $StepRunner
    )

    foreach ($name in Get-VerificationStepNames -Scope $Scope) {
        $step = [pscustomobject]@{ Name = $name }
        Write-Host "`n==> $name"
        try {
            & $StepRunner $step
        }
        catch {
            throw "Verification step '$name' failed: $($_.Exception.Message)"
        }
    }
}

function Invoke-ExternalCommand {
    param(
        [Parameter(Mandatory)] [string] $Executable,
        [string[]] $Arguments = @(),
        [Parameter(Mandatory)] [string] $WorkingDirectory
    )

    Push-Location -LiteralPath $WorkingDirectory
    try {
        & $Executable @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Command '$Executable' exited with code $LASTEXITCODE."
        }
    }
    finally {
        Pop-Location
    }
}

function Get-PinnedNodeVersion {
    param([Parameter(Mandatory)] [string] $RepositoryRoot)

    $versionFile = Join-Path $RepositoryRoot '.node-version'
    if (-not (Test-Path -LiteralPath $versionFile -PathType Leaf)) {
        throw "Node version pin is missing: $versionFile"
    }
    return (Get-Content -Raw -LiteralPath $versionFile).Trim()
}

function Get-PinnedNpmVersion {
    param([Parameter(Mandatory)] [string] $RepositoryRoot)

    $packageFile = Join-Path $RepositoryRoot 'frontend/operator-console/package.json'
    $package = Get-Content -Raw -LiteralPath $packageFile | ConvertFrom-Json
    if ($package.packageManager -notmatch '^npm@(?<version>\d+\.\d+\.\d+)$') {
        throw "package.json must pin npm with an exact packageManager value."
    }
    return $Matches.version
}

function Get-MavenWrapperInvocation {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] [string] $RepositoryRoot,
        [Parameter(Mandatory)] [bool] $WindowsPlatform
    )

    if ($WindowsPlatform) {
        $windowsRepositoryRoot = $RepositoryRoot.TrimEnd('/', '\')
        return [pscustomobject]@{
            Executable = '{0}\mvnw.cmd' -f $windowsRepositoryRoot
            Arguments = @('--batch-mode', 'clean', 'verify')
        }
    }

    return [pscustomobject]@{
        Executable = 'sh'
        Arguments = @(
            "$($RepositoryRoot.TrimEnd('/', '\'))/mvnw",
            '--batch-mode',
            'clean',
            'verify'
        )
    }
}

function Test-WindowsPlatform {
    return $env:OS -eq 'Windows_NT'
}

function Invoke-RepositoryVerificationStep {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)] $Step,
        [Parameter(Mandatory)] [string] $RepositoryRoot
    )

    $frontendRoot = Join-Path $RepositoryRoot 'frontend/operator-console'
    switch ($Step.Name) {
        'check-java' {
            $javaOutput = (& java -version 2>&1 | Out-String)
            if ($LASTEXITCODE -ne 0 -or $javaOutput -notmatch 'version "21(?:\.|\")') {
                throw "Java 21 is required. Active output: $($javaOutput.Trim())"
            }
        }
        'check-node' {
            $actual = (& node --version).Trim().TrimStart('v')
            if ($LASTEXITCODE -ne 0) {
                throw "Node.js version could not be read."
            }
            Assert-ExactVersion -Name 'Node.js' -Actual $actual -Expected (Get-PinnedNodeVersion $RepositoryRoot)
        }
        'check-npm' {
            $actual = (& npm --version).Trim()
            if ($LASTEXITCODE -ne 0) {
                throw "npm version could not be read."
            }
            Assert-ExactVersion -Name 'npm' -Actual $actual -Expected (Get-PinnedNpmVersion $RepositoryRoot)
        }
        'check-repository-tools' {
            $requiredCommands = @('git', 'docker')
            if (-not (Test-WindowsPlatform)) {
                $requiredCommands += 'sh'
            }
            foreach ($commandName in $requiredCommands) {
                if ($null -eq (Get-Command $commandName -ErrorAction SilentlyContinue)) {
                    throw "Required command '$commandName' is unavailable."
                }
            }
            $wrapper = if (Test-WindowsPlatform) { 'mvnw.cmd' } else { 'mvnw' }
            if (-not (Test-Path -LiteralPath (Join-Path $RepositoryRoot $wrapper) -PathType Leaf)) {
                throw "Pinned Maven Wrapper entry point '$wrapper' is missing."
            }
        }
        'verification-system-tests' {
            & (Join-Path $RepositoryRoot 'scripts/verification/Verification.Tests.ps1')
            & (Join-Path $RepositoryRoot 'scripts/local/LocalKnowledgePreparation.Tests.ps1')
            & (Join-Path $RepositoryRoot 'scripts/evaluation/SynTenRetrievalEvaluationV1.Tests.ps1')
        }
        'maven-verify' {
            $invocation = Get-MavenWrapperInvocation `
                -RepositoryRoot $RepositoryRoot `
                -WindowsPlatform (Test-WindowsPlatform)
            Invoke-ExternalCommand `
                -Executable $invocation.Executable `
                -Arguments $invocation.Arguments `
                -WorkingDirectory $RepositoryRoot
        }
        'backend-no-skips' {
            $reports = @(Get-ChildItem `
                -Path (Join-Path $RepositoryRoot 'backend/*/target/surefire-reports/TEST-*.xml') `
                -File `
                -ErrorAction SilentlyContinue | ForEach-Object FullName)
            Assert-JUnitReportsHaveNoSkippedTests -ReportPaths $reports -Description 'backend'
        }
        'frontend-install' {
            Invoke-ExternalCommand -Executable 'npm' -Arguments @('ci') -WorkingDirectory $frontendRoot
        }
        'frontend-test' {
            $report = Join-Path $frontendRoot 'coverage/test-results/junit.xml'
            if (Test-Path -LiteralPath $report -PathType Leaf) {
                Remove-Item -LiteralPath $report -Force
            }
            Invoke-ExternalCommand -Executable 'npm' -Arguments @('test', '--', '--watch=false') -WorkingDirectory $frontendRoot
        }
        'frontend-no-skips' {
            $report = Join-Path $frontendRoot 'coverage/test-results/junit.xml'
            Assert-JUnitReportsHaveNoSkippedTests -ReportPaths @($report) -Description 'frontend'
        }
        'frontend-format' {
            Invoke-ExternalCommand `
                -Executable 'npx' `
                -Arguments @('prettier', '--check', 'src/**/*.{ts,html,scss}', '*.json') `
                -WorkingDirectory $frontendRoot
        }
        'frontend-build' {
            Invoke-ExternalCommand -Executable 'npm' -Arguments @('run', 'build') -WorkingDirectory $frontendRoot
        }
        'compose-config' {
            Invoke-ExternalCommand -Executable 'docker' -Arguments @('compose', 'config', '--quiet') -WorkingDirectory $RepositoryRoot
        }
        'diff-check' {
            Invoke-ExternalCommand -Executable 'git' -Arguments @('diff', '--check') -WorkingDirectory $RepositoryRoot
        }
        default {
            throw "Unknown verification step '$($Step.Name)'."
        }
    }
}

Export-ModuleMember -Function @(
    'Get-VerificationStepNames',
    'Assert-ExactVersion',
    'Assert-JUnitXmlHasNoSkippedTests',
    'Assert-JUnitReportsHaveNoSkippedTests',
    'Invoke-VerificationPlan',
    'Invoke-ExternalCommand',
    'Get-MavenWrapperInvocation',
    'Test-WindowsPlatform',
    'Invoke-RepositoryVerificationStep'
)
