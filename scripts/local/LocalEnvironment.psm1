Set-StrictMode -Version Latest

function Assert-DotEnvDoesNotContainBedrockBearerToken {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [string] $Path
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return
    }

    foreach ($line in [IO.File]::ReadAllLines($Path)) {
        if ($line -match '^\s*AWS_BEARER_TOKEN_BEDROCK\s*=') {
            throw 'AWS_BEARER_TOKEN_BEDROCK must not be stored in the repository .env file. Remove it and rotate the key if it was copied there.'
        }
    }
}

function Invoke-WithBedrockBearerTokenForApiLaunch {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory)]
        [scriptblock] $BeforeApiLaunch,

        [Parameter(Mandatory)]
        [scriptblock] $ApiLaunch,

        [Parameter(Mandatory)]
        [scriptblock] $AfterApiLaunch,

        [scriptblock] $EnvironmentReader = {
            param($name, $scope)
            return [Environment]::GetEnvironmentVariable($name, $scope)
        },

        [scriptblock] $EnvironmentWriter = {
            param($name, $value)
            [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        }
    )

    $tokenName = 'AWS_BEARER_TOKEN_BEDROCK'
    $sourceScope = $null
    $selectedToken = $null

    foreach ($scope in @('Process', 'User', 'Machine')) {
        $candidate = & $EnvironmentReader $tokenName $scope
        if (-not [string]::IsNullOrWhiteSpace($candidate)) {
            $sourceScope = $scope
            $selectedToken = $candidate
            break
        }
    }

    & $EnvironmentWriter $tokenName $null
    try {
        $null = & $BeforeApiLaunch
        if ($null -ne $sourceScope) {
            & $EnvironmentWriter $tokenName $selectedToken
        }
        try {
            $null = & $ApiLaunch
        }
        finally {
            & $EnvironmentWriter $tokenName $null
        }
        $null = & $AfterApiLaunch
        return $sourceScope
    }
    finally {
        & $EnvironmentWriter $tokenName $null
        $selectedToken = $null
    }
}

Export-ModuleMember -Function @(
    'Assert-DotEnvDoesNotContainBedrockBearerToken',
    'Invoke-WithBedrockBearerTokenForApiLaunch'
)
