$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$modulePath = Join-Path $PSScriptRoot 'LocalEnvironment.psm1'
if (-not (Test-Path -LiteralPath $modulePath -PathType Leaf)) {
    throw "Local environment module is missing: $modulePath"
}
Import-Module $modulePath -Force

$failures = [System.Collections.Generic.List[string]]::new()

function Invoke-LocalEnvironmentTest {
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

Invoke-LocalEnvironmentTest 'selectsProcessThenUserThenMachineScopeWithoutReturningTheToken' {
    $cases = @(
        @{ Values = @{ Process = 'process-secret'; User = 'user-secret'; Machine = 'machine-secret' }; Expected = 'Process' }
        @{ Values = @{ Process = ''; User = 'user-secret'; Machine = 'machine-secret' }; Expected = 'User' }
        @{ Values = @{ Process = ''; User = ''; Machine = 'machine-secret' }; Expected = 'Machine' }
    )

    foreach ($case in $cases) {
        $processEnvironment = @{}
        $stages = [System.Collections.Generic.List[string]]::new()
        $source = Invoke-WithBedrockBearerTokenForApiLaunch `
            -BeforeApiLaunch { $stages.Add("before:$($processEnvironment['AWS_BEARER_TOKEN_BEDROCK'])") } `
            -ApiLaunch { $stages.Add("api:$($processEnvironment['AWS_BEARER_TOKEN_BEDROCK'])") } `
            -AfterApiLaunch { $stages.Add("after:$($processEnvironment['AWS_BEARER_TOKEN_BEDROCK'])") } `
            -EnvironmentReader {
                param($name, $scope)
                return $case.Values[$scope]
            } `
            -EnvironmentWriter {
                param($name, $value)
                $processEnvironment[$name] = $value
            }

        if ($source -ne $case.Expected) {
            throw "Expected scope '$($case.Expected)' but got '$source'."
        }
        if ($source -in $case.Values.Values) {
            throw 'Token value was returned instead of its source scope.'
        }
        if ($stages[0] -ne 'before:' -or $stages[2] -ne 'after:') {
            throw 'A non-API child could observe the Bedrock token.'
        }
        $expectedToken = $case.Values[$case.Expected]
        if ($stages[1] -ne "api:$expectedToken") {
            throw 'The API child did not receive the selected Bedrock token.'
        }
    }
}

Invoke-LocalEnvironmentTest 'rejectsBedrockBearerTokenAssignmentInDotEnvWithoutEchoingValue' {
    $temporaryFile = [IO.Path]::GetTempFileName()
    $syntheticToken = 'synthetic-secret-that-must-not-be-echoed'
    try {
        [IO.File]::WriteAllText(
            $temporaryFile,
            "SPRING_DATASOURCE_URL=jdbc:postgresql://localhost/test`nAWS_BEARER_TOKEN_BEDROCK=$syntheticToken"
        )
        try {
            Assert-DotEnvDoesNotContainBedrockBearerToken -Path $temporaryFile
        }
        catch {
            if ($_.Exception.Message.Contains($syntheticToken)) {
                throw 'The .env rejection echoed the credential value.'
            }
            return
        }
        throw 'A Bedrock bearer-token assignment in .env was accepted.'
    }
    finally {
        Remove-Item -LiteralPath $temporaryFile -Force
    }
}

if ($failures.Count -gt 0) {
    throw ($failures -join [Environment]::NewLine)
}

Write-Host 'All local-environment tests passed.'
