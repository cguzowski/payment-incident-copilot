$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$modulePath = Join-Path $PSScriptRoot 'Verification.psm1'
if (-not (Test-Path -LiteralPath $modulePath)) {
    throw "Verification module is missing: $modulePath"
}
Import-Module $modulePath -Force

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

$failures = [System.Collections.Generic.List[string]]::new()

function Assert-Equal {
    param(
        [Parameter(Mandatory)] $Expected,
        [Parameter(Mandatory)] $Actual,
        [Parameter(Mandatory)] [string] $Message
    )

    $expectedText = $Expected -join ','
    $actualText = $Actual -join ','
    if ($expectedText -ne $actualText) {
        throw "$Message Expected '$expectedText' but got '$actualText'."
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
            throw "Expected error like '$MessagePattern' but got '$($_.Exception.Message)'."
        }
        return
    }
    throw "Expected an error like '$MessagePattern' but no error was raised."
}

function Invoke-VerificationTest {
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

Invoke-VerificationTest 'buildsTheCompletePlanInRequiredOrder' {
    Assert-Equal @(
        'check-java'
        'check-node'
        'check-npm'
        'check-repository-tools'
        'verification-system-tests'
        'credential-safety'
        'maven-verify'
        'backend-no-skips'
        'frontend-install'
        'frontend-test'
        'frontend-no-skips'
        'frontend-format'
        'frontend-build'
        'compose-config'
        'diff-check'
    ) (Get-VerificationStepNames -Scope All) 'Complete verification order differs.'
}

Invoke-VerificationTest 'buildsIndependentBackendAndFrontendPlans' {
    Assert-Equal @(
        'check-java'
        'check-repository-tools'
        'maven-verify'
        'backend-no-skips'
    ) (Get-VerificationStepNames -Scope Backend) 'Backend verification scope differs.'
    Assert-Equal @(
        'check-node'
        'check-npm'
        'frontend-install'
        'frontend-test'
        'frontend-no-skips'
        'frontend-format'
        'frontend-build'
    ) (Get-VerificationStepNames -Scope Frontend) 'Frontend verification scope differs.'
    Assert-Equal @(
        'check-repository-tools'
        'verification-system-tests'
        'credential-safety'
        'compose-config'
        'diff-check'
    ) (Get-VerificationStepNames -Scope Repository) 'Repository verification scope differs.'
}

Invoke-VerificationTest 'rejectsUnexpectedPinnedVersions' {
    Assert-ExactVersion -Name 'Node.js' -Actual '24.14.1' -Expected '24.14.1'
    Assert-Throws {
        Assert-ExactVersion -Name 'Node.js' -Actual '24.14.0' -Expected '24.14.1'
    } '*Node.js*24.14.1*24.14.0*'
}

Invoke-VerificationTest 'buildsPortableMavenWrapperInvocations' {
    $windowsInvocation = Get-MavenWrapperInvocation `
        -RepositoryRoot 'Z:\synthetic-repository' `
        -WindowsPlatform $true
    if ($windowsInvocation.Executable -ne 'Z:\synthetic-repository\mvnw.cmd') {
        throw "Windows Maven executable differs: '$($windowsInvocation.Executable)'."
    }
    Assert-Equal @('--batch-mode', 'clean', 'verify') `
        $windowsInvocation.Arguments `
        'Windows Maven arguments differ.'

    $unixInvocation = Get-MavenWrapperInvocation `
        -RepositoryRoot '/synthetic-repository' `
        -WindowsPlatform $false
    if ($unixInvocation.Executable -ne 'sh') {
        throw "Unix Maven executable differs: '$($unixInvocation.Executable)'."
    }
    Assert-Equal @('/synthetic-repository/mvnw', '--batch-mode', 'clean', 'verify') `
        $unixInvocation.Arguments `
        'Unix Maven arguments differ.'
}

Invoke-VerificationTest 'rejectsSkippedJUnitResults' {
    Assert-JUnitXmlHasNoSkippedTests -XmlContent '<testsuite tests="2" failures="0" errors="0" skipped="0"></testsuite>' -Source 'green.xml'
    Assert-Throws {
        Assert-JUnitXmlHasNoSkippedTests -XmlContent '<testsuite tests="2" failures="0" errors="0" skipped="1"><testcase><skipped/></testcase></testsuite>' -Source 'skipped.xml'
    } '*skipped.xml*skipped*'
}

Invoke-VerificationTest 'rejectsTokenShapedRepositoryContentWithoutEchoingValue' {
    $syntheticToken = 'AB' + 'SKSyntheticBedrockCredentialMaterial1234567890'
    try {
        Assert-TextHasNoBedrockBearerToken `
            -Content "safe-prefix $syntheticToken safe-suffix" `
            -Source 'synthetic.txt'
    }
    catch {
        if ($_.Exception.Message.Contains($syntheticToken)) {
            throw 'Credential-safety failure echoed the matched token.'
        }
        if ($_.Exception.Message -notlike '*synthetic.txt*') {
            throw "Credential-safety failure did not identify the source file."
        }
        return
    }
    throw 'Token-shaped repository content was accepted.'
}

Invoke-VerificationTest 'rejectsIgnoredDotEnvVariantsWithoutEchoingValue' {
    $temporaryRepository = Join-Path `
        ([IO.Path]::GetTempPath()) `
        ("payment-copilot-verification-" + [guid]::NewGuid().ToString('N'))
    $syntheticToken = 'AB' + 'SKIgnoredEnvironmentCredentialMaterial1234567890'
    [IO.Directory]::CreateDirectory($temporaryRepository) | Out-Null
    try {
        [IO.File]::WriteAllText((Join-Path $temporaryRepository '.gitignore'), ".env.*`n")
        [IO.File]::WriteAllText(
            (Join-Path $temporaryRepository '.env.local'),
            "AWS_BEARER_TOKEN_BEDROCK=$syntheticToken"
        )
        & git -C $temporaryRepository init --quiet
        if ($LASTEXITCODE -ne 0) {
            throw 'Could not initialize the temporary Git repository.'
        }

        try {
            Assert-RepositoryContainsNoBedrockBearerTokens `
                -RepositoryRoot $temporaryRepository
        }
        catch {
            if ($_.Exception.Message.Contains($syntheticToken)) {
                throw 'Credential-safety failure echoed the ignored environment credential.'
            }
            if ($_.Exception.Message -notlike '*.env.local*') {
                throw 'Credential-safety failure did not identify the ignored environment file.'
            }
            return
        }
        throw 'Token-shaped content in an ignored root environment file was accepted.'
    }
    finally {
        $resolvedTemporaryRepository = [IO.Path]::GetFullPath($temporaryRepository)
        $resolvedTemporaryRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
        if (-not $resolvedTemporaryRepository.StartsWith(
                $resolvedTemporaryRoot,
                [StringComparison]::OrdinalIgnoreCase
            )) {
            throw 'Refusing to remove a temporary repository outside the system temp directory.'
        }
        Remove-Item -LiteralPath $resolvedTemporaryRepository -Recurse -Force
    }
}

Invoke-VerificationTest 'stopsAtTheFirstFailedStepAndNamesIt' {
    $visited = [System.Collections.Generic.List[string]]::new()
    Assert-Throws {
        Invoke-VerificationPlan -Scope Backend -StepRunner {
            param($step)
            $visited.Add($step.Name)
            if ($step.Name -eq 'maven-verify') {
                throw 'synthetic command failure'
            }
        }
    } '*maven-verify*synthetic command failure*'
    Assert-Equal @(
        'check-java'
        'check-repository-tools'
        'maven-verify'
    ) $visited 'Verification did not stop at the first failure.'
}

Invoke-VerificationTest 'delegatesCiChecksToTheRepositoryVerificationEntryPoint' {
    $workflowPath = Join-Path $repositoryRoot '.github\workflows\ci.yml'
    $workflow = Get-Content -LiteralPath $workflowPath -Raw

    foreach ($scope in @('Backend', 'Frontend', 'Repository')) {
        if ($workflow -notmatch "verify\.ps1\s+-Scope\s+$scope") {
            throw "CI does not delegate the $scope checks to verify.ps1."
        }
    }

    foreach ($duplicatedCommand in @('mvn --batch-mode clean verify', 'npm test', 'npm run build', 'docker compose config')) {
        if ($workflow.Contains($duplicatedCommand)) {
            throw "CI duplicates verification command '$duplicatedCommand'."
        }
    }
}

Invoke-VerificationTest 'configuresAngularTestsToEmitTheRequiredJUnitReport' {
    $angularConfigurationPath = Join-Path $repositoryRoot 'frontend\operator-console\angular.json'
    $angularConfiguration = Get-Content -LiteralPath $angularConfigurationPath -Raw | ConvertFrom-Json
    $reporters = $angularConfiguration.projects.'operator-console'.architect.test.options.reporters
    $junitReporter = $reporters | Where-Object { $_ -is [System.Array] -and $_[0] -eq 'junit' }

    if ($null -eq $junitReporter) {
        throw 'Angular tests do not configure a JUnit reporter.'
    }
    if ($junitReporter[1].outputFile -ne 'coverage/test-results/junit.xml') {
        throw "Angular JUnit output path differs: '$($junitReporter[1].outputFile)'."
    }
}

if ($failures.Count -gt 0) {
    throw ($failures -join [Environment]::NewLine)
}

Write-Host "All verification-system tests passed."
